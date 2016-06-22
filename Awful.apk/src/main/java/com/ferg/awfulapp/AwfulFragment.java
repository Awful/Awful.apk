/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
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
 *******************************************************************************/

package com.ferg.awfulapp;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.util.AwfulError;
import com.ferg.awfulapp.widget.AwfulProgressBar;
import com.ferg.awfulapp.widget.ProbationBar;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;

import static com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.BOTH;
import static com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.TOP;

public abstract class AwfulFragment extends Fragment implements AwfulRequest.ProgressListener, AwfulPreferences.AwfulPreferenceUpdate {
	protected String TAG = "AwfulFragment";
    protected static final boolean DEBUG = Constants.DEBUG;

	protected AwfulPreferences mPrefs;
	private int currentProgress = 100;
	private AwfulProgressBar mProgressBar;
	protected SwipyRefreshLayout mSRL;

    @Nullable
    private ProbationBar probationBar = null;

    protected final Handler mHandler = new Handler();

    @Override
    public void onAttach(Context context) {
    	super.onAttach(context);
        if(!(getActivity() instanceof AwfulActivity)){
    		throw new IllegalStateException("AwfulFragment - parent activity must extend AwfulActivity!");
    	}
    	if(mPrefs == null){
    		mPrefs = AwfulPreferences.getInstance(context, this);
    	}
    }

