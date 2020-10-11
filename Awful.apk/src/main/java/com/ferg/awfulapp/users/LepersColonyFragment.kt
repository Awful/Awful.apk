package com.ferg.awfulapp.users

import android.content.DialogInterface.BUTTON_POSITIVE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.volley.VolleyError
import com.ferg.awfulapp.AwfulFragment
import com.ferg.awfulapp.NavigationEvent
import com.ferg.awfulapp.NavigationEvent.Companion.parse
import com.ferg.awfulapp.R
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.popupmenu.PunishmentContextMenu
import com.ferg.awfulapp.provider.AwfulTheme
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.LepersColonyRequest
import com.ferg.awfulapp.thread.AwfulHtmlPage
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.util.bind
import com.ferg.awfulapp.util.tryGetInt
import com.ferg.awfulapp.webview.AwfulWebView
import com.ferg.awfulapp.webview.WebViewJsInterface
import com.ferg.awfulapp.widget.PageBar
import com.ferg.awfulapp.widget.PagePicker
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.BOTH
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.TOP
import com.samskivert.mustache.Mustache

/**
 * Created by baka kaba on 29/10/2018.
 *
 * AwfulFragment that handles displaying and navigating through the Leper's Colony
 * and user rap sheets.
 *
 * Basically a reworking of the thread view (since it needs to display similar HTML content) that
 * parses Leper's Colony pages and converts them into 'posts' called [Punishment]s. This data gets
 * displayed through the mustache template in [showData], and the WebView's JS handler takes care
 * of displaying the context menu for a punishment.
 *
 * Rap Sheets are treated as a special type of Leper's Colony page, if a [NavigationEvent.LepersColony.userId]
 * is provided it shows the pages for that user. In future it might be better to handle them as
 * separate fragments, e.g. for side-by-side tablet layout, or as a popup (say if you want to view
 * a user's rap sheet while reading a thread, as a modal dialog). In the latter case the pull-to-refresh
 * stuff would probably need to be disabled.
 *
 * State is held by storing the last [NavigationEvent.LepersColony], displaying another page
 * involves creating and navigating to a new NavigationEvent. This could be reworked to store a stack
 * of NavigationEvents, basically creating a backstack - for now the back button moves from Rap Sheet
 * to Leper's Colony, and then back home to the main activity
 */


class LepersColonyFragment : AwfulFragment(), SwipyRefreshLayout.OnRefreshListener {

    private val webView: AwfulWebView by bind(R.id.web_view)
    private val pageBar: PageBar by bind(R.id.page_bar)

    /**
     * We're handling state using navigation events, e.g. turning a page creates a new event to navigate.
     * This holds the last navigation event, i.e. the current state. Maybe later we can store a bunch as a backstack
     */
    private lateinit var navigationState: NavigationEvent.LepersColony
    /** the number of the last page in the Leper's Colony / current rap sheet, as seen on the last data load */
    private var lastPage = FIRST_PAGE

