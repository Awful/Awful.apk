package com.ferg.awfulapp

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.ViewGroup
import com.ferg.awfulapp.Pages.*
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.util.AwfulUtils
import com.ferg.awfulapp.widget.ToggleViewPager
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
        private val viewPager: ToggleViewPager,
        prefs: AwfulPreferences,
        activity: FragmentActivity,
        private val callbacks: PagerCallbacks,
        savedInstanceState: Bundle?
) {

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

    // Access to the fragments in the adapter
    val forumIndexFragment get() = pagerAdapter.fragments[ForumIndex] as ForumsIndexFragment?
    val forumDisplayFragment get() = pagerAdapter.fragments[ForumDisplay] as ForumDisplayFragment?
    val threadDisplayFragment get() = pagerAdapter.fragments[ThreadDisplay] as ThreadDisplayFragment?

    init {
        onPreferenceChange(prefs)
        onConfigurationChange(prefs)
        pagerAdapter = ForumPagerAdapter(this, activity.supportFragmentManager).apply {
            savedInstanceState?.let { state -> threadViewAdded = state.getBoolean(KEY_THREAD_VIEW_ADDED) }
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

    /**
     * Open and display a given forum, at a certain page if required.
     */
    fun openForum(forumId: Int, pageNum: Int? = null) {
        currentPagerItem = ForumDisplay
        forumDisplayFragment?.openForum(forumId, pageNum)
    }


    /**
     * Open and display a given thread, optionally at a specific page or jumping to a post anchor.
     *
     * @param forceReload set true to always reload the page, even if it's currently showing what
     * you've requested, otherwise we just page over to it
     */
    fun openThread(id: Int, page: Int? = null, jump: String? = null, forceReload: Boolean = true) {
        currentPagerItem = ThreadDisplay
        threadDisplayFragment?.apply {
            Timber.i("Opening thread (old/new) ID:$threadId/$id, PAGE:$pageNumber/$page, JUMP:$postJump/$jump - force=$forceReload")
            this.openThread(id, page, jump, false)
        }
    }


    /**
     * Open and display a thread given by an AwfulURL - you need to ensure this is a valid thread one
     */
    fun openThread(url: AwfulURL) {
        currentPagerItem = ThreadDisplay
        threadDisplayFragment?.openThread(url)
    }

    /**
     * Called when the view pager either moves to a different page, or the current page is replaced (e.g. with a new fragment)
     */
    fun onCurrentPageChanged() {
        getCurrentFragment()?.let { fragment -> callbacks.onPageChanged(currentPagerItem, fragment) }
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
    fun onSaveInstanceState(bundle: Bundle) = apply { bundle.putBoolean(KEY_THREAD_VIEW_ADDED, pagerAdapter.threadViewAdded) }

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

    fun setSwipeEnabled(enabled: Boolean) = viewPager.setSwipeEnabled(enabled)
}


interface PagerCallbacks {
    /**
     * Called when the current page in the forums pager has changed.
     *
     * This could be a change of focus (i.e. a different page), or the current page has updated
     * (e.g. its view has been added to the pager).
     *
     * @param page which page is now focused in the pager
     * @param pageFragment the fragment for this page
     */
    fun onPageChanged(page: Pages, pageFragment: AwfulFragment)
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

    override fun getPageWidth(position: Int) = if (controller.tabletMode) Pages[position].width else super.getPageWidth(position)

    override fun onPageSelected(pageNum: Int) {
        if (AwfulActivity.DEBUG) Timber.i("onPageSelected: $pageNum")
        // TODO: this only allows for one 'focused' page, even though 2 might be visible in tablet mode. If we ever get a way to make #isFragmentVisible work properly, use that here
        fragments[currentPage]?.setAsBackgroundPage()
        currentPage = Pages[pageNum]
        fragments[currentPage]?.setAsFocusedPage()
        controller.onCurrentPageChanged()
    }

    override fun getItem(position: Int): Fragment {
        Timber.i("Creating fragment for %s", Pages[position])
        return when (Pages[position]) {
            ForumIndex -> ForumsIndexFragment()
            ForumDisplay -> ForumDisplayFragment.getInstance(Constants.USERCP_ID, ForumDisplayFragment.FIRST_PAGE, false)
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
        // #onPageSelected fires before the page's fragments are created/restored, so this callback
        // ensures we update listeners when we actually *get* a new fragment for the current page
        if (page == currentPage) controller.onCurrentPageChanged()
    }

}