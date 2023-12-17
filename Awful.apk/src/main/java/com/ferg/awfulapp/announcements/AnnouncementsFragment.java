package com.ferg.awfulapp.announcements;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.AwfulFragment;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.databinding.AnnouncementsFragmentBinding;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulTheme;
import com.ferg.awfulapp.task.AnnouncementsRequest;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.thread.AwfulHtmlPage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.webview.AwfulWebView;
import com.ferg.awfulapp.webview.WebViewJsInterface;
import com.ferg.awfulapp.widget.StatusFrog;

import java.util.List;

import timber.log.Timber;

/**
 * Created by baka kaba on 05/02/2017.
 * <p>
 * Basic fragment that displays announcements.
 * <p>
 * This is basically a butchered thread view since that's kind of what the announcements page is.
 * Most of that happens in the request (not setting certain fields), here we just throw in some
 * meaningless constants in the {@link AwfulHtmlPage#getThreadHtml} call, and hope it doesn't break. Seems to work! Fix later!
 * <p>
 * Also this also assumes the announcements page won't ever have more than one page.
 * Whatever it's not even a thread
 */

public class AnnouncementsFragment extends AwfulFragment {

    AnnouncementsFragmentBinding binding;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AnnouncementsFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        initialiseWebView();
        return view;
    }

    private void initialiseWebView() {
        binding.announcementsWebview.setJavascriptHandler(new WebViewJsInterface() {

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
        binding.announcementsWebview.setWebViewClient(new WebViewClient() {
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
        binding.announcementsWebview.setContent(AwfulHtmlPage.getContainerHtml(getPrefs(), -1, false));
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
        binding.statusFrog.setStatusText(R.string.announcements_status_fetching).showSpinner(true);
        queueRequest(
                new AnnouncementsRequest(context).build(this, new AwfulRequest.AwfulResultCallback<List<AwfulPost>>() {
                    @Override
                    public void success(List<AwfulPost> result) {
                        AnnouncementsManager.getInstance().markAllRead();
                        // update the status frog if there are no announcements, otherwise hide it and display them
                        if (result.size() < 1) {
                            binding.statusFrog.setStatusText(R.string.announcements_status_none).showSpinner(false);
                        } else {
                            binding.announcementsWebview.setVisibility(View.VISIBLE);
                            // these page params don't mean anything in the context of the announcement page
                            // we just want it to a) display ok, and b) not let the user click anything bad
                            String bodyHtml = AwfulHtmlPage.getThreadHtml(result, AwfulPreferences.getInstance(), 1, 1);
                            if (binding.announcementsWebview != null) {
                                binding.announcementsWebview.setBodyHtml(bodyHtml);
                            }
                            binding.statusFrog.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void failure(VolleyError error) {
                        binding.statusFrog.setStatusText(R.string.announcements_status_failed).showSpinner(false);
                        Timber.w("Announcement get failed!\n" + error.getMessage());
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
        if (binding.announcementsWebview != null) {
            binding.announcementsWebview.onPause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (binding.announcementsWebview != null) {
            binding.announcementsWebview.onResume();
        }
        super.onResume();
    }

    @Override
    protected boolean doScroll(boolean down) {
        if (binding.announcementsWebview == null) {
            return false;
        } else if (down) {
            binding.announcementsWebview.pageDown(false);
        } else {
            binding.announcementsWebview.pageUp(false);
        }
        return true;
    }
}