    companion object {
        /** the number of the first page in the Leper's Colony or a rap sheet */
        const val FIRST_PAGE = 1
        // state save/restore keys
        private const val KEY_USER_ID = "user ID"
        private const val KEY_CURRENT_PAGE = "current page"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflateView(R.layout.rap_sheet, container, inflater)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipyLayout = view.findViewById(R.id.swipe_refresh_layout)
        allowedSwipeRefreshDirections = BOTH
        swipyLayout?.apply {
            setColorSchemeResources(*ColorProvider.getSRLProgressColors(null))
            setProgressBackgroundColor(ColorProvider.getSRLBackgroundColor(null))
            isEnabled = !prefs.disablePullNext
            setOnRefreshListener(this@LepersColonyFragment)
        }

        pageBar.setListener(object : PageBar.PageBarCallbacks {
            override fun onPageNavigation(nextPage: Boolean) = turnPage(nextPage)
            override fun onRefreshClicked() = refreshPage()
            override fun onPageNumberClicked() = PagePicker(activity, lastPage, navigationState.page) { button, resultValue ->
                if (button == BUTTON_POSITIVE) navigationState.copy(page = resultValue).run(::navigate)
            }.show()
        })

        webView.initialise()
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        // rebuild state if we have any saved, otherwise try to parse the intent for an initial navigation event
        val navigationEvent = aSavedState?.run {
            NavigationEvent.LepersColony(userId = tryGetInt(KEY_USER_ID), page = tryGetInt(KEY_CURRENT_PAGE) ?: FIRST_PAGE)
        } ?: activity?.run { intent.parse() }

        navigationEvent?.run(::navigate)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putInt(KEY_CURRENT_PAGE, navigationState.page)
            navigationState.userId?.let { putInt(KEY_USER_ID, it) }
        }
        super.onSaveInstanceState(outState)
    }

    override fun handleNavigation(event: NavigationEvent): Boolean {
        if (event is NavigationEvent.LepersColony) {
            navigationState = event
            getTitle()?.run(this::setActionBarTitle)
            loadData()
            return true
        }
        return false
    }




    /** true if the current navigation state is showing a user's rap sheet */
    private val isRapSheet get() = navigationState.userId != null

    override fun getTitle(): String? = if (isRapSheet) "Rap Sheet" else "Leper's Colony"

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item?.itemId == android.R.id.home) {
            return true.also { goUp() }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() = true.also { goBack() }

    /** Handle back navigation - no page backstack, just moves up the hierarchy */
    private fun goBack() = goUp()

    /** Handle up navigation - just moves RapSheet -> LC -> Forums */
    private fun goUp() = navigate(if (isRapSheet) NavigationEvent.LepersColony() else NavigationEvent.MainActivity)


    override fun onRefresh(direction: SwipyRefreshLayoutDirection) =
            when {
                direction == TOP -> refreshPage()
                navigationState.page == lastPage -> refreshPage()
                else -> turnPage(true)
            }

    /**
     * Refresh the currently displayed page
     */
    private fun refreshPage() = loadData()

    /**
     * Load the next page, or the previous one if [toNext] is false.
     * Does nothing if this would move beyond the first or last page.
     */
    private fun turnPage(toNext: Boolean) {
        val newPage = navigationState.page + (if (toNext) 1 else -1)
        if (newPage in FIRST_PAGE..lastPage) {
            navigate(navigationState.copy(page = newPage))
        }
    }

    /**
     * Load and display data for the current [navigationState]
     */
    private fun loadData() {
        NetworkUtils.cancelRequests(LepersColonyRequest.REQUEST_TAG)
        activity?.let { context ->
            val request = LepersColonyRequest(context, navigationState.page, navigationState.userId?.toString())
                    .build(this, object : AwfulRequest.AwfulResultCallback<LepersColonyRequest.LepersColonyPage> {

                        override fun success(result: LepersColonyRequest.LepersColonyPage) {
                            result.run {
                                lastPage = totalPages
                                pageBar.updatePagePosition(navigationState.page, lastPage)
                                showData(punishments)
                            }
                        }

                        override fun failure(error: VolleyError?) {}
                    })
            queueRequest(request, cancelOnDestroy = false)
        }
    }


    /**
     * Format and display a set of [Punishment]s in the WebView
     */
    private fun showData(punishments: List<Punishment>) {
        activity?.run {
            val mustacheFile = assets.open("mustache/lepers_colony.mustache").reader()
            val template = Mustache.compiler().compile(mustacheFile)
            punishments.asSequence()
                    .map(template::execute)
                    .fold(StringBuilder(), StringBuilder::append)
                    .toString()
                    .run(webView::setBodyHtml)
        }
    }


    /**
     * Set up the web view, including the context menu call.
     */
    private fun AwfulWebView.initialise() {
        setJavascriptHandler(object : WebViewJsInterface() {

            @JavascriptInterface
            fun onMoreClick(username: String, userId: String, badPostUrl: String, adminUsername: String, adminId: String) {
                PunishmentContextMenu.newInstance(
                        User(userId.toInt(), username),
                        User(adminId.toInt(), adminUsername),
                        badPostUrl.takeIf(String::isNotEmpty), // treat empty urls as a missing (null) value, the context menu handles this case
                        isRapSheet
                ).show(fragmentManager!!, "Post Actions")
            }

            @JavascriptInterface
            fun getCss(): String = AwfulTheme.forForum(null).cssPath

            @JavascriptInterface
            fun getPostJump(): String = ""
        })
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(aView: WebView, url: String): Boolean {
                url.run(AwfulURL::parse).run(NavigationEvent::Url).run(::navigate)
                return true
            }
        }
        setContent(AwfulHtmlPage.getContainerHtml(prefs, null, false))
    }
}

