/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferg.awfulapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.format.Formatter
import android.text.style.ForegroundColorSpan
import android.view.InflateException
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.MenuItemCompat
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.core.view.size
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.android.volley.Response
import com.android.volley.VolleyError
import com.ferg.awfulapp.CaptchaActivity.Companion.handleCaptchaChallenge
import com.ferg.awfulapp.FontManager.Companion.getInstance
import com.ferg.awfulapp.NavigationEvent.LepersColony
import com.ferg.awfulapp.NavigationEvent.ModQueueRequest
import com.ferg.awfulapp.NavigationEvent.SearchForums
import com.ferg.awfulapp.NavigationEvent.Url
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.CookieController
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.popupmenu.BasePopupMenu.OnActionClickedListener
import com.ferg.awfulapp.popupmenu.PostContextMenu
import com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction
import com.ferg.awfulapp.popupmenu.UrlContextMenu
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.BooleanPreference
import com.ferg.awfulapp.preferences.StringSetPreference
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.AwfulTheme
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.search.SearchFilter
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.BookmarkRequest
import com.ferg.awfulapp.task.IgnoreRequest
import com.ferg.awfulapp.task.ImageSizeRequest
import com.ferg.awfulapp.task.MarkLastReadRequest
import com.ferg.awfulapp.task.RedirectTask
import com.ferg.awfulapp.task.RefreshUserProfileRequest
import com.ferg.awfulapp.task.ReportCheckRequest
import com.ferg.awfulapp.task.ReportCheckResult
import com.ferg.awfulapp.task.ReportRequest
import com.ferg.awfulapp.task.SinglePostRequest
import com.ferg.awfulapp.task.ThreadLockUnlockRequest
import com.ferg.awfulapp.task.ThreadPageRequest
import com.ferg.awfulapp.task.ThreadPageRequest.Companion.REQUEST_TAG
import com.ferg.awfulapp.task.VoteRequest
import com.ferg.awfulapp.thread.AwfulHtmlPage
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPagedItem
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.thread.AwfulURL.TYPE
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.util.AwfulUtils
import com.ferg.awfulapp.webview.AwfulWebView
import com.ferg.awfulapp.webview.LoggingWebChromeClient
import com.ferg.awfulapp.webview.WebViewJsInterface
import com.ferg.awfulapp.widget.MinMaxNumberPicker
import com.ferg.awfulapp.widget.PageBar
import com.ferg.awfulapp.widget.PageBar.PageBarCallbacks
import com.ferg.awfulapp.widget.PagePicker
import com.ferg.awfulapp.widget.WebViewSearchBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Strings
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.i
import timber.log.Timber.Forest.w
import java.util.LinkedList
import java.util.Locale
import java.util.regex.Pattern

/**
 * Uses intent extras:
 * TYPE - STRING ID - DESCRIPTION
 * int - Constants.THREAD_ID - id number for that thread
 * int - Constants.THREAD_PAGE - page number to load
 * 
 * Can also handle an HTTP intent that refers to an SA showthread.php? url.
 */
class ThreadDisplayFragment : AwfulFragment(), NavigationEventHandler {

    companion object {
        private const val THREAD_ID_KEY = "thread_id"
        private const val THREAD_PAGE_KEY = "thread_page"
        private const val SCROLL_POSITION_KEY = "scroll_position"
        private const val KEEP_SCREEN_ON_KEY = "screen_stays_on"
        private const val BLANK_USER_ID = 0
        const val FIRST_PAGE: Int = 1


        const val NULL_THREAD_ID: Int = 0
    }

    private var mPostLoaderCallback: PostLoaderManager? = null
    private var mThreadLoaderCallback: ThreadDataCallback? = null

    /*
		Potentially null views, if layout inflation failed (i.e. the WebView package is updating)
	 */
    private var pageBar: PageBar? = null
    private var mUserPostNotice: TextView? = null
    private var mFAB: FloatingActionButton? = null
    private var mThreadView: AwfulWebView? = null

    /** An optional ID to only display posts by a specific user  */
    private var postFilterUserId: Int? = null

    /** The username to display when filtering by a specific user  */
    private var postFilterUsername: String? = null

    /** Stores the page the user was on before enabling filtering, so they can jump back  */
    private var pageBeforeFiltering = 0

    var pageNumber: Int = FIRST_PAGE
        private set
    private var currentThreadId: Int = NULL_THREAD_ID

    // TODO: fix this it's all over the place, getting assigned as 1 in loadThread etc - maybe it should default to FIRST_PAGE?
    /** Current thread's last page  */
    private var lastPage = 0

    /**
     * Get the current thread's parent forum's ID.
     * 
     * @return the parent forum's ID, or 0 if something went wrong
     */
    var parentForumId: Int = 0
        private set
    private var threadLocked = false
    private var threadBookmarked = false
    private var threadArchived = false
    private var threadLockableUnlockable = false

    private var keepScreenOn = false

    //oh god i'm replicating core android functionality, this is a bad sign.
    private val backStack: LinkedList<AwfulStackEntry?> = LinkedList()
    private var bypassBackStack = false

    private var mTitle: String? = null
    private var postJump = ""
    private var savedScrollPosition = 0

    /** Whether the currently displayed page represents a full page of posts  */
    private var displayingFullPage = false

    private var shareProvider: ShareActionProvider? = null

    private var parentActivity: ForumsIndexActivity? = null

    private val mSelf = this

    private var pendingNavigation: NavigationEvent? = null


    private val ignorePostsHtml = HashMap<String?, String?>()
    private var redirect: AsyncTask<Void?, Void?, String?>? = null
    private var downloadLink: Uri? = null

    private val mThreadObserver = ThreadContentObserver(handler)


