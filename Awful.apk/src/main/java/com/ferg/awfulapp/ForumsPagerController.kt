package com.ferg.awfulapp

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
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

enum class Pages(val width: Float) {
    ForumList(0.4f), ThreadList(0.6f), ThreadDisplay(1f);

    companion object {
        operator fun get(index: Int) = values()[index]
    }
}

class ForumsPagerController(
        val viewPager: ToggleViewPager,
        prefs: AwfulPreferences,
        activity: FragmentActivity,
        private val callbacks: PagerCallbacks
) {

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
        viewPager.offscreenPageLimit = 2
        onPreferenceChange(prefs)
        onConfigurationChange(prefs)
        pagerAdapter = ForumPagerAdapter(this, activity.supportFragmentManager)
        viewPager.adapter = pagerAdapter
        viewPager.setOnPageChangeListener(pagerAdapter)
        Timber.d("--- init finish")
    }


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


    fun openForum(forumId: Int, pageNum: Int) {
        showPage(Pages.ThreadList)
        pagerAdapter.threadListFragment!!.openForum(forumId, pageNum)
    }

    fun openThread(url: AwfulURL) {
        showPage(Pages.ThreadDisplay)
        // TODO: we don't hold state for this, so it doesn't really make sense
        // better to have a PagerState object that either has a URL or IDs, or just parse out the data from this URL?
        pagerAdapter.threadDisplayFragment!!.openThread(url)
    }


    fun openThread(id: Int, page: Int, jump: String, forceReload: Boolean = true) {
        showPage(Pages.ThreadDisplay)
        with (pagerAdapter.threadDisplayFragment!!) {
            if (forceReload || threadId != id || pageNumber != page || postJump != jump) {
                Timber.i("Opening thread (old/new) ID:$threadId/$id, PAGE:$pageNumber/$page, JUMP:$postJump/$jump - force=$forceReload")
                openThread(id, page, jump, false)
            }
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

    fun reenableSwipe() {
        if (viewPager.beginFakeDrag()) {
            viewPager.endFakeDrag()
        }
        viewPager.setSwipeEnabled(true)
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

    private val TAG = this.javaClass.simpleName

    var threadViewAdded: Boolean by Delegates.observable(false) { _, old, new ->
        if (old != new) notifyDataSetChanged()
    }

    var forumListFragment: ForumsIndexFragment? = null
    var threadListFragment: ForumDisplayFragment? = null
    var threadDisplayFragment: ThreadDisplayFragment? = null
    var currentFragment: AwfulFragment? = null


    override fun onPageSelected(pageNum: Int) {
        // TODO: redo this so we're not instantiating things
        if (AwfulActivity.DEBUG) Log.i(TAG, "onPageSelected: " + pageNum)
        currentFragment?.onPageHidden()
        val selectedPage = instantiateItem(controller.viewPager, pageNum) as AwfulFragment
        controller.onPageChanged(pageNum, selectedPage)
        currentFragment = selectedPage
    }


    override fun getItem(position: Int): Fragment {
        return when (Pages[position]) {
            Pages.ForumList -> { Timber.d("Creating ForumsIndexFragment"); ForumsIndexFragment() }
            Pages.ThreadList -> { Timber.d("Creating ForumsDisplayFragment"); ForumDisplayFragment.getInstance(Constants.USERCP_ID, ForumDisplayFragment.FIRST_PAGE, false) }
            Pages.ThreadDisplay -> { Timber.d("Creating ThreadDisplayFragment"); ThreadDisplayFragment() }
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any =
            // overriding this so we get a reference when we provide a fragment OR the framework restores one
         super.instantiateItem(container, position).apply {
             when (this) {
                 is ForumsIndexFragment -> { forumListFragment = this; Timber.d("Setting ForumsIndexFragment") }
                 is ForumDisplayFragment -> { threadListFragment = this; Timber.d("Setting ForumDisplayFragment") }
                 is ThreadDisplayFragment -> { threadDisplayFragment = this; threadViewAdded = true; Timber.d("Setting ThreadDisplayFragment") }
             }
         }


    override fun getCount() = if (threadViewAdded) 3 else 2
    override fun getPageWidth(position: Int) = if (controller.tabletMode) Pages[position].width else super.getPageWidth(position)

}