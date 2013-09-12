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

import android.os.Looper;
import android.support.v4.app.LoaderManager;
import android.text.TextUtils;
import android.view.*;
import android.view.animation.Animation;
import android.widget.PopupWindow;
import com.android.volley.VolleyError;
import com.ferg.awfulapp.util.AwfulError;
import com.ferg.awfulapp.task.AwfulRequest;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.ImageLoader;
import com.androidquery.AQuery;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.widget.AwfulProgressBar;

public abstract class AwfulFragment extends Fragment implements ActionMode.Callback, AwfulRequest.ProgressListener, AwfulPreferences.AwfulPreferenceUpdate {
	protected String TAG = "AwfulFragment";
    protected static final boolean DEBUG = Constants.DEBUG;

	protected AwfulPreferences mPrefs;
	protected AQuery aq;
	protected int currentProgress = 100;
	private AwfulProgressBar mProgressBar;
	protected PullToRefreshAttacher mP2RAttacher;

    private PopupWindow popupAlert;
    private Runnable popupClose;
	

    protected Handler mHandler = new Handler();

    @Override
    public void onAttach(Activity aActivity) {
    	super.onAttach(aActivity); if(DEBUG) Log.e(TAG, "onAttach");
    	if(!(aActivity instanceof AwfulActivity)){
    		Log.e("AwfulFragment","PARENT ACTIVITY NOT EXTENDING AwfulActivity!");
    	}
        mPrefs = AwfulPreferences.getInstance(getAwfulActivity(), this);
    }
    
