package com.ferg.awfulapp.announcements;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.AwfulFragment;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulTheme;
import com.ferg.awfulapp.task.AnnouncementsRequest;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.webview.AwfulWebView;
import com.ferg.awfulapp.webview.WebViewJsInterface;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by baka kaba on 05/02/2017.
 * <p>
 * Basic fragment that displays announcements.
 * <p>
 * This is basically a butchered thread view since that's kind of what the announcements page is.
 * Most of that happens in the request (not setting certain fields), here we just throw in some
 * meaningless constants in the {@link AwfulThread#getHtml(List, AwfulPreferences, int, int, int, boolean)}
 * call, and hope it doesn't break. Seems to work! Fix later!
 * <p>
 * Also this also assumes the announcements page won't ever have more than one page.
 * Whatever it's not even a thread
 */

public class AnnouncementsFragment extends AwfulFragment {

    @BindView(R.id.announcements_webview)
    AwfulWebView webView;

    // TODO: 27/01/2017 stick a frog in the background or something while loading
    private String bodyHtml = "";

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflateView(R.layout.announcements_fragment, container, inflater);
        ButterKnife.bind(this, view);

        initialiseWebView();
        return view;
    }

    private void initialiseWebView() {
        webView.setJavascriptHandler(new WebViewJsInterface() {
            // TODO: 27/01/2017 work out which of these we can drop, or maybe make special announcements HTML instead of reusing the thread one
            @JavascriptInterface
            public String getBodyHtml() {
                return bodyHtml;
            }

            @JavascriptInterface
            public String getCSS() {
                return AwfulTheme.forForum(null).getCssPath();
            }

            @JavascriptInterface
            public String getIgnorePostHtml(String id) {
                return null;
            }

            @JavascriptInterface
            public String getPostJump() {
                return "";
            }

            @JavascriptInterface
            public void loadIgnoredPost(final String ignorePost) {
            }

            @JavascriptInterface
            public void haltSwipe() {
            }

            @JavascriptInterface
            public void resumeSwipe() {
            }

        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (webView != null && !bodyHtml.isEmpty()) {
                    webView.refreshPageContents(true);
                }
            }

            // this lets links open back in the main activity if we handle them (e.g. 'look at this thread'),
            // and opens them in a browser or whatever if we don't (e.g. 'click here to buy a thing on the site')
            @Override
            public boolean shouldOverrideUrlLoading(WebView aView, String url) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                return true;
            }
        });
        webView.setContent(AwfulThread.getContainerHtml(mPrefs, -1));
    }


    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
        showAnnouncements();
    }

    /**
     * Fire off the network request to get and show the current announcements.
     * Not caching these in the DB! Maybe later! Probably not!!
     */
    private void showAnnouncements() {
        Context context = getContext().getApplicationContext();
        setProgress(25);
        queueRequest(
                new AnnouncementsRequest(context).build(this, new AwfulRequest.AwfulResultCallback<List<AwfulPost>>() {
                    @Override
                    public void success(List<AwfulPost> result) {
                        AnnouncementsManager.getInstance().markAllRead();
                        // these constants don't mean anything in the context of the announcement page
                        // we just want it to a) display ok, and b) not let the user click anything bad
                        bodyHtml = AwfulThread.getHtml(result, AwfulPreferences.getInstance(), 1, 1, 0, true);
                        if (webView != null) {
                            webView.refreshPageContents(true);
                        }
                    }

                    @Override
                    public void failure(VolleyError error) {
                        Log.w(TAG, "Announcement get failed!\n" + error.getMessage());
                        setProgress(100);
                    }
                })
        );
    }


    @Override
    public String getTitle() {
        return getString(R.string.announcements);
    }


    @Override
    public void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (webView != null) {
            webView.onResume();
        }
        super.onResume();
    }

    @Override
    protected boolean doScroll(boolean down) {
        if (webView == null) {
            return false;
        } else if (down) {
            webView.pageDown(false);
        } else {
            webView.pageUp(false);
        }
        return true;
    }
}
