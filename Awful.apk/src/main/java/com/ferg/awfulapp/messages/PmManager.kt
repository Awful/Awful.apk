package com.ferg.awfulapp.messages

import android.content.Context
import android.util.Log
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.task.ThreadListRequest
import org.jsoup.nodes.Document
import kotlin.concurrent.Volatile

/**
 * Created by baka kaba on 16/08/2016.
 * 
 * 
 * A class that takes care of Private Messages, keeping track of notified state and
 * informing listeners when there's a change.
 * 
 * 
 * Pretty barebones right now, it could be expanded to manage all the PM access, do
 * filtering, run periodic background updates etc. Or whatever
 */
object PmManager {
    private const val TAG = "PmManager"

    /**
     * Gotta synchronise things since the html to parse is coming in on a network thread
     */
    private val callbacks: MutableMap<Listener, Any?> = mutableMapOf()

    @Volatile
    private var lastNotifiedPmUrl: String? = null

    /**
     * Parse the UCP html to find and extract the new PM notification, if there is one.
     * 
     * @param page The UCP page's html
     */
    fun parseUcpPage(page: Document) {
        val senderElements = page.select(".private_messages td.sender")
        val hrefElements = page.select(".private_messages [href*='privatemessageid']")

        // get the first message details - the page potentially shows several
        val sender = if (senderElements.isEmpty()) "" else senderElements[0].text()
        val href = if (hrefElements.isEmpty()) "" else hrefElements[0].attr("abs:href")

        // just log an error if we couldn't get all the details for some reason
        if ("" == href || "" == sender) {
            val message = "Unable to correctly parse UCP for PMs!\nHref: {}, Sender: {}"
            Log.w(TAG, String.format(message, href, sender))
        } else if (href != lastNotifiedPmUrl) {
            // otherwise let all the listeners know
            val numDisplayed = hrefElements.size
            for (listener in callbacks.keys) {
                listener.onNewPm(href, sender, numDisplayed)
            }
            lastNotifiedPmUrl = href
        }
    }

    /**
     * Register a listener for private message updates.
     * 
     * 
     * Only holds a weak reference, so keep your own reference if necessary.
     */
    fun registerListener(listener: Listener) {
        callbacks[listener] = Any()
    }


    /**
     * Check the site to update the current PM status
     */
    @JvmStatic
    fun updatePms(context: Context) {
        // just need to load the user's bookmarks page to trigger a parse
        NetworkUtils.queueRequest(ThreadListRequest(context, Constants.USERCP_ID, 1).build())
    }


    interface Listener {
        /**
         * Called when the app first identifies an unread PM alert.
         * 
         * This will currently trigger when a 'new' PM is listed on the bookmarks page.
         * This can happen when the app is first opened, when a new PM arrives, or when
         * the top PM is read and another unread message takes the top spot.
         * @param messageUrl    The full URL to the message
         * @param sender        The name of the sender
         * @param unreadCount   The number of unread messages listed on the UCP page (may not be all?)
         */
        fun onNewPm(messageUrl: String, sender: String, unreadCount: Int)
    }
}
