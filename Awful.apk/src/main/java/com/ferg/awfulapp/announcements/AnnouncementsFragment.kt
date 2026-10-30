package com.ferg.awfulapp.announcements

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.volley.VolleyError
import com.ferg.awfulapp.AwfulFragment
import com.ferg.awfulapp.R
import com.ferg.awfulapp.databinding.AnnouncementsFragmentBinding
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.AwfulTheme
import com.ferg.awfulapp.task.AnnouncementsRequest
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.thread.AwfulHtmlPage
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.webview.WebViewJsInterface
import timber.log.Timber.Forest.w

/**
 * Created by baka kaba on 05/02/2017.
 * 
 * 
 * Basic fragment that displays announcements.
 * 
 * 
 * This is basically a butchered thread view since that's kind of what the announcements page is.
 * Most of that happens in the request (not setting certain fields), here we just throw in some
 * meaningless constants in the [AwfulHtmlPage.getThreadHtml] call, and hope it doesn't break. Seems to work! Fix later!
 * 
 * 
 * Also this also assumes the announcements page won't ever have more than one page.
 * Whatever it's not even a thread
 */
class AnnouncementsFragment : AwfulFragment() {
    lateinit var binding: AnnouncementsFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AnnouncementsFragmentBinding.inflate(inflater, container, false)
        val view: View = binding.getRoot()
        initialiseWebView()
        awfulActivity?.setPreferredFont(view)
        return view
    }

    private fun initialiseWebView() {
        binding.announcementsWebview.setJavascriptHandler(object : WebViewJsInterface() {
            @get:JavascriptInterface
            val cSS: String
                get() = AwfulTheme.forForum(null).cssPath

            @JavascriptInterface
            fun getIgnorePostHtml(id: String?): String? {
                return null
            }

            @get:JavascriptInterface
            val postJump: String
                get() = ""

            @JavascriptInterface
            fun loadIgnoredPost(ignorePost: String?) {
            }

            @JavascriptInterface
            fun haltSwipe() {
            }

            @JavascriptInterface
            fun resumeSwipe() {
            }
        })
        binding.announcementsWebview.webViewClient = object : WebViewClient() {
            // this lets links open back in the main activity if we handle them (e.g. 'look at this thread'),
            // and opens them in a browser or whatever if we don't (e.g. 'click here to buy a thing on the site')
            override fun shouldOverrideUrlLoading(aView: WebView?, url: String?): Boolean {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
                return true
            }
        }
        binding.announcementsWebview.setContent(AwfulHtmlPage.getContainerHtml(prefs, -1, false))
    }


    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        showAnnouncements()
    }

    /**
     * Fire off the network request to get and show the current announcements.
     * Not caching these in the DB! Maybe later! Probably not!!
     */
    private fun showAnnouncements() {
        val context = requireContext().applicationContext
        binding.statusFrog.setStatusText(R.string.announcements_status_fetching).showSpinner(true)
        queueRequest(
            AnnouncementsRequest(context).build(
                this,
                object : AwfulResultCallback<List<AwfulPost>> {
                    override fun success(result: List<AwfulPost>) {
                        AnnouncementsManager.getInstance().markAllRead()
                        // update the status frog if there are no announcements, otherwise hide it and display them
                        if (result.isEmpty()) {
                            binding.statusFrog.setStatusText(R.string.announcements_status_none)
                                .showSpinner(false)
                        } else {
                            binding.announcementsWebview.visibility = View.VISIBLE
                            // these page params don't mean anything in the context of the announcement page
                            // we just want it to a) display ok, and b) not let the user click anything bad
                            val bodyHtml = AwfulHtmlPage.getThreadHtml(
                                result.toMutableList(),
                                AwfulPreferences.getInstance(),
                                1,
                                1
                            )
                            binding.announcementsWebview.setBodyHtml(bodyHtml)
                            binding.statusFrog.visibility = View.INVISIBLE
                        }
                    }

                    override fun failure(error: VolleyError?) {
                        binding.statusFrog.setStatusText(R.string.announcements_status_failed)
                            .showSpinner(false)
                        w("Announcement get failed! %s", error?.message)
                    }
                })
        )
    }


    override fun getTitle(): String {
        return getString(R.string.announcements)
    }


    override fun onPause() {
        binding.announcementsWebview.onPause()
        super.onPause()
    }

    override fun onResume() {
        binding.announcementsWebview.onResume()
        super.onResume()
    }

    override fun doScroll(down: Boolean): Boolean {
        if (down) {
            binding.announcementsWebview.pageDown(false)
        } else {
            binding.announcementsWebview.pageUp(false)
        }
        return true
    }
}
