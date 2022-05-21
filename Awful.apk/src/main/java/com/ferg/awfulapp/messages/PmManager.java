package com.ferg.awfulapp.messages;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.task.ThreadListRequest;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by baka kaba on 16/08/2016.
 * <p/>
 * A class that takes care of Private Messages, keeping track of notified state and
 * informing listeners when there's a change.
 * <p/>
 * Pretty barebones right now, it could be expanded to manage all the PM access, do
 * filtering, run periodic background updates etc. Or whatever
 */
public class PmManager {

    private static final String TAG = "PmManager";

    /**
     * Gotta synchronise things since the html to parse is coming in on a network thread
     */
    private static Map<Listener, Object> callbacks = Collections.synchronizedMap(new WeakHashMap<Listener, Object>());

    @Nullable
    private volatile static String lastNotifiedPmUrl = null;

    /**
     * Parse the UCP html to find and extract the new PM notification, if there is one.
     *
     * @param page The UCP page's html
     */
    public static void parseUcpPage(Document page) {
        Elements senderElements = page.select(".private_messages td.sender");
        Elements hrefElements = page.select(".private_messages [href*='privatemessageid']");

        // get the first message details - the page potentially shows several
        String sender = (senderElements.isEmpty()) ? "" : senderElements.get(0).text();
        String href = (hrefElements.isEmpty()) ? "" : hrefElements.get(0).attr("abs:href");

        // just log an error if we couldn't get all the details for some reason
        if ("".equals(href) || "".equals(sender)) {
            String message = "Unable to correctly parse UCP for PMs!\nHref: {}, Sender: {}";
            Log.w(TAG, String.format(message, href, sender));
        } else if (!href.equals(lastNotifiedPmUrl)) {
            // otherwise let all the listeners know
            int numDisplayed = hrefElements.size();
            for (Listener listener : callbacks.keySet()) {
                listener.onNewPm(href, sender, numDisplayed);
            }
            lastNotifiedPmUrl = href;
        }
    }

    /**
     * Register a listener for private message updates.
     * <p/>
     * Only holds a weak reference, so keep your own reference if necessary.
     */
    public static void registerListener(@NonNull Listener listener) {
        callbacks.put(listener, new Object());
    }


    /**
     * Check the site to update the current PM status
     */
    public static void updatePms(@NonNull Context context) {
        // just need to load the user's bookmarks page to trigger a parse
        NetworkUtils.queueRequest(new ThreadListRequest(context, Constants.USERCP_ID, 1).build());
    }


    public interface Listener {
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
        void onNewPm(@NonNull String messageUrl, @NonNull String sender, int unreadCount);
    }

}
