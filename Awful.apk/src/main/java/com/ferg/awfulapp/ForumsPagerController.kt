package com.ferg.awfulapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import com.ferg.awfulapp.Pages.*
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.util.AwfulUtils
import timber.log.Timber
import kotlin.properties.Delegates

/**
 * Created by baka kaba on 04/11/2017.
 *
 * Components to manage and display a ViewPager holding the various forum page fragments, and act as
 * an interface to those fragments (e.g. to execute navigation events)
 */

/**
 * Defines each of the ViewPager pages, in order.
 *
 * @param width the width of this page in tablet mode, as a fraction of the screen width
 */
enum class Pages(val width: Float) {
    ForumIndex(0.4f), ForumDisplay(0.6f), ThreadDisplay(1f);

    companion object {
        operator fun get(index: Int) = values()[index]
    }
}


/**
 * Manages the ViewPager holding the forum pages.
 *
 * This component is meant to take responsibility for the forum view fragments, handling the layout,
 * passing navigation events and showing the correct page, and updating listeners of any changes in
 * the pager. It's also responsible for providing a title for the activity to use, based on the
 * fragments being shown and which of them is the current page.
 *
 * The view pager is meant to hide the thread display page completely until that page is explicitly
 * opened, i.e. by opening a thread. This is handled by setting a flag when the page is opened, and
 * also by saving that flag into the Activity's Bundle in [onSaveInstanceState], which can be restored
 * by passing it into the constructor when the Activity recreates.
 *
 * @param viewPager the inflated ViewPager from the activity's layout
 * @param savedInstanceState the activity's saved state - used to restore the viewpager
 */