    protected View inflateView(int resId, ViewGroup container, LayoutInflater inflater){
    	View v = inflater.inflate(resId, container, false);
    	View progressBar = v.findViewById(R.id.progress_bar);
    	if(progressBar instanceof AwfulProgressBar){
    		mProgressBar = (AwfulProgressBar) progressBar;
    	}
    	aq = new AQuery(v);
    	return v;
    }
    
	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState); if(DEBUG) Log.e(TAG, "onActivityCreated");
		onPreferenceChange(mPrefs);
		if(mProgressBar != null){
			mProgressBar.setBackgroundColor(ColorProvider.getBackgroundColor());
		}
	}

    @Override
    public void onStart() {
        super.onStart(); if(DEBUG) Log.e(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume(); if(DEBUG) Log.e(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause(); if(DEBUG) Log.e(TAG, "onPause");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState); if(DEBUG) Log.e(TAG,"onSaveInstanceState");
    }

    @Override
    public void onStop() {
        super.onStop(); if(DEBUG) Log.e(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
    	super.onDestroy(); if(DEBUG) Log.e(TAG, "onDestroy");
        popupAlert = null;
        mPrefs.unregisterCallback(this);
    }

    @Override
    public void onDetach() {
        super.onDetach(); if(DEBUG) Log.e(TAG, "onDetach");
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
    
    protected void displayThread(int aId, int aPage, int forumId, int forumPage) {
    	if(getActivity() != null){
    		getAwfulActivity().displayThread(aId, aPage, forumId, forumPage);
    	}
    }
	
	
	public AwfulActivity getAwfulActivity(){
		return (AwfulActivity) getActivity();
	}
	
	protected void displayForum(long forumId, long page){
		if(getAwfulActivity() != null){
			getAwfulActivity().displayForum((int)forumId, (int)page);
		}
	}
	
    public void displayPostReplyDialog(final int threadId, final int postId, final int type) {
		if(getAwfulActivity() != null){
			getAwfulActivity().runOnUiThread(new Runnable(){
				@Override
				public void run() {
			    	getAwfulActivity().displayReplyWindow(threadId, postId, type);
				}
			});
		}
    }
	
	protected void setProgress(int percent){
		currentProgress = percent;
		AwfulActivity aa = getAwfulActivity();
		if(mProgressBar != null){
			mProgressBar.setProgress(percent);
            if(aa != null){
                aa.hideProgressBar();
            }
		}
	}
	
	public int getProgressPercent(){
		return currentProgress;
	}
	
	protected void setTitle(String title){
		if(getActivity()!=null){
			getAwfulActivity().setActionbarTitle(title, this);
		}
	}
	
	protected boolean isFragmentVisible(){
		if(getActivity()!=null){
			return getAwfulActivity().isFragmentVisible(this);
		}
		return false;
	}
	
	protected void startActionMode(){
		if(getAwfulActivity() != null){
			getAwfulActivity().startSupportActionMode(this);
		}
	}

    @Override
    public void requestStarted(AwfulRequest req) {
        AwfulActivity aa = getAwfulActivity();
        if(aa != null){
            aa.setSupportProgressBarVisibility(false);
            aa.setSupportProgressBarIndeterminateVisibility(true);
        }
    }

    @Override
    public void requestUpdate(AwfulRequest req, int percent) {
        setProgress(percent);
    }

    @Override
    public void requestEnded(AwfulRequest req, VolleyError error) {
        AwfulActivity aa = getAwfulActivity();
        if(aa != null){
            aa.setSupportProgressBarIndeterminateVisibility(false);
            aa.setSupportProgressBarVisibility(false);
        }
        if(mP2RAttacher != null){
            mP2RAttacher.setRefreshComplete();
        }
        if(error instanceof AwfulError){
            displayAlert((AwfulError) error);
        }else if(error != null){
            displayAlert(R.string.loading_failed);
        }
    }

    @Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		
	}

	protected boolean isLoggedIn(){
		return getAwfulActivity().isLoggedIn();
	}
	
	public boolean onBackPressed() {
		return false;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {	}

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
    public void queueRequest(Request request, boolean cancelOnDestroy){
        AwfulApplication app = getAwfulApplication();
        if(app != null && request != null){
            if(cancelOnDestroy){
                request.setTag(this);
            }
            app.queueRequest(request);
        }
    }

    protected void cancelNetworkRequests(){
        AwfulApplication app = getAwfulApplication();
        if(app != null){
            app.cancelRequests(this);
        }
    }

    public ImageLoader getImageLoader(){
        if(getAwfulApplication() != null){
            return getAwfulApplication().getImageLoader();
        }
        return null;
    }



    protected void invalidateOptionsMenu() {
        AwfulActivity act = getAwfulActivity();
        if(act != null){
            act.supportInvalidateOptionsMenu();
        }
    }

    public abstract String getTitle();
    public abstract void onPageVisible();
    public abstract void onPageHidden();
    public abstract String getInternalId();
    public abstract boolean volumeScroll(KeyEvent event);


    private static final int ALERT_DISPLAY_MILLIS = 3000;
    protected void displayAlert(int titleRes){
        if(getActivity() != null){
            displayAlert(getString(titleRes), null, ALERT_DISPLAY_MILLIS, 0, null);
        }
    }

    protected void displayAlert(int titleRes, int subtitleRes, int iconRes){
        if(getActivity() != null){
            if(subtitleRes != 0){
                displayAlert(getString(titleRes), getString(subtitleRes), ALERT_DISPLAY_MILLIS, iconRes, null);
            }else{
                displayAlert(getString(titleRes), null, ALERT_DISPLAY_MILLIS, iconRes, null);
            }
        }
    }

    protected void displayAlert(AwfulError error){
        displayAlert(error.getMessage(), error.getSubMessage(), error.getAlertTime(), error.getIconResource(), error.getIconAnimation());
    }

    protected void displayAlert(String title){
        displayAlert(title, null, ALERT_DISPLAY_MILLIS, 0, null);
    }

    protected void displayAlert(String title, int iconRes){
        displayAlert(title, null, ALERT_DISPLAY_MILLIS, iconRes, null);
    }

    protected void displayAlert(String title, String subtext){
        displayAlert(title, subtext, ALERT_DISPLAY_MILLIS, 0, null);
    }

    private void displayAlert(final String title, final String subtext, final int timeoutMillis, final int iconRes, final Animation animate){
        if(Looper.getMainLooper().equals(Looper.myLooper())){
            displayAlertInternal(title, subtext, timeoutMillis, iconRes, animate);
        }else{
            //post on main thread, if this is called from a secondary thread.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    displayAlertInternal(title, subtext, timeoutMillis, iconRes, animate);
                }
            });
        }
    }

    private void displayAlertInternal(String title, String subtext, int timeoutMillis, int iconRes, Animation animate){
        if(getActivity() == null){
            return;
        }
        if(popupAlert != null){
            if(popupClose != null){
                mHandler.removeCallbacks(popupClose);
                popupClose = null;
            }
            popupAlert.dismiss();
            popupAlert = null;
        }
        View popup = LayoutInflater.from(getActivity()).inflate(R.layout.alert_popup, null);
        AQuery aq = new AQuery(popup);
        aq.find(R.id.popup_title).text(title);
        if(TextUtils.isEmpty(subtext)){
            aq.find(R.id.popup_subtitle).gone();
        }else{
            aq.find(R.id.popup_subtitle).visible().text(subtext);
        }
        if(iconRes != 0){
            if(animate != null){
                aq.find(R.id.popup_icon).image(iconRes).animate(animate);
            }else{
                aq.find(R.id.popup_icon).image(iconRes);
            }
        }
        int popupDimen = (int) getResources().getDimension(R.dimen.popup_size);
        popupAlert = new PopupWindow(popup, popupDimen, popupDimen);
        popupAlert.setBackgroundDrawable(null);
        popupAlert.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                popupAlert = null;
                if(popupClose != null){
                    mHandler.removeCallbacks(popupClose);
                    popupClose = null;
                }
            }
        });
        popupAlert.showAtLocation(getView(), Gravity.CENTER, 0, 0);
        if(timeoutMillis > 0){
            popupClose = new Runnable() {
                @Override
                public void run() {
                    //TODO fade out
                    if(popupAlert != null){
                        popupAlert.dismiss();
                        popupAlert = null;
                        popupClose = null;
                    }
                }
            };
            mHandler.postDelayed(popupClose, timeoutMillis);
        }
    }

    protected void restartLoader(int id, Bundle data, LoaderManager.LoaderCallbacks<? extends Object> callback) {
        if(getActivity() != null){
            getLoaderManager().restartLoader(id, data, callback);
        }
    }
}