    protected View inflateView(int resId, ViewGroup container, LayoutInflater inflater){
    	View v = inflater.inflate(resId, container, false);
        View progressBar = v.findViewById(R.id.progress_bar);
        if(progressBar instanceof AwfulProgressBar){
            mProgressBar = (AwfulProgressBar) progressBar;
        }

        // set up the probation bar, if we have one - use this ID when adding to a layout!
        probationBar = (ProbationBar) v.findViewById(R.id.probation_bar);
        if (probationBar != null) {
            probationBar.setListener(new ProbationBar.Callbacks() {
                @Override
                public void onProbationButtonClicked() {
                    goToLeperColony();
                }
            });
        }
        return v;
    }

	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
		onPreferenceChange(mPrefs, null);
	}


    @Override
    public void onDestroy() {
    	super.onDestroy();
        this.cancelNetworkRequests();
        mHandler.removeCallbacksAndMessages(null);
        mPrefs.unregisterCallback(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.cancelNetworkRequests();
        mHandler.removeCallbacksAndMessages(null);
    }

    protected void displayForumIndex(){
    	if(getActivity() != null){
    		getAwfulActivity().displayForumIndex();
    	}
    }

    protected void displayForumContents(int aId) {
    	if(getActivity() != null){
    		getAwfulActivity().displayForum(aId, 1);
    	}
    }

    protected void displayThread(int aId, int aPage, int forumId, int forumPage, boolean forceReload) {
    	if(getActivity() != null){
    		getAwfulActivity().displayThread(aId, aPage, forumId, forumPage, forceReload);
    	}
    }


	public AwfulActivity getAwfulActivity(){
		return (AwfulActivity) getActivity();
	}

	protected void displayForum(long forumId, long page){
		if(getAwfulActivity() != null){
			getAwfulActivity().displayForum((int) forumId, (int) page);
		}
	}

    public void displayPostReplyDialog(final int threadId, final int postId, final int type) {
		if(getAwfulActivity() != null){
			getAwfulActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        startActivityForResult(
                                new Intent(getActivity(), PostReplyActivity.class)
                                        .putExtra(Constants.REPLY_THREAD_ID, threadId)
                                        .putExtra(Constants.EDITING, type)
                                        .putExtra(Constants.REPLY_POST_ID, postId),
                                PostReplyFragment.REQUEST_POST);
                    }
                }
            });
		}
    }

	protected void setProgress(int percent){
		currentProgress = percent;
		if(currentProgress > 0 && mSRL != null){
            mSRL.setRefreshing(false);
		}
		if(mProgressBar != null){
			mProgressBar.setProgress(percent, getActivity());
		}
	}

	public int getProgressPercent(){
		return currentProgress;
	}


    /**
     * Set the actionbar's title.
     * @param title The text to set as the title
     */
    @CallSuper
	protected void setTitle(@NonNull String title){
        // TODO: fix race condition in ForumDisplayFragment and ThreadDisplayFragment - both restart their loaders in onResume,
        // both of those set the actionbar title - even in phone mode where only one is visible. Whichever loads last sets the actionbar text
        AwfulActivity activity = getAwfulActivity();
		if (activity != null && activity.isFragmentVisible(this)) {
            Log.d(TAG, "setTitle: setting for " + this.getClass().getSimpleName());
            activity.setActionbarTitle(title, this);
		}
	}


    ///////////////////////////////////////////////////////////////////////////
    // Probation bar
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Refresh the probation bar's visual state
     */
    protected final void refreshProbationBar() {
        if (probationBar != null) {
            probationBar.setProbation(mPrefs.isOnProbation() ? mPrefs.probationTime : null);
        }
    }


    /**
     * Open the Leper Colony page - call this when the user clicks the probation button
     */
    private void goToLeperColony() {
        Intent openThread = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FUNCTION_BANLIST + '?' + Constants.PARAM_USER_ID + "=" + mPrefs.userId));
        startActivity(openThread);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Reacting to request progress
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void requestStarted(AwfulRequest req) {
        if(mSRL != null){
            // P2R Library is ... awful - part 1
            mSRL.setDirection(TOP);
            mSRL.setRefreshing(true);
        }
    }

    @Override
    public void requestUpdate(AwfulRequest req, int percent) {
        setProgress(percent);
    }

    @Override
    public void requestEnded(AwfulRequest req, VolleyError error) {
        if(mSRL != null){
            mSRL.setRefreshing(false);
            // P2R Library is ... awful - part 2
            mSRL.setDirection(this instanceof ThreadDisplayFragment ? BOTH : TOP);
        }
        if(error instanceof AwfulError){
            new AlertBuilder().fromError((AwfulError) error).show();
        }else if(error != null){
            new AlertBuilder().setTitle(R.string.loading_failed).show();
        }
    }

    @Override
	public void onPreferenceChange(AwfulPreferences prefs, String key) {
		if(mSRL != null){
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

            mSRL.setDistanceToTriggerSync(Math.round(prefs.p2rDistance*dpHeight));
		}
	}


    public boolean onBackPressed() {
		return false;
	}


    protected AwfulApplication getAwfulApplication(){
        AwfulActivity act = getAwfulActivity();
        if(act != null){
            return (AwfulApplication) act.getApplication();
        }
        return null;
    }
    public void queueRequest(Request request){
        queueRequest(request, false);
    }

    /**
     * Queue a network {@link Request}.
     * Set true to tag the request with the fragment, so it will be cancelled
     * when the fragment is destroyed. Set false if you want to retain the request's
     * default tag, e.g. so pending {@link com.ferg.awfulapp.task.PostRequest}s can
     * be cancelled when starting a new one.
     * @param request           A Volley request
     * @param cancelOnDestroy   Whether to tag with the fragment and automatically cancel
     */
    public void queueRequest(Request request, boolean cancelOnDestroy){
        if(request != null){
            if(cancelOnDestroy){
                request.setTag(this);
            }
            NetworkUtils.queueRequest(request);
        }
    }

    protected void cancelNetworkRequests(){
        NetworkUtils.cancelRequests(this);
    }



    protected void invalidateOptionsMenu() {
        AwfulActivity act = getAwfulActivity();
        if(act != null){
            act.supportInvalidateOptionsMenu();
        }
    }

    /** Get this fragment's display title */
    public abstract String getTitle();
    public void onPageVisible() {}
    public void onPageHidden() {}


    /**
     * Try to handle a KeyEvent as a volume scroll action.
     * @param event The event to handle
     * @return      true if the event was consumed
     */
    public final boolean attemptVolumeScroll(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                return doScroll(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN);
            } else {
                return true;
            }
        }
        return false;
    }


    /**
     * Perform a scroll action, e.g. in response to a volume scroll event.
     * </p>
     * Does nothing by default, override this and return true to handle it.
     * @param down  true to scroll down, false for up
     * @return      return true to consume this scroll event
     */
    protected boolean doScroll(boolean down) {
        return false;
    }


    /**
     * Builds and displays alert toasts
     */
    @SuppressWarnings("UnusedReturnValue")
    protected class AlertBuilder {
        @NonNull
        private String title = "";
        @NonNull
        private String subtitle = "";
        @DrawableRes
        private int iconResId = 0;
        @Nullable
        private Animation animation = null;

        @NonNull
        AlertBuilder setTitle(@StringRes int title) {
            this.title = getString(title);
            return this;
        }

        @NonNull
        AlertBuilder setTitle(@Nullable String title) {
            this.title = (title == null) ? "" : title;
            return this;
        }

        @NonNull
        AlertBuilder setSubtitle(@StringRes int subtitle) {
            this.subtitle = getString(subtitle);
            return this;
        }

        @NonNull
        AlertBuilder setSubtitle(@Nullable String subtitle) {
            this.subtitle = (subtitle == null) ? "" : subtitle;
            return this;
        }

        @NonNull
        AlertBuilder setIcon(@DrawableRes int iconResId) {
            this.iconResId = iconResId;
            return this;
        }

        @NonNull
        AlertBuilder setIconAnimation(@Nullable Animation animation) {
            this.animation = animation;
            return this;
        }

        @NonNull
        AlertBuilder fromError(@NonNull AwfulError error) {
            setTitle(error.getMessage());
            setSubtitle(error.getSubMessage());
            setIcon(error.getIconResource());
            setIconAnimation(error.getIconAnimation());
            return this;
        }

        void show() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayAlertInternal(title, subtitle, iconResId, animation);
                    }
                });
            }
        }
    }


    private void displayAlertInternal(@NonNull String title, @NonNull String subtext, int iconRes, @Nullable Animation animate){
        Activity activity = getActivity();
        if(activity == null){
            return;
        }
        LayoutInflater inflater = activity.getLayoutInflater();
        View popup = inflater.inflate(R.layout.alert_popup,
                (ViewGroup) activity.findViewById(R.id.alert_popup_root));
        TextView popupTitle = (TextView)popup.findViewById(R.id.popup_title);
        popupTitle.setText(title);
        TextView popupSubTitle = (TextView) popup.findViewById(R.id.popup_subtitle);
        if(TextUtils.isEmpty(subtext)){
            popupSubTitle.setVisibility(View.GONE);
        }else{
            popupSubTitle.setVisibility(View.VISIBLE);
            popupSubTitle.setText(subtext);
        }
        if(iconRes != 0){
            ImageView popupIcon = (ImageView) popup.findViewById(R.id.popup_icon);
            if(animate != null){
                popupIcon.setImageResource(iconRes);
                popupIcon.startAnimation(animate);
            }else{
                popupIcon.setImageResource(iconRes);
            }
        }
        Toast toast = new Toast(getAwfulApplication());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(popup);
        toast.show();
    }

    protected void restartLoader(int id, Bundle data, LoaderManager.LoaderCallbacks<? extends Object> callback) {
        if(getActivity() != null){
            getLoaderManager().restartLoader(id, data, callback);
        }
    }


    /**
     * Utility method to safely handle clipboard copying.
     * A <a href="https://code.google.com/p/android/issues/detail?id=58043">bug in 4.3</a>
     * means that clipboard writes can throw runtime exceptions if another app has registered
     * as a listener. This method catches them and displays an error message for the user.
     *
     * @param label             The {@link ClipData}'s label
     * @param clipText          The {@link ClipData}'s text
     * @param successMessageId  If supplied, a success message popup will be displayed
     * @return                  false if the copy failed
     */
    protected boolean safeCopyToClipboard(String label,
                                          String clipText,
                                          @Nullable @StringRes Integer successMessageId) {
        ClipboardManager clipboard = (ClipboardManager) this.getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, clipText);
        try {
            clipboard.setPrimaryClip(clip);
            if (successMessageId != null) {
                new AlertBuilder().setTitle(successMessageId)
                        .setIcon(R.drawable.ic_insert_link_dark)
                        .show();
            }
            return true;
        } catch (IllegalArgumentException | SecurityException e) {
            new AlertBuilder().setTitle("Unable to copy to clipboard!")
                    .setSubtitle("Another app has locked access, you may need to reboot")
                    .setIcon(R.drawable.ic_error)
                    .show();
            e.printStackTrace();
            return false;
        }
    }
}
