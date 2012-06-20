package com.ferg.awfulapp;

import android.os.Message;

import com.ferg.awfulapp.preferences.AwfulPreferences;

public interface AwfulUpdateCallback {
	/**
	 * Called when the loading process for this view has failed.
	 * Keep in mind, the user may still have cached data.
	 */
	public void loadingFailed(Message aMsg);
	/**
	 * Called when a background load for this page has begun.
	 */
	public void loadingStarted(Message aMsg);
	/**
	 * Called when a task sends a status update.
	 */
	public void loadingUpdate(Message aMsg);
	/**
	 * Called when a loading process has succeeded for the current view.
	 * This does not supplement/replace dataUpdate(), it is only used for displaying loading status.
	 */
	public void loadingSucceeded(Message aMsg);
	/**
	 * Called when any preference changes. Use this callback to update text/background color, font sizes, ect.
	 * @param mPrefs 
	 */
	public void onPreferenceChange(AwfulPreferences prefs);
}
