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
 */

/**
 * Defines each of the ViewPager pages, in order.
 *
 * @param width the width of this page in tablet mode, as a fraction of the screen width
 */
enum class Pages(val width: Float) {
    ForumList(0.4f), ThreadList(0.6f), ThreadDisplay(1f);

    companion object {
        operator fun get(index: Int) = values()[index]
    }
}


/**
 * Manages the ViewPager holding the forum pages.
 *
 * @param viewPager the inflated ViewPager from the activity's layout
 * @param savedInstanceState the activity's saved state - used to restore the viewpager
 */
class ForumsPagerController(
        val viewPager: ToggleViewPager,
        prefs: AwfulPreferences,
        activity: FragmentActivity,
        private val callbacks: PagerCallbacks,
        savedInstanceState: Bundle?
) {

    companion object {
        private const val KEY_THREAD_VIEW_ADDED = "thread view added"
    }

    private val pagerAdapter: ForumPagerAdapter
    var tabletMode: Boolean = false
    var currentPagerItem: Int
        get() = viewPager.currentItem
        set(itemNum) {
            // TODO: reject if out of range - use ENUM instead?
            // TODO: set item
            viewPager.currentItem = itemNum
        }


    init {
        Timber.d("--- init start")
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
        Timber.d("--- init finish")
    }

    /**
     * Call this from the host activity, so we can store the current ViewPager state.
     */
    fun onSaveInstanceState(bundle: Bundle) = apply { bundle.putBoolean(KEY_THREAD_VIEW_ADDED, pagerAdapter.threadViewAdded) }


    fun onPreferenceChange(prefs: AwfulPreferences) {
        setSwipeEnabled(!prefs.lockScrolling)
        // TODO: the context here might need to be a WindowManager or Activity or whatever
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

    fun getForumsIndexFragment() = pagerAdapter.forumListFragment
    fun getForumDisplayFragment() = pagerAdapter.threadListFragment
    fun getThreadDisplayFragment() = pagerAdapter.threadDisplayFragment

    // TODO: need more general functions here - show a forum (if it's open just page to it, otherwise also load page 1) and show thread (same deal, just page over if it's already open)

    fun openForum(forumId: Int, pageNum: Int? = null) {
        showPage(Pages.ThreadList)
        pagerAdapter.threadListFragment?.openForum(forumId, pageNum)
    }

    fun openThread(url: AwfulURL) {
        showPage(Pages.ThreadDisplay)
        // TODO: we don't hold state for this, so it doesn't really make sense
        // better to have a PagerState object that either has a URL or IDs, or just parse out the data from this URL?
        pagerAdapter.threadDisplayFragment!!.openThread(url)
    }


    fun openThread(id: Int, page: Int? = null, jump: String? = null, forceReload: Boolean = true) {
        showPage(Pages.ThreadDisplay)
        pagerAdapter.threadDisplayFragment?.apply {
            Timber.i("Opening thread (old/new) ID:$threadId/$id, PAGE:$pageNumber/$page, JUMP:$postJump/$jump - force=$forceReload")
            this.openThread(id, page, jump, false)
        }
        // TODO: notify nav drawer
    }

    fun getCurrentFragment() = pagerAdapter.currentFragment

    fun goBackOnePage(): Boolean {
        TODO()
    }

    // TODO: better way of checking what's visible?
    fun isFragmentVisible(frag: AwfulFragment): Boolean = frag == pagerAdapter.currentFragment

    fun setSwipeEnabled(enabled: Boolean) = viewPager.setSwipeEnabled(enabled)

    fun reenableSwipe() =
            with(viewPager) {
                if (beginFakeDrag()) endFakeDrag()
                setSwipeEnabled(true)
            }


    fun onPageChanged(pageNum: Int, pageFragment: AwfulFragment) {
        callbacks.onPageChanged(pageNum, pageFragment)
    }

    private fun showPage(type: Pages) {
        if (type == Pages.ThreadDisplay) pagerAdapter.threadViewAdded = true
        viewPager.setCurrentItem(type.ordinal, true)
    }

}

interface PagerCallbacks {
    fun onPageChanged(pageNum: Int, pageFragment: AwfulFragment)
}


private class ForumPagerAdapter(
        val controller: ForumsPagerController,
        fm: FragmentManager
) : FragmentPagerAdapter(fm),
        ViewPager.OnPageChangeListener by ViewPager.SimpleOnPageChangeListener() {

    var forumListFragment: ForumsIndexFragment? = null
    var threadListFragment: ForumDisplayFragment? = null
    var threadDisplayFragment: ThreadDisplayFragment? = null
    var currentFragment: AwfulFragment? = null

    var threadViewAdded: Boolean by Delegates.observable(false) { _, old, new ->
        if (old != new) notifyDataSetChanged()
    }

    override fun getCount() = if (threadViewAdded) 3 else 2
    override fun getPageWidth(position: Int) = if (controller.tabletMode) Pages[position].width else super.getPageWidth(position)

    override fun onPageSelected(pageNum: Int) {
        // TODO: redo this so we're not instantiating things
        if (AwfulActivity.DEBUG) Timber.i("onPageSelected: $pageNum")
        currentFragment?.onPageHidden()
        val selectedPage = instantiateItem(controller.viewPager, pageNum) as AwfulFragment
        controller.onPageChanged(pageNum, selectedPage)
        currentFragment = selectedPage
    }


    override fun getItem(position: Int): Fragment {
        return when (Pages[position]) {
            Pages.ForumList -> {
                Timber.d("Creating ForumsIndexFragment"); ForumsIndexFragment()
            }
            Pages.ThreadList -> {
                Timber.d("Creating ForumsDisplayFragment"); ForumDisplayFragment.getInstance(Constants.USERCP_ID, ForumDisplayFragment.FIRST_PAGE, false)
            }
            Pages.ThreadDisplay -> {
                Timber.d("Creating ThreadDisplayFragment"); ThreadDisplayFragment()
            }
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any =
            // overriding this so we get a reference when we provide a fragment OR the framework restores one
            super.instantiateItem(container, position).apply {
                when (this) {
                    is ForumsIndexFragment -> {
                        forumListFragment = this; Timber.d("Setting ForumsIndexFragment")
                    }
                    is ForumDisplayFragment -> {
                        threadListFragment = this; Timber.d("Setting ForumDisplayFragment")
                    }
                    is ThreadDisplayFragment -> {
                        threadDisplayFragment = this; threadViewAdded = true; Timber.d("Setting ThreadDisplayFragment")
                    }
                }
            }

}