    override fun onCreateView(
        aInflater: LayoutInflater,
        aContainer: ViewGroup?,
        aSavedState: Bundle?
    ): View? {
        return try {
            inflateView(R.layout.thread_display, aContainer, aInflater)
        } catch (e: InflateException) {
            if (webViewIsMissing(e)) {
                null
            } else {
                throw e
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageBar = view.findViewById(R.id.page_bar)
        pageBar?.setListener(object : PageBarCallbacks {
            override fun onPageNavigation(nextPage: Boolean) {
                turnPage(nextPage)
            }

            override fun onRefreshClicked() {
                refresh()
            }

            override fun onPageNumberClicked() {
                displayPagePicker()
            }
        })
        awfulActivity?.setPreferredFont(pageBar?.textView)

        if (savedInstanceState != null) {
            // setting this before the thread view is initialized, so it will reflect the stored state
            keepScreenOn = savedInstanceState.getBoolean(KEEP_SCREEN_ON_KEY)
        }
        mThreadView = view.findViewById(R.id.thread)
        initThreadViewProperties()

        mUserPostNotice = view.findViewById(R.id.thread_userpost_notice)
        refreshProbationBar()

        mFAB = view.findViewById(R.id.just_post)
        mFAB?.setOnClickListener(onButtonClick)
        mFAB?.hide()

        refreshLayout = view.findViewById(R.id.thread_swipe)
        refreshLayout?.let {
            it.onRefresh = ::refresh
            if (!prefs.disablePullNext) {
                it.onLoadMore = {
                    if(!displayingFullPage){
                        refresh()
                    } else {
                        turnPage(true)
                    }
                }
            }
        }
    }


    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)

        setHasOptionsMenu(true)
        parentActivity = activity as ForumsIndexActivity?
        mPostLoaderCallback = PostLoaderManager()
        mThreadLoaderCallback = ThreadDataCallback()

        // if a navigation event is pending, we don't care about any saved state - just do the navigation
        pendingNavigation?.let {
            d("Activity attached: found pending navigation event, going there")
            val event = pendingNavigation!!
            pendingNavigation = null
            navigate(event)
            return
        }

        var loadFromCache = false
        if (aSavedState != null) {
            // restoring old state - we have a thread ID and page
            // TODO: 04/05/2017 post filtering state isn't restored properly - need to do filtering AND maintain filtered page/position AND recreate the backstack/'go back' UI
            i("Restoring fragment - loading cached posts from database")
            this.threadId = aSavedState.getInt(
                THREAD_ID_KEY,
                NULL_THREAD_ID
            )
            this.pageNumber = aSavedState.getInt(
                THREAD_PAGE_KEY,
                FIRST_PAGE
            )
            // TODO: 04/05/2017 saved scroll position doesn't seem to actually get used to set the position?
            savedScrollPosition = aSavedState.getInt(SCROLL_POSITION_KEY, 0)
            loadFromCache = true
        }
        // no valid thread ID means do nothing I guess? If the intent that created the activity+fragments didn't request a thread
        if (this.threadId <= 0) {
            return
        }
        // if we recreated the fragment (and had a valid thread ID) we just want to load the cached page data,
        // so we get the same state as before (we don't want to reload the page and e.g. have all the posts marked as seen)
        if (loadFromCache) {
            refreshPosts()
            refreshInfo()
        } else {
            syncThread()
        }
        updateUiElements()
    }


    /**
     * Check if an InflateException is caused by a missing WebView.
     * 
     * 
     * Also displays a message for the user.
     * 
     * @param e the exception thrown when inflating the layout
     * @return true if the WebView is missing
     */
    private fun webViewIsMissing(e: InflateException): Boolean {
        val message = e.message
        if (message == null || !message.lowercase(Locale.getDefault()).contains("webview")) {
            return false
        }
        w("Can't inflate thread view, WebView package is updating?:\n")
        e.printStackTrace()
        alertView
            .setIcon(R.drawable.ic_error)
            .setTitle(R.string.web_view_missing_alert_title)
            .setSubtitle(R.string.web_view_missing_alert_message)
            .show()
        return true
    }