class ForumsPagerController(
        private val viewPager: SwipeLockViewPager,
        prefs: AwfulPreferences,
        activity: FragmentActivity,
        savedInstanceState: Bundle?
) : NavigationEventHandler {

    companion object {
        private const val KEY_THREAD_VIEW_ADDED = "thread view added"
    }

    private val pagerAdapter: ForumPagerAdapter
    /** Whether the view pager is currently in tablet mode */
    var tabletMode: Boolean = false
        private set
    /** The current primary page - set this to show a specific page */
    var currentPagerItem: Pages
        get() = Pages[viewPager.currentItem]
        set(page) {
            if (page == ThreadDisplay) pagerAdapter.threadViewAdded = true
            viewPager.currentItem = page.ordinal
        }

    // Access to the fragments in the adapter - as functions so we can pass them as getters for the 'run when not null' tasks
    fun getForumIndexFragment() = pagerAdapter.fragments[ForumIndex] as ForumsIndexFragment?

    fun getForumDisplayFragment() = pagerAdapter.fragments[ForumDisplay] as ForumDisplayFragment?
    fun getThreadDisplayFragment() = pagerAdapter.fragments[ThreadDisplay] as ThreadDisplayFragment?

    init {
        onPreferenceChange(prefs)
        onConfigurationChange(prefs)
        pagerAdapter = ForumPagerAdapter(this, activity.supportFragmentManager).apply {
            savedInstanceState?.let { state ->
                threadViewAdded = state.getBoolean(KEY_THREAD_VIEW_ADDED)
            }
        }
        with(viewPager) {
            offscreenPageLimit = 2
            adapter = pagerAdapter
            setOnPageChangeListener(pagerAdapter)
        }
    }

    //
    // Navigation events
    //
    // The methods that require a fragment to be in place are a little strange - basically we want to
    // navigate to that page, and then defer the call to the fragment until it's ready. This means
    // that intents that open the app (like launcher widgets, or thread URLs clicked in another app)
    // can trigger a call to #openThread or whatever, when everything is still initialising, and when
    // the pager gets the fragments, the pending events will get processed.
    //
    // So we're passing a *function* that gets the relevant fragment, so it can be retried until it
    // actually produces a non-null value. Hopefully there's a better way, but this works for now.
    //

    /**
     * Handle a [NavigationEvent] by paging to the appropriate fragment and passing the event to it.
     */
    override fun handleNavigation(event: NavigationEvent): Boolean = when (event) {
        is NavigationEvent.Forum, is NavigationEvent.Bookmarks -> {
            currentPagerItem = ForumDisplay
            ::getForumDisplayFragment.runWhenNotNull { navigate(event) }
            true
        }
        is NavigationEvent.Thread, is NavigationEvent.Url -> {
            currentPagerItem = ThreadDisplay
            ::getThreadDisplayFragment.runWhenNotNull { navigate(event) }
            true
        }
        else -> false
    }


    /**
     * Called when a new page has been added to the pager (i.e. a page has been set to a fragment).
     */
    fun onPageAdded() {
        attemptAllDeferredTasks()
    }

    /*
        This is the code to handle pending tasks, i.e. events to call on a fragment that were sent
        before the fragment was available. Hopefully there's a better way to do all this...
        Tasks are just added to a queue, so multiple #showThread calls (for example) will just be
        run one after the other, and there are potential concurrency issues with the list, BUT this
        should only happen as soon as the app starts, and very quickly, so there shouldn't be time
        for any of these issues to happen...
     */

    private val deferredTasks = mutableListOf<() -> Unit>()

    /**
     * A getter, and code to run on the result if it's not null. This will be attempted immediately -
     * if the getter returns null, this call will be deferred until [attemptAllDeferredTasks] is called.
     */
    private fun <T> (() -> T?).runWhenNotNull(f: T.() -> Unit) {
        this()?.f() ?: deferredTasks.add { this.runWhenNotNull(f) }
    }

    /**
     * Attempt to run all tasks that were deferred (e.g. from [runWhenNotNull])
     */
    private fun attemptAllDeferredTasks() {
        Timber.d("Attempting ${deferredTasks.size} deferred tasks")
        deferredTasks.toList().also { deferredTasks.clear() }.forEach { it() }
        Timber.d("Processed deferred tasks: ${deferredTasks.size} remaining")
    }

    //
    // Utility functions
    //


    fun getVisibleFragmentTitle(): String {
        return getCurrentFragment()?.getTitle() ?: ""
        // might be useful if we can ever get visible fragment identification working
//        return pagerAdapter.run {
//            listOfNotNull(forumListFragment, threadListFragment, threadDisplayFragment)
//                    .filter(::isFragmentVisible)
//                    .map(AwfulFragment::getTitle)
//                    .joinToString(" / ")
//        }
    }

    fun getCurrentFragment() = pagerAdapter.fragments[currentPagerItem]


    // TODO: find a way to RELIABLY determine which fragments are visible in tablet mode
    /*
        Things that haven't worked:
        #userVisibleHint seems like a coin flip
        #isVisible and #isResumed seem to stick on true
        checking after #onPageScrollStateChanged fires with IDLE doesn't help, nothing seems to reflect the actual displayed state
     */
    private fun isFragmentVisible(frag: AwfulFragment) =
            frag == getCurrentFragment() //|| (tabletMode && frag.userVisibleHint)

    //
    // App events
    //

    /**
     * Call this from the host activity, so we can store the current ViewPager state.
     */
    fun onSaveInstanceState(bundle: Bundle) =
            apply { bundle.putBoolean(KEY_THREAD_VIEW_ADDED, pagerAdapter.threadViewAdded) }

    fun onPreferenceChange(prefs: AwfulPreferences) {
        setSwipeEnabled(!prefs.lockScrolling)
        if (!AwfulUtils.isTablet(prefs.context) && AwfulUtils.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1) && prefs.transformer != "Disabled") {
            viewPager.setPageTransformer(true, AwfulUtils.getViewPagerTransformer())
        }
    }

    fun onConfigurationChange(prefs: AwfulPreferences) {
        val isTablet = AwfulUtils.isTablet(prefs.context)
        // only update if there's a change in tablet mode (also happens on init when tabletMode is null)
        if (tabletMode != isTablet) {
            viewPager.pageMargin = if (isTablet) 1 else 0
            if (isTablet) viewPager.setPageMarginDrawable(ColorDrawable(ColorProvider.ACTION_BAR.color))
        }
        tabletMode = isTablet
    }

    fun onBackPressed(): Boolean {
        val handledByFragment = getCurrentFragment()?.onBackPressed() ?: false
        if (handledByFragment) return true
        // current fragment didn't consume it, try and shift back a page
        with(viewPager) {
            return hasPreviousPage().also { itDoes -> if (itDoes) goToPreviousPage() }
        }
    }

    fun setSwipeEnabled(enabled: Boolean) {
        viewPager.swipeEnabled = enabled
    }
}


/**
 * Represents a page in the forums view pager.
 */
interface ForumsPagerPage {
    /**
     * Called when this page is focused, and should be actively updating.
     */
    fun setAsFocusedPage()

    /**
     * Called when this page is in the background, and can suspend updates.
     */
    fun setAsBackgroundPage()
}


/**
 * An adapter that keeps a reference to all the created and restored fragments, hides the thread view
 * page until it's explicitly opened, and manages tablet mode.
 */
