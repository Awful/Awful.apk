package com.ferg.awfulapp

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.util.AwfulUtils
import com.ferg.awfulapp.widget.ToggleViewPager
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

    private val TAG = this.javaClass.simpleName
    private val pagerAdapter: ForumPagerAdapter

    var tabletMode: Boolean? = null

    var forumId = Constants.USERCP_ID
    var forumPage = 1
    var threadId: Int? = null
    var threadPage = 1
    var postJump: String? = null

    var currentPagerItem: Int
        get() = viewPager.currentItem
        set(itemNum) {
            // TODO: reject if out of range - use ENUM instead?
            // TODO: set item
            viewPager.currentItem = itemNum
        }

    init {
        onPreferenceChange(prefs)
        viewPager.offscreenPageLimit = 2
        onConfigurationChange(prefs)
        pagerAdapter = ForumPagerAdapter(this, activity.supportFragmentManager)
        viewPager.adapter = pagerAdapter
        viewPager.setOnPageChangeListener(pagerAdapter)
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
        pagerAdapter.threadListFragment.openForum(forumId, pageNum)
        showPage(Pages.ThreadList)
    }

    fun openThread(url: AwfulURL) {
        // TODO: we don't hold state for this, so it doesn't really make sense
        // better to have a PagerState object that either has a URL or IDs, or just parse out the data from this URL?
        pagerAdapter.threadDisplayFragment.openThread(url)
        showPage(Pages.ThreadDisplay)
    }


    fun openThread(id: Int, page: Int, jump: String, forceReload: Boolean = true) {
        if (forceReload || threadId != id || threadPage != page || postJump != jump) {
            Log.i(TAG, "Opening thread (old/new) ID:$threadId/$id, PAGE:$threadPage/$page, JUMP:$postJump/$jump - force=$forceReload")
            pagerAdapter.threadDisplayFragment.openThread(id, page, jump, false)
        }
        showPage(Pages.ThreadDisplay)
        threadId = id
        threadPage = page
        postJump = jump
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
    val forumListFragment: ForumsIndexFragment by lazy { ForumsIndexFragment() }
    val threadListFragment: ForumDisplayFragment by lazy {
        // TODO: what should skipload be?
        ForumDisplayFragment.getInstance(controller.forumId, controller.forumPage, false)
    }
    val threadDisplayFragment: ThreadDisplayFragment by lazy { threadViewAdded = true; ThreadDisplayFragment() }
    var currentFragment: AwfulFragment? = null


    override fun onPageSelected(pageNum: Int) {
        // TODO: set new current if necess (plus cleanup), do callback for activity
        if (AwfulActivity.DEBUG) Log.i(TAG, "onPageSelected: " + pageNum)
        currentFragment?.onPageHidden()
        val selectedPage = instantiateItem(controller.viewPager, pageNum) as AwfulFragment
        controller.onPageChanged(pageNum, selectedPage)
        currentFragment = selectedPage
    }


    override fun getItem(position: Int): Fragment {
        return when (Pages[position]) {
            Pages.ForumList -> forumListFragment
            Pages.ThreadList -> threadListFragment
            Pages.ThreadDisplay -> threadDisplayFragment
        }
    }

    override fun getCount() = if (threadViewAdded) 3 else 2
    override fun getPageWidth(position: Int) = if (controller.tabletMode == true) Pages[position].width else super.getPageWidth(position)

}