    private val threadWebViewClient: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(aView: WebView, aUrl: String): Boolean {
            val aLink = AwfulURL.parse(aUrl)
            when (aLink.type) {
                TYPE.FORUM -> navigate(
                    NavigationEvent.Forum(
                        aLink.id.toInt(),
                        aLink.page.toInt()
                    )
                )

                TYPE.THREAD -> if (aLink.isRedirect) {
                    startPostRedirect(aLink.getURL(prefs.postPerPage))
                } else {
                    pushThread(
                        aLink.id.toInt(),
                        aLink.page.toInt(),
                        aLink.fragment?.replace(Regex("\\D"), "")
                    )
                }

                TYPE.POST -> startPostRedirect(aLink.getURL(prefs.postPerPage))
                TYPE.EXTERNAL -> if (prefs.alwaysOpenUrls) {
                    startUrlIntent(aUrl)
                } else {
                    showUrlMenu(aUrl)
                }

                TYPE.MODQUEUE_REQUEST -> navigate(
                    ModQueueRequest(
                        aLink.isBanRequest,
                        aLink.id.toInt(),
                        aLink.targetPostId.toInt(),
                        aLink.threadId.toInt()
                    )
                )
                TYPE.BANLIST -> navigate(LepersColony(aLink.id.toInt()))
                TYPE.INDEX -> navigate(NavigationEvent.ForumIndex)
                else -> return true
            }
            return true
        }
    }


    private fun initThreadViewProperties() {
        if (mThreadView == null) {
            w("initThreadViewProperties called for null WebView")
            return
        }
        mThreadView?.let {
            it.webViewClient = threadWebViewClient
            it.webChromeClient = object : LoggingWebChromeClient(it) {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    setProgress(newProgress / 2 + 50) //second half of progress bar
                }
            }
            it.setJavascriptHandler(clickInterface)

            refreshSessionCookie()
            d("Setting up WebView container HTML")
            it.setContent(this.blankPage)
            it.keepScreenOn = keepScreenOn

            it.setDownloadListener { url, _, _, _, _ ->
                enqueueDownload(
                    Uri.parse(url)
                )
            }
        }
    }

    private fun updatePageBar() {
        pageBar?.updatePagePosition(this.pageNumber, this.lastPage)
        if (activity != null) {
            invalidateOptionsMenu()
        }
    }


    override fun onResume() {
        super.onResume()
        mThreadView?.onResume()
        requireActivity().contentResolver
            .registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver)
        refreshInfo()
    }


    override fun setAsFocusedPage() {
        mThreadView?.onResume()
        mThreadView?.keepScreenOn = keepScreenOn
    }

    override fun setAsBackgroundPage() {
        mThreadView?.keepScreenOn = false
        mThreadView?.onPause()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().contentResolver.unregisterContentObserver(mThreadObserver)
        loaderManager.destroyLoader(Constants.THREAD_INFO_LOADER_ID)
        mThreadView?.onPause()
    }

    override fun cancelNetworkRequests() {
        super.cancelNetworkRequests()
        NetworkUtils.cancelRequests(REQUEST_TAG)
    }


    override fun onDestroy() {
        super.onDestroy()
        loaderManager.destroyLoader(Constants.POST_LOADER_ID)
    }

    @Synchronized
    private fun refreshSessionCookie() {
        if (mThreadView != null) {
            val cookieMonster = CookieManager.getInstance()
            cookieMonster.removeAllCookies(null /* status not interesting/actionable */)
            cookieMonster.setCookie(
                Constants.COOKIE_DOMAIN, CookieController.getCookieString(
                    Constants.COOKIE_NAME_SESSIONID
                )
            )
            cookieMonster.setCookie(
                Constants.COOKIE_DOMAIN, CookieController.getCookieString(
                    Constants.COOKIE_NAME_SESSIONHASH
                )
            )
            cookieMonster.setCookie(
                Constants.COOKIE_DOMAIN, CookieController.getCookieString(
                    Constants.COOKIE_NAME_USERID
                )
            )
            cookieMonster.setCookie(
                Constants.COOKIE_DOMAIN, CookieController.getCookieString(
                    Constants.COOKIE_NAME_PASSWORD
                )
            )

            // Add the captcha cookie if it is present.
            val captchaCookie = CookieController.getCookieString(Constants.COOKIE_NAME_CAPTCHA)
            if (captchaCookie.isNotEmpty()) {
                cookieMonster.setCookie(Constants.COOKIE_DOMAIN_CAPTCHA, captchaCookie)
            }

            cookieMonster.setAcceptThirdPartyCookies(mThreadView, true)
            cookieMonster.flush()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        if (menu.isEmpty()) {
            inflater.inflate(R.menu.post_menu, menu)
            val share = menu.findItem(R.id.share_thread)
            if (share != null && MenuItemCompat.getActionProvider(share) is ShareActionProvider) {
                shareProvider = MenuItemCompat.getActionProvider(share) as ShareActionProvider?
                shareProvider?.setShareIntent(createShareIntent(null))
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (activity == null) {
            return
        }
        val lockUnlock = menu.findItem(R.id.lock_unlock)
        if (lockUnlock != null) {
            lockUnlock.isVisible = threadLockableUnlockable
            lockUnlock.title = (if (threadLocked) getString(R.string.thread_unlock) else getString(
                R.string.thread_lock
            ))
        }
        val find = menu.findItem(R.id.find)
        if (find != null) {
            find.isVisible = true
        }
        val reply = menu.findItem(R.id.reply)
        if (reply != null) {
            reply.isVisible = prefs.noFAB
        }
        val bk = menu.findItem(R.id.bookmark)
        if (bk != null) {
            if (threadArchived) {
                bk.title = getString(R.string.bookmarkarchived)
            } else {
                bk.title = (if (threadBookmarked) getString(R.string.unbookmark) else getString(R.string.bookmark))
            }
            bk.isEnabled = !threadArchived
        }
        val screen = menu.findItem(R.id.keep_screen_on)
        if (screen != null) {
            screen.isChecked = keepScreenOn
        }
        val yospos = menu.findItem(R.id.yospos)
        if (yospos != null) {
            yospos.isVisible = this.parentForumId == Constants.FORUM_ID_YOSPOS
        }
        val fm = getInstance()
        for (i in 0..<menu.size) {
            fm.setMenuItemFont(menu[i])
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.lock_unlock -> showThreadLockUnlockDialog()
            R.id.reply -> displayPostReplyDialog()
            R.id.next_page -> turnPage(true)
            R.id.rate_thread -> rateThread()
            R.id.copy_url -> copyThreadURL(null, postFilterUserId)
            R.id.find -> (item.actionView as WebViewSearchBar).webView = mThreadView
            R.id.keep_screen_on -> {
                this.toggleScreenOn()
                item.isChecked = !item.isChecked
            }

            R.id.bookmark -> toggleThreadBookmark()
            R.id.yospos -> toggleYospos()
            R.id.show_self -> showUsersPosts(prefs.userId, prefs.username)
            R.id.search_this_thread -> {
                val threadFilter =
                    SearchFilter(SearchFilter.FilterType.ThreadId, currentThreadId.toString())
                navigate(SearchForums(threadFilter))
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }


    /**
     * Get a URL that links to a particular thread.
     * 
     * @param postId An optional post ID, appended as the URL's fragment
     * @param userId An optional user ID, appended as a query parameter
     * @return the full URL
     */
    private fun generateThreadUrl(postId: Int?, userId: Int?): String {
        val builder = Constants.FUNCTION_THREAD.toUri().buildUpon()
            .appendQueryParameter(Constants.PARAM_THREAD_ID, this.threadId.toString())
            .appendQueryParameter(Constants.PARAM_PAGE, this.pageNumber.toString())
            .appendQueryParameter(Constants.PARAM_PER_PAGE, prefs.postPerPage.toString())
        if (userId != null) {
            builder.appendQueryParameter(Constants.PARAM_USER_ID, userId.toString())
        }
        if (postId != null) {
            builder.fragment("post$postId")
        }
        return builder.toString()
    }


    /**
     * Get a URL that links to a particular post.
     * 
     * @param postId The ID of the post to link to
     * @return the full URL
     */
    private fun generatePostUrl(postId: Int): String {
        return Constants.FUNCTION_THREAD.toUri().buildUpon()
            .appendQueryParameter(Constants.PARAM_GOTO, Constants.VALUE_POST)
            .appendQueryParameter(Constants.PARAM_POST_ID, postId.toString())
            .toString()
    }


    /**
     * Get a share intent for a url.
     * 
     * 
     * If url is null, a link to the current thread will be generated.
     * 
     * @param url The url to share
     */
    fun createShareIntent(url: String?): Intent {
        var url = url
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        if (url == null) {
            // we're sharing the current thread - we can add the title in here
            intent.putExtra(Intent.EXTRA_SUBJECT, mTitle)
            url = generateThreadUrl(null, postFilterUserId)
        }
        return intent.putExtra(Intent.EXTRA_TEXT, url)
    }


    /**
     * Copy a thread's URL to the clipboard
     * @param postId    An optional post ID, used as the url's fragment
     * @param userId    An optional user ID, appended to the url as a parameter
     */
    fun copyThreadURL(postId: Int?, userId: Int?) {
        val clipLabel = getString(R.string.copy_url) + this.pageNumber
        val clipText = generateThreadUrl(postId, userId)
        safeCopyToClipboard(clipLabel, clipText, R.string.copy_url_success)
    }


    /**
     * Display a thread-rating dialog.
     * 
     * This handles the network request to submit the vote, and user feedback.
     */
    private fun rateThread() {
        val items = arrayOf<CharSequence?>("1", "2", "3", "4", "5")
        val activity: Activity? = this.activity

        AlertDialog.Builder(activity)
            .setTitle("Rate this thread")
            .setItems(
                items
            ) { _: DialogInterface?, item: Int ->
                queueRequest(
                    VoteRequest(
                        requireActivity(),
                        this.threadId, item + 1
                    )
                        .build(this@ThreadDisplayFragment, object : AwfulResultCallback<Void?> {
                            override fun success(result: Void?) {
                                alertView.setTitle(R.string.vote_succeeded)
                                    .setSubtitle(R.string.vote_succeeded_sub)
                                    .setIcon(R.drawable.ic_mood)
                                    .show()
                            }


                            override fun failure(error: VolleyError?) {
                            }
                        })
                )
            }.show()
    }


    /**
     * Add a user to the ignore list.
     * 
     * @param userId The awful ID of the user
     */
    fun ignoreUser(userId: Int) {
        val activity: Activity? = getActivity()
        if (prefs.ignoreFormkey == null) {
            queueRequest(RefreshUserProfileRequest(requireActivity()).build())
        }
        if (prefs.showIgnoreWarning) {
            val onClickListener =
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    if (which == AlertDialog.BUTTON_NEUTRAL) {
                        // cancel future alerts if the user clicks the "don't warn" option
                        prefs.setPreference(BooleanPreference.SHOW_IGNORE_WARNING, false)
                    }
                    doIgnoreUser(requireActivity(), userId)
                }

            AlertDialog.Builder(activity)
                .setPositiveButton(R.string.confirm, onClickListener)
                .setNeutralButton(R.string.dont_show_again, onClickListener)
                .setNegativeButton(R.string.cancel, null)
                .setTitle(R.string.ignore_title)
                .setMessage(R.string.ignore_message)
                .show()
        } else {
            doIgnoreUser(requireActivity(), userId)
        }
    }


    /**
     * Carry out the ignore user request
     */
    private fun doIgnoreUser(context: Context, userId: Int) {
        //we don't care about status callbacks for this, so we use the build() that doesn't do callbacks
        queueRequest(IgnoreRequest(context, userId).build())
    }


    /**
     * Toggle a user as marked or unmarked.
     */
    fun toggleMarkUser(username: String?) {
        if (prefs.markedUsers?.contains(username) == true) {
            prefs.unmarkUser(username)
        } else {
            prefs.markUser(username)
        }
    }


    /**
     * Toggle between displaying a single user's posts, or all posts
     * @param aPostId    The ID of the post to display, if toggling filtering off
     * @param aUserId    The ID of the user whose posts we're showing, if toggling on
     * @param aUsername    The username of the user, if toggling on
     */
    // TODO: refactor this and the methods it calls - it's so weird
    fun toggleUserPosts(aPostId: Int, aUserId: Int, aUsername: String?) {
        if (postFilterUserId != null) {
            showAllPosts(aPostId)
        } else {
            showUsersPosts(aUserId, aUsername)
        }
    }


    /**
     * Display a dialog to report a post
     * 
     * @param postId    The ID of the bad post
     */
    fun reportUser(postId: Int) {
        queueRequest(
            ReportCheckRequest(requireActivity(), postId)
                .build(
                    this@ThreadDisplayFragment,
                    object : AwfulResultCallback<ReportCheckResult> {
                        override fun success(result: ReportCheckResult) {
                            if (result.alreadyReported) {
                                alertView.setTitle("This post has already been reported recently")
                                    .setIcon(R.drawable.ic_mood).show()
                            } else {
                                showReportDialog(
                                    postId,
                                    result.warning ?: ""
                                )
                            }
                        }

                        override fun failure(error: VolleyError?) {
                            alertView.setTitle("Failed to check report status")
                                .setIcon(R.drawable.ic_mood).show()
                        }
                    })
        )
    }

    private fun showReportDialog(postId: Int, warning: String) {
        val reportReason = EditText(this.activity)

        val body = getString(R.string.report_post_message)
        val message: CharSequence?
        if (warning.isEmpty()) {
            message = body
        } else {
            val warningColor = resources.getColor(R.color.popup_warning_text)
            val warningSpan = SpannableString(warning)
            warningSpan.setSpan(ForegroundColorSpan(warningColor), 0, warning.length, 0)
            val sb = SpannableStringBuilder()
            sb.append(warningSpan)
            sb.append("\n\n")
            sb.append(body)
            message = sb
        }

        AlertDialog.Builder(this.activity)
            .setTitle("Report inappropriate post")
            .setMessage(message)
            .setView(reportReason)
            .setPositiveButton(
                "Report"
            ) { dialog: DialogInterface?, whichButton: Int ->
                val reason = reportReason.text.toString()
                queueRequest(
                    ReportRequest(
                        requireActivity(),
                        postId,
                        reason
                    ).build(this@ThreadDisplayFragment, object : AwfulResultCallback<String> {
                        override fun success(result: String) {
                            alertView.setTitle(result).setIcon(R.drawable.ic_mood).show()
                        }

                        override fun failure(error: VolleyError?) {
                            alertView.setTitle(error?.message).setIcon(R.drawable.ic_mood).show()
                        }
                    })
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Toggles whether to display this user's avatar or not
     * 
     * @param avatarUrl        The URL of the avatar to toggle the display of. Ignored if null or empty string.
     */
    fun toggleAvatar(avatarUrl: String?) {
        if (TextUtils.isEmpty(avatarUrl)) {
            return
        }
        val blocked = prefs.getPreference(StringSetPreference.BLOCKED_AVATAR_URLS, mutableSetOf())
        val newSet: MutableSet<String?> =
            HashSet(blocked) // not allowed to mutate original set

        if (!newSet.remove(avatarUrl)) {
            newSet.add(avatarUrl)
        }
        prefs.setPreference(StringSetPreference.BLOCKED_AVATAR_URLS, newSet)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        d("onSaveInstanceState - storing thread ID, page number and scroll position")
        outState.putInt(THREAD_ID_KEY, this.threadId)
        outState.putInt(THREAD_PAGE_KEY, this.pageNumber)
        if(mThreadView != null) {
            outState.putInt(SCROLL_POSITION_KEY, mThreadView!!.scrollY)
        }
        outState.putBoolean(KEEP_SCREEN_ON_KEY, keepScreenOn)
    }


    /**
     * Reload the current thread page
     */
    private fun syncThread() {
        val activity: Activity? = getActivity()
        if (activity != null) {
            i(
                "Syncing - reloading from site (thread %d, page %d) to update DB",
                this.threadId,
                this.pageNumber
            )
            // cancel pending post loading requests
            NetworkUtils.cancelRequests(REQUEST_TAG)
            // call this with cancelOnDestroy=false to retain the request's specific type tag
            val pageNumber = this.pageNumber
            val userId = postFilterUserId ?: BLANK_USER_ID
            queueRequest(
                ThreadPageRequest(activity, this.threadId, pageNumber, userId)
                    .build(this, object : AwfulResultCallback<Void?> {
                        override fun success(result: Void?) {
                            refreshLayout?.finishedLoading()
                            refreshInfo()
                            setProgress(75)
                            refreshPosts()
                        }

                        override fun failure(error: VolleyError?) {
                            error?.let {handleCaptchaChallenge(activity, it)}
                            w("Failed to sync thread! Error: %s", error?.message)
                            refreshLayout?.finishedLoading()
                            refreshInfo()
                            refreshPosts()
                        }
                    }), false
            )
        }
    }


    /**
     * Mark a post as the last read in this thread.
     * 
     * 
     * This takes an attribute in the HTML called `data-idx`, which is basically
     * an enumeration of the posts in the thread.
     * 
     * @param index The `data-idx` value of the post.
     */
    fun markLastRead(index: Int) {
        alertView.setTitle(R.string.mark_last_read_progress)
            .setSubtitle(R.string.please_wait_subtext)
            .setIcon(R.drawable.ic_visibility)
            .show()

        queueRequest(
            MarkLastReadRequest(requireActivity(), this.threadId, index)
                .build(null, object : AwfulResultCallback<Void?> {
                    override fun success(result: Void?) {
                        if (activity != null) {
                            alertView.setTitle(R.string.mark_last_read_success)
                                .setIcon(R.drawable.ic_visibility)
                                .show()
                            refreshInfo()
                            refreshPosts()
                        }
                    }


                    override fun failure(error: VolleyError?) {
                    }
                })
        )
    }


    /**
     * Toggle this thread's bookmarked status.
     */
    private fun toggleThreadBookmark() {
        val activity: Activity? = getActivity()
        if (activity != null) {
            queueRequest(
                BookmarkRequest(activity, this.threadId, !threadBookmarked)
                    .build(this, object : AwfulResultCallback<Void?> {
                        override fun success(result: Void?) {
                            refreshInfo()
                        }

                        override fun failure(error: VolleyError?) {
                            refreshInfo()
                        }
                    })
            )
        }
    }


    /**
     * Toggle between amberPOS and greenPOS, refreshing the display.
     */
    private fun toggleYospos() {
        prefs.amberDefaultPos = !prefs.amberDefaultPos
        prefs.setPreference(BooleanPreference.AMBER_DEFAULT_POS, prefs.amberDefaultPos)
        mThreadView?.runJavascript(
            String.format(
                "changeCSS('%s')", AwfulTheme.forForum(
                    this.parentForumId
                ).cssPath
            )
        )
    }


    /**
     * Reload with a new URL
     * @param postUrl    The URL of the post we should land on
     */
    private fun startPostRedirect(postUrl: String) {
        val activity = awfulActivity ?: return
        redirect?.cancel(false)
        setProgress(50)
        redirect = object : RedirectTask(postUrl) {
            override fun onPostExecute(url: String?) {
                if (isCancelled) {
                    return
                } else if (url == null) {
                    alertView.setDisplayLength(Toast.LENGTH_LONG).show(AwfulError())
                    return
                }

                val result = AwfulURL.parse(url)
                if (postUrl.contains(Constants.VALUE_LASTPOST)) {
                    //This is a workaround for how the forums handle the perPage value with goto=lastpost.
                    //The redirected url is lacking the perpage=XX value.
                    //We just override the assumed (40) with the number we requested when starting the redirect.
                    //I gotta ask chooch to fix this at some point.
                    result.setPerPage(prefs.postPerPage)
                }
                if (result.type == TYPE.THREAD) {
                    val threadId = result.id.toInt()
                    val threadPage = result.getPage(prefs.postPerPage).toInt()
                    val postJump = result.fragment
                    if (bypassBackStack) {
                        openThread(threadId, threadPage, postJump)
                    } else {
                        pushThread(threadId, threadPage, postJump)
                    }
                } else if (result.type == TYPE.INDEX) {
                    activity.navigate(NavigationEvent.ForumIndex)
                }
                redirect = null
                bypassBackStack = false
                setProgress(100)
            }
        }.execute()
    }


    /**
     * Show the page picker dialog, and handle user input and navigation.
     */
    private fun displayPagePicker() {
        val activity: FragmentActivity = activity ?: return

        PagePicker(
            activity,
            this.lastPage,
            this.pageNumber, object : MinMaxNumberPicker.ResultListener {
                override fun onButtonPressed(button: Int, resultValue: Int) {
                    if (button == DialogInterface.BUTTON_POSITIVE) {
                        goToPage(resultValue)
                    }
                }
            }).show()
    }


    override fun onActivityResult(aRequestCode: Int, aResultCode: Int, aData: Intent?) {
        d("onActivityResult - request code: %d, result: %d", aRequestCode, aResultCode)
        // If we're here because of a post result, refresh the thread
        when (aRequestCode) {
            PostReplyFragment.REQUEST_POST -> {
                bypassBackStack = true
                if (aResultCode == PostReplyFragment.RESULT_POSTED) {
                    startPostRedirect(
                        AwfulURL.threadLastPage(
                            this.threadId.toLong(),
                            prefs.postPerPage
                        ).getURL(prefs.postPerPage)
                    )
                } else if (aResultCode > 100) { //any result >100 it is a post id we edited
                    // TODO: >100 is a bit too magical
                    startPostRedirect(
                        AwfulURL.post(aResultCode.toLong(), prefs.postPerPage)
                            .getURL(prefs.postPerPage)
                    )
                }
            }
        }
    }


    /**
     * Refresh the page
     */
    private fun refresh() {
        showBlankPage()
        syncThread()
    }


    /**
     * Load the next or previous page.
     * 
     * The current page will reload if there is no next/previous page to move to.
     */
    private fun turnPage(forwards: Boolean) {
        val currentPage = this.pageNumber
        val limit = if (forwards) this.lastPage else FIRST_PAGE
        if (currentPage == limit) {
            refresh()
        } else {
            goToPage(currentPage + (if (forwards) 1 else -1))
        }
    }


    /**
     * General click listener for thread view widgets
     */
    private val onButtonClick = View.OnClickListener { aView: View? ->
        if (aView?.id == R.id.just_post) {
            displayPostReplyDialog()
        }
    }

    private fun displayPostReplyDialog() {
        displayPostReplyDialog(this.threadId, -1, AwfulMessage.TYPE_NEW_REPLY)
    }


    /**
     * Show a dialog that allows the user to lock or unlock the current thread, as appropriate.
     */
    private fun showThreadLockUnlockDialog() {
        AlertDialog.Builder(activity)
            .setTitle(getString(if (threadLocked) R.string.thread_unlock else R.string.thread_lock) + "?")
            .setPositiveButton(
                R.string.alert_ok
            ) { dialogInterface: DialogInterface?, i: Int -> toggleThreadLock() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    /**
     * Trigger a request to toggle the current thread's locked/unlocked state.
     */
    private fun toggleThreadLock() {
        queueRequest(
            ThreadLockUnlockRequest(requireActivity(), this.threadId).build(
                mSelf,
                object : AwfulResultCallback<Void?> {
                    override fun success(result: Void?) {
                        // TODO: maybe this should trigger a thread data refresh instead, update everything from the source
                        threadLocked = !threadLocked
                    }

                    override fun failure(error: VolleyError?) {
                        e(
                            String.format(
                                "Couldn\'t %s this thread",
                                if (threadLocked) "unlock" else "lock"
                            )
                        )
                    }
                })
        )
    }

    private fun populateThreadView(aPosts: MutableList<AwfulPost>) {
        if (mThreadView == null) {
            w("populateThreadView called with null WebView")
            return
        }
        updateUiElements()

        try {
            d("populateThreadView: displaying %d posts", aPosts.size)
            val html = AwfulHtmlPage.getThreadHtml(
                aPosts, AwfulPreferences.getInstance(requireActivity()),
                this.pageNumber,
                this.lastPage
            )
            refreshSessionCookie()
            mThreadView?.setBodyHtml(html)
            displayingFullPage =
                aPosts.size >= prefs.postPerPage // shouldn't ever be > but just to be safe
            setProgress(100)
        } catch (e: Exception) {
            // If we've already left the activity the webview may still be working to populate,
            // just log it
            e(e, "populateThreadView: display failed")
        }
    }

    private val clickInterface = ClickInterface()

    fun getPostJump(): String {
        return postJump
    }

    private fun setPostJump(postJump: String) {
        // TODO: this strips out any prefix (so it handles prefixed fragments AND bare IDs) and adds the required prefix to all. Might be better to handle this in AwfulURL?
        this.postJump = "post" + postJump.replace("\\D".toRegex(), "")
    }

    private inner class ClickInterface : WebViewJsInterface() {
        @JavascriptInterface
        fun onMoreClick(
            aPostId: String,
            aUsername: String,
            aUserId: String,
            lastReadUrl: String,
            editable: Boolean,
            hasModControls: Boolean,
            posterRole: String?,
            isPlat: Boolean,
            avatarUrl: String
        ) {
            val postActions = PostContextMenu.newInstance(
                this@ThreadDisplayFragment.threadId,
                aPostId.toInt(),
                lastReadUrl.toInt(),
                editable,
                hasModControls,
                aUsername,
                aUserId.toInt(),
                isPlat,
                posterRole,
                postFilterUserId,
                avatarUrl
            )

            postActions.setTargetFragment(this@ThreadDisplayFragment, -1)
            postActions.setOnActionClickedListener(object : OnActionClickedListener<PostMenuAction?> {
                override fun onActionClicked(action: PostMenuAction?) {
                    when(action) {
                        PostMenuAction.HIDE_AVATAR -> mThreadView?.evaluateJavascript(
                            String.format(
                                "hideAvatar('%s')",
                                avatarUrl
                            ), null
                        )
                        PostMenuAction.SHOW_AVATAR -> mThreadView?.evaluateJavascript(
                            String.format(
                                "showAvatar('%s')",
                                avatarUrl
                            ), null
                        )

                        else -> return
                    }
                }
            })
            postActions.show(mSelf.requireFragmentManager(), "Post Actions")
        }

        override fun setCustomPreferences(preferences: MutableMap<String, String>) {
            // TODO: 23/01/2017 add methods so you can't mess with the map directly
            preferences["postjumpid"] = postJump
            preferences["scrollPosition"] = savedScrollPosition.toString()
        }

        @JavascriptInterface
        fun getIgnorePostHtml(id: String?): String? {
            return ignorePostsHtml[id]
        }

        @JavascriptInterface
        fun getPostJump(): String {
            return postJump
        }

        @get:JavascriptInterface
        val cSS: String
            get() = AwfulTheme.forForum(this@ThreadDisplayFragment.parentForumId).cssPath


        @JavascriptInterface
        fun loadIgnoredPost(ignorePost: String) {
            queueRequest(
                SinglePostRequest(requireActivity(), ignorePost).build(
                    mSelf,
                    object : AwfulResultCallback<String> {
                        override fun success(result: String) {
                            ignorePostsHtml[ignorePost] = result
                            mThreadView?.runJavascript(
                                String.format(
                                    "insertIgnoredPost('%s')",
                                    ignorePost
                                )
                            )
                        }

                        override fun failure(error: VolleyError?) {
                            w("Failed to load ignored post #$ignorePost")
                        }
                    })
            )

        }

        @JavascriptInterface
        fun haltSwipe() {
            (mSelf.awfulActivity as ForumsIndexActivity).preventSwipe()
        }

        @JavascriptInterface
        fun resumeSwipe() {
            (mSelf.awfulActivity as ForumsIndexActivity).allowSwipe()
        }

        @JavascriptInterface
        fun popupText(text: String?) {
            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun openUrlMenu(url: String?) {
            showUrlMenu(url)
        }

        @JavascriptInterface
        fun displayImageZoom(url: String?) {
            displayImage(url)
        }
    }


    private fun showUrlMenu(url: String?) {
        if (url == null) {
            w("Passed null URL to #showUrlMenu!")
            return
        }
        val fragmentManager = getFragmentManager()
        if (fragmentManager == null) {
            w("showUrlMenu called but can't get FragmentManager!")
            return
        }
        if (fragmentManager.isStateSaved) {
            // probably got a javascript callback after the fragment was stopped,
            // easiest to just let them tap for the menu again when they come back
            return
        }

        var isImage = false
        var isGif = false
        // TODO: parsing fails on magic webdev urls like http://tpm2016.zoffix.com/#/40
        // it thinks the # is the start of the ref section of the url, so the Path for that url is '/'
        val path = url.toUri()
        var lastSegment = path.lastPathSegment
        // null-safe path checking (there may be no path segments, e.g. a link to a domain name)
        if (lastSegment != null) {
            lastSegment = lastSegment.lowercase(Locale.getDefault())
            // using 'contains' instead of 'ends with' in case of any url suffix shenanigans, like twitter's ".jpg:large"
            // TODO: 08/08/2019 make general functions for identifying images etc since we need to do this in multiple places
            isImage =
                (StringUtils.indexOfAny(lastSegment, ".jpg", ".jpeg", ".png", ".gif", ".webp") != -1
                        && !StringUtils.contains(lastSegment, ".gifv"))
                        || (lastSegment == "attachment.php" && path.host == "forums.somethingawful.com")
            isGif = StringUtils.contains(lastSegment, ".gif")
                    && !StringUtils.contains(lastSegment, ".gifv")
        }
        var linkUrl: String = url
        val youtube = Pattern.compile("youtube\\.com/watch\\?v=([a-zA-Z0-9-_]+).*").matcher(linkUrl)
        if (youtube.find()) {
            linkUrl =
                path.scheme + "://" + path.authority + path.path + "?v=" + youtube.group(
                    1
                )
        } else if (Strings.CS.contains(
                path.host,
                "twitter.com"
            ) || Strings.CS.contains(path.host, "x.com") || Strings.CS.contains(
                path.host,
                "bsky.app"
            )
        ) {
            linkUrl = "${path.scheme}://${path.authority}${path.path}"
        }

        val linkActions = UrlContextMenu.newInstance(
            linkUrl,
            isImage,
            isGif,
            if (isGif) "Getting file size" else null
        )

        if (isGif || !AwfulPreferences.getInstance().canLoadImages()) {
            queueRequest(ImageSizeRequest(linkUrl, Response.Listener { result: Int? ->
                if (linkActions == null) {
                    return@Listener
                }
                val size = if (result == null) "unknown" else Formatter.formatShortFileSize(
                    context,
                    result.toLong()
                )
                linkActions.setSubheading(String.format("Size: %s", size))
            }))
        }
        linkActions.setTargetFragment(this@ThreadDisplayFragment, -1)
        linkActions.show(fragmentManager, "Link Actions")
    }

    fun showImageInline(url: String) {
        mThreadView?.runJavascript(String.format("showInlineImage('%s')", url))
    }

    fun enqueueDownload(link: Uri) {
        if (!AwfulUtils.isTiramisu33) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                downloadLink = link
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Constants.AWFUL_PERMISSION_WRITE_EXTERNAL_STORAGE
                )
                return
            }
        }
        val request = DownloadManager.Request(link)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        if (link.lastPathSegment == "attachment.php" && link.host == "forums.somethingawful.com") {
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "attachment.png"
            )
            request.addRequestHeader(
                "Cookie",
                CookieManager.getInstance().getCookie(Constants.COOKIE_DOMAIN)
            )
        } else {
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                link.lastPathSegment
            )
        }
        request.allowScanningByMediaScanner()
        request.setTitle(link.lastPathSegment)

        val dlManager =
            awfulActivity?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dlManager.enqueue(request)
    }

    fun copyToClipboard(text: String) {
        safeCopyToClipboard("Copied URL", text, null)
        alertView
            .setTitle(R.string.copy_url_success)
            .setIcon(R.drawable.ic_insert_link)
            .show()
    }

    fun startUrlIntent(url: String?) {
        val intentUri = Uri.parse(url)
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, intentUri)
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            requireActivity().startActivity(browserIntent)
        } catch (error: ActivityNotFoundException) {
            alertView.setTitle("Cannot open link:")
                .setDisplayLength(Toast.LENGTH_LONG)
                .setSubtitle("None of your apps want to open this " + intentUri.scheme + ":\\\\ link. Try installing an app that is less picky")
                .show()
        }
    }

    fun displayImage(url: String?) {
        val intent =
            BasicActivity.intentFor(ZoomViewFragment::class.java, requireActivity(), "")
        intent.putExtra(ZoomViewFragment.EXTRA_IMAGE_URL, url)
        startActivity(intent)
    }

    override fun onPreferenceChange(preferences: AwfulPreferences, key: String?) {
        super.onPreferenceChange(preferences, key)
        i("onPreferenceChange%s", (if (key != null) ":$key" else ""))
        if (null != awfulActivity && pageBar != null) {
            awfulActivity?.setPreferredFont(pageBar?.textView)
            pageBar?.setTextColour(ColorProvider.ACTION_BAR_TEXT.color)
        }
        refreshLayout?.let {
            it.spinnerTint = ColorProvider.getARLActiveColor(parentForumId)
            it.spinnerTintInert = ColorProvider.getARLInertColor(parentForumId)
        }

        mThreadView?.let {

            it.setBackgroundColor(Color.TRANSPARENT)
            it.runJavascript(String.format("changeFontFace('%s')", preferences.preferredFont))
            it.settings.defaultFontSize = preferences.postFontSizeSp
            it.settings.defaultFixedFontSize = preferences.postFixedFontSizeSp

            if ("marked_users" == key) {
                it.runJavascript(
                    String.format(
                        "updateMarkedUsers('%s')",
                        TextUtils.join(",", preferences.markedUsers!!)
                    )
                )
            }
        }
        clickInterface.updatePreferences()
        if (mFAB != null) {
            if (preferences.noFAB) {
                mFAB?.hide()
            } else {
                mFAB?.show()
            }
        }
    }


    /**
     * Update any UI elements that need to be refreshed.
     */
    private fun updateUiElements() {
        // TODO: probably more things can be put in here, there's a lot to unravel
        updatePageBar()
        refreshProbationBar()

        refreshLayout?.let {
            it.spinnerTint = ColorProvider.getARLActiveColor(parentForumId)
            it.spinnerTintInert = ColorProvider.getARLInertColor(parentForumId)
        }
    }


    /**
     * Load a specific page in the current thread.
     * 
     * This method does nothing if the page number is not valid (i.e. between [.FIRST_PAGE] and the last page).
     * @param aPage    a page number for this thread
     */
    private fun goToPage(aPage: Int) {
        if (aPage <= 0 || aPage > this.lastPage) {
            return
        }
        this.pageNumber = aPage
        updateUiElements()
        setPostJump("")
        showBlankPage()
        syncThread()
    }


    private val blankPage: String
        /**
         * Get an empty page structure, themed according to the thread's parent forum
         * @return    The basic page HTML, with no post content
         */
        get() = AwfulHtmlPage.getContainerHtml(prefs, this.parentForumId, true)

    var threadId: Int
        get() = currentThreadId
        private set(aThreadId) {
            currentThreadId = aThreadId
            if (activity != null) {
                (activity as ForumsIndexActivity).onPageContentChanged()
            }
        }


    /**
     * Show posts filtered to a specific user
     * @param id    the user's ID
     * @param name    the user's username
     */
    private fun showUsersPosts(id: Int, name: String?) {
        // TODO: legend has it this doesn't work and shows other people's posts if the page isn't full
        pageBeforeFiltering = this.pageNumber
        setPostFiltering(id, name)
        this.pageNumber = FIRST_PAGE
        this.lastPage = FIRST_PAGE
        setPostJump("")
        refresh()
    }


    /**
     * Clear filtering added by [.showUsersPosts] and return to a specific post
     * @param postId    The ID of the post to navigate to
     */
    private fun showAllPosts(postId: Int?) {
        if (postId != null) {
            showBlankPage()
            openThread(AwfulURL.parse(generatePostUrl(postId)))
        } else {
            setPostFiltering(null, null)
            this.pageNumber = pageBeforeFiltering
            this.lastPage = 0
            setPostJump("")
            refresh()
        }
    }


    /**
     * Set or unset the "show user's posts" filtering state.
     * @param userId    The ID of the user to filter to, or null for no filtering
     * @param username    The username of the user. If ID is null, this is ignored
     */
    private fun setPostFiltering(userId: Int?, username: String?) {
        postFilterUserId = userId
        postFilterUsername = if (userId == null) null else username
    }


    /**
     * Clear the thread display, e.g. to show a blank page before loading new content
     */
    private fun showBlankPage() {
        mThreadView?.setBodyHtml(null)
    }


    private inner class PostLoaderManager : LoaderManager.LoaderCallbacks<Cursor> {
        private val sortOrder = AwfulPost.POST_INDEX + " ASC"
        private val selection = AwfulPost.THREAD_ID + "=? AND " + AwfulPost.POST_INDEX + ">=? AND " + AwfulPost.POST_INDEX + "<?"
        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor> {
            val index = AwfulPagedItem.pageToIndex(this@ThreadDisplayFragment.pageNumber, prefs.postPerPage, 0)
            i(
                "Loading page %d of thread %d from database\nStart index is %d with %d posts per page",
                this@ThreadDisplayFragment.pageNumber, this@ThreadDisplayFragment.threadId, index, prefs.postPerPage
            )
            return CursorLoader(
                requireActivity(),
                AwfulPost.CONTENT_URI,
                AwfulProvider.PostProjection,
                selection,
                AwfulProvider.int2StrArray(this@ThreadDisplayFragment.threadId, index, index + prefs.postPerPage),
                sortOrder
            )
        }

        override fun onLoadFinished(aLoader: Loader<Cursor?>, aData: Cursor) {
            setProgress(90)
            if (aData.isClosed) {
                return
            }
            if (mThreadView != null) {
                populateThreadView(AwfulPost.fromCursor(aData))
            }
            // TODO: 04/05/2017 sometimes you don't want this resetting, e.g. restoring fragment state
            savedScrollPosition = 0
        }

        override fun onLoaderReset(aLoader: Loader<Cursor?>) {
        }
    }


    private inner class ThreadDataCallback : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor> {
            return CursorLoader(
                requireActivity(),
                ContentUris.withAppendedId(AwfulThread.CONTENT_URI, this@ThreadDisplayFragment.threadId.toLong()),
                AwfulProvider.ThreadProjection,
                null,
                null,
                null
            )
        }

        override fun onLoadFinished(aLoader: Loader<Cursor>, aData: Cursor) {
            i("Loaded thread metadata, updating fragment state and UI")
            if (aData.count > 0 && aData.moveToFirst()) {
                this@ThreadDisplayFragment.lastPage = AwfulPagedItem.indexToPage(
                    aData.getInt(aData.getColumnIndexOrThrow(AwfulThread.POSTCOUNT)),
                    prefs.postPerPage
                )
                threadLocked = aData.getInt(aData.getColumnIndexOrThrow(AwfulThread.LOCKED)) > 0
                threadLockableUnlockable =
                    aData.getInt(aData.getColumnIndexOrThrow(AwfulThread.CAN_OPEN_CLOSE)) > 0
                threadBookmarked = aData.getInt(aData.getColumnIndexOrThrow(AwfulThread.BOOKMARKED)) > 0
                threadArchived = aData.getInt(aData.getColumnIndexOrThrow(AwfulThread.ARCHIVED)) > 0
                mTitle = aData.getString(aData.getColumnIndexOrThrow(AwfulThread.TITLE))
                this@ThreadDisplayFragment.parentForumId = aData.getInt(aData.getColumnIndexOrThrow(AwfulThread.FORUM_ID))
                if (this@ThreadDisplayFragment.parentForumId != 0) {
                    mThreadView?.runJavascript(
                        String.format(
                            "changeCSS('%s')", AwfulTheme.forForum(
                                this@ThreadDisplayFragment.parentForumId
                            ).cssPath
                        )
                    )
                }

                parentActivity?.onPageContentChanged()

                updateUiElements()
                if (mUserPostNotice != null) {
                    if (postFilterUserId != null) {
                        mUserPostNotice?.visibility = View.VISIBLE
                        mUserPostNotice?.text = String.format(
                            "Viewing posts by %s in this thread,\nPress the back button to return.",
                            postFilterUsername
                        )
                        mUserPostNotice?.setTextColor(ColorProvider.PRIMARY_TEXT.color)
                        mUserPostNotice?.setBackgroundColor(ColorProvider.BACKGROUND.color)
                    } else {
                        mUserPostNotice?.visibility = View.GONE
                    }
                }
                shareProvider?.setShareIntent(createShareIntent(null))
                invalidateOptionsMenu()
                if (mFAB != null) {
                    if (prefs.noFAB || threadLocked || threadArchived) {
                        mFAB?.hide()
                    } else {
                        mFAB?.show()
                    }
                }
            }
        }

        override fun onLoaderReset(aLoader: Loader<Cursor?>) {
        }
    }

    private inner class ThreadContentObserver(aHandler: Handler?) : ContentObserver(aHandler) {
        override fun onChange(selfChange: Boolean) {
            i("Thread metadata has been updated - forcing refresh")
            refreshInfo()
        }
    }


    /**
     * Refresh the displayed thread's data (bookmarked, locked etc.)
     * 
     * This loads from the database, and reflects the last cached status of the thread.
     * To actually download current data from the site call [.syncThread] instead.
     * @see ThreadDataCallback
     */
    private fun refreshInfo() {
        restartLoader(Constants.THREAD_INFO_LOADER_ID, null, mThreadLoaderCallback ?: return)
    }


    /**
     * Refresh the posts displayed, according to current setting (thread ID, page etc.)
     * 
     * This loads from the database, and reflects the last cached view of the thread.
     * To actually download updated data (including changes in posts' viewed status) call
     * [.syncThread] instead.
     * @see PostLoaderManager
     */
    private fun refreshPosts() {
        restartLoader(Constants.POST_LOADER_ID, null, mPostLoaderCallback ?: return)
    }


    fun setTitle(title: String) {
        mTitle = title
        parentActivity?.onPageContentChanged()
    }


    override fun getTitle(): String? {
        return mTitle
    }


    override fun handleNavigation(event: NavigationEvent): Boolean {
        // need to check if the fragment is attached to the activity - if not, defer any handled events until it is attached
        if (event is NavigationEvent.Thread) {
            if (!isAdded) {
                deferNavigation(event)
            } else {
                // if we're currently displaying this thread, and no page was specified (i.e. it's
                // a "show this thread" navigation) then we don't need to do anything
                if (event.id != currentThreadId || event.page != null) {
                    openThread(event.id, event.page, event.postJump)
                }
            }
            return true
        } else if (event is Url) {
            if (!isAdded) {
                deferNavigation(event)
            } else {
                openThread(event.url)
            }
            return true
        }
        return false
    }

    /**
     * Store a navigation event for handling when this fragment is attached to the activity
     */
    private fun deferNavigation(event: NavigationEvent) {
        d("Deferring navigation event(%s) - isAdded = %b", event, isAdded)
        pendingNavigation = event
    }


    /**
     * Open a thread, jumping to a specific page and post if required.
     * @param id          The thread's ID
     * @param page        An optional page to display, otherwise it defaults to the first page
     * @param postJump    An optional URL fragment representing the post ID to jump to
     */
    private fun openThread(id: Int, page: Int?, postJump: String?) {
        i(
            "Opening thread (old/new) ID:%d/%d, PAGE:%s/%s, JUMP:%s/%s",
            this.threadId, id, this.pageNumber, page, getPostJump(), postJump
        )
        clearBackStack()
        val threadPage = page ?: FIRST_PAGE
        loadThread(id, threadPage, postJump, true)
    }


    /**
     * Open a specific thread represented in an AwfulURL
     */
    private fun openThread(url: AwfulURL?) {
        // TODO: fix this prefs stuff, get it initialised somewhere consistent in the lifecycle, preferably in AwfulFragment
        // TODO: validate the AwfulURL, e.g. make sure it's the correct type
        if (url == null) {
            Toast.makeText(this.activity, "Error occurred: URL was empty", Toast.LENGTH_LONG)
                .show()
            return
        }
        clearBackStack()
        if (url.isRedirect) {
            startPostRedirect(url.getURL(prefs.postPerPage))
        } else {
            loadThread(
                url.id.toInt(),
                url.getPage(prefs.postPerPage).toInt(),
                url.fragment,
                true
            )
        }
    }


    /**
     * Load the thread represented in an AwfulStackEntry
     */
    private fun loadThread(thread: AwfulStackEntry) {
        loadThread(thread.id, thread.page, null, false)
    }


    /**
     * Actually load the new thread
     * @param id        The thread's ID
     * @param page        The number of the page to display
     * @param postJump    An optional URL fragment representing the post ID to jump to
     */
    private fun loadThread(id: Int, page: Int, postJump: String?, fullSync: Boolean) {
        this.threadId = id
        this.pageNumber = page
        this.setPostJump(postJump ?: "")
        setPostFiltering(null, null)
        this.lastPage = FIRST_PAGE
        updateUiElements()
        showBlankPage()
        if (activity != null) {
            loaderManager.destroyLoader(Constants.THREAD_INFO_LOADER_ID)
            loaderManager.destroyLoader(Constants.POST_LOADER_ID)
            refreshInfo()
            // TODO: shouldn't every load do a sync?
            if (fullSync) {
                syncThread()
            } else {
                refreshPosts()
            }
        }
    }

    private class AwfulStackEntry(val id: Int, val page: Int, val scrollPos: Int)

    private fun pushThread(id: Int, page: Int, postJump: String?) {
        if (mThreadView != null && this.threadId != 0) {
            backStack.addFirst(
                AwfulStackEntry(
                    this.threadId,
                    this.pageNumber,
                    mThreadView!!.scrollY
                )
            )
        }
        loadThread(id, page, postJump, true)
    }

    private fun popThread() {
        loadThread(backStack.removeFirst()!!)
    }

    private fun clearBackStack() {
        backStack.clear()
    }

    private fun backStackCount(): Int {
        return backStack.size
    }

    override fun onBackPressed(): Boolean {
        if (backStackCount() > 0) {
            popThread()
            return true
        } else if (postFilterUserId != null) {
            showAllPosts(null)
            return true
        } else {
            return false
        }
    }


    override fun doScroll(down: Boolean): Boolean {
        val threadView = mThreadView ?: return false

        if (down) {
            threadView.pageDown(false)
        } else {
            threadView.pageUp(false)
        }

        return true
    }


    private fun toggleScreenOn() {
        keepScreenOn = !keepScreenOn
        mThreadView?.keepScreenOn = keepScreenOn

        //TODO icon
        alertView.setTitle(if (keepScreenOn) "Screen stays on" else "Screen turns itself off")
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.AWFUL_PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (downloadLink != null) enqueueDownload(downloadLink!!)
                } else {
                    Toast.makeText(
                        activity,
                        R.string.no_file_permission_download,
                        Toast.LENGTH_LONG
                    ).show()
                }
                downloadLink = null
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