private class ForumPagerAdapter(
        val controller: ForumsPagerController,
        fm: FragmentManager
) : FragmentPagerAdapter(fm),
        ViewPager.OnPageChangeListener by ViewPager.SimpleOnPageChangeListener() {

    /** The fragments representing each page in the viewpager (if added) */
    val fragments = mutableMapOf<Pages, AwfulFragment?>(
            ForumIndex to null,
            ForumDisplay to null,
            ThreadDisplay to null
    )
    private var currentPage = ForumIndex
    /** Whether the thread view fragment has been added ('unlocking' that page for swiping) */
    var threadViewAdded: Boolean by Delegates.observable(false) { _, old, new ->
        if (old != new) notifyDataSetChanged()
    }


    override fun getCount() = if (threadViewAdded) 3 else 2

    override fun getPageWidth(position: Int) =
            if (controller.tabletMode) Pages[position].width else super.getPageWidth(position)

    override fun onPageSelected(pageNum: Int) {
        Timber.i("onPageSelected: $pageNum")
        // TODO: this only allows for one 'focused' page, even though 2 might be visible in tablet mode. If we ever get a way to make #isFragmentVisible work properly, use that here
        fragments[currentPage]?.setAsBackgroundPage()
        currentPage = Pages[pageNum]
        fragments[currentPage]?.setAsFocusedPage()
    }

    override fun getItem(position: Int): Fragment {
        Timber.i("Creating fragment for %s", Pages[position])
        return when (Pages[position]) {
            ForumIndex -> ForumsIndexFragment()
            ForumDisplay -> ForumDisplayFragment.getInstance(
                    Constants.USERCP_ID,
                    ForumDisplayFragment.FIRST_PAGE,
                    false
            )
            ThreadDisplay -> ThreadDisplayFragment()
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any =
    // this will either call #getItem OR restore saved fragments from the fragment manager, so we grab our references here
            super.instantiateItem(container, position).apply {
                when (this) {
                    is ForumsIndexFragment -> setAs(ForumIndex)
                    is ForumDisplayFragment -> setAs(ForumDisplay)
                    is ThreadDisplayFragment -> setAs(ThreadDisplay)
                }
            }

    /**
     * Set as one of the fragment pages in the pager.
     */
    private fun AwfulFragment.setAs(page: Pages) {
        Timber.d("Setting fragment for %s", page)
        fragments[page] = this
        controller.onPageAdded()
        // #onPageSelected fires before the page's fragments are created/restored, so this callback
        // ensures we update listeners when we actually *get* a new fragment for the current page
    }

}

/**
 * ViewPager wrapper that allows swiping to be enabled and disabled, e.g. to allow horizontal swiping
 * in code blocks in the webview without the pager moving too.
 */
class SwipeLockViewPager @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    /** Enable or disable swiping on this viewpager */
    var swipeEnabled by Delegates.observable(true, { _, _, enabled -> if (!enabled) cancelSwipe() })
    private var ignoreMotion = false

    /** Forcibly end the current swipe, and ignore any further motion events (avoids regaining focus during a swipe and seeing it as a large, sudden move) */
    private fun cancelSwipe() {
        SystemClock.uptimeMillis()
                .let { now -> MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0) }
                .let { onTouchEvent(it) }
        ignoreMotion = true
    }

    /** True if the current page is not the first */
    fun hasPreviousPage() = currentItem > 0

    /** Move to the page in the previous position, if possible */
    fun goToPreviousPage() {
        if (hasPreviousPage()) currentItem -= 1
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean =
            swipeEnabled && preventCrash {
                // if we're ignoring the current motion, this resets it when a new one starts
                if (ev.actionMasked == MotionEvent.ACTION_DOWN) ignoreMotion = false
                if (!ignoreMotion) super.onInterceptTouchEvent(ev) else false
            }

    @SuppressLint("ClickableViewAccessibility") // we're just calling through to the super method anyway
    override fun onTouchEvent(ev: MotionEvent): Boolean =
            swipeEnabled && preventCrash { super.onTouchEvent(ev) }


    /**
     * Fix to avoid apparent bug in the support library, with infrequent crashing from an IAE.
     * Runs the code in [block] and returns the result, or false if the crash occurred.
     * (See [this issue](https://code.google.com/p/android/issues/detail?id=64553))
     */
    private inline fun preventCrash(block: () -> Boolean): Boolean {
        // TODO: When/if this is fixed, remove the internal SwipyRefreshLayout class and refactor the XML layouts to use the external library version again, thanks!
        return try {
            block()
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
