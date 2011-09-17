package com.ferg.awful;

public interface AwfulUpdateCallback {
	/**
	 * This callback triggers when the data underlying this fragment has changed in some way.
	 * This does not guarantee that the data has completely loaded, this callback is also triggered by failed loads or when a service connects.
	 * @param pageChange Specifies if the current page number has changed since the last dataUpdate(), useful for resetting List position.
	 */
	public void dataUpdate(boolean pageChange);
	/**
	 * Called when the loading process for this view has failed.
	 * Keep in mind, the user may still have cached data.
	 */
	public void loadingFailed();
	/**
	 * Called when a background load for this page has begun.
	 */
	public void loadingStarted();
	/**
	 * Called when a loading process has succeeded for the current view.
	 * This does not supplement/replace dataUpdate(), it is only used for displaying loading status.
	 */
	public void loadingSucceeded();
	/**
	 * Called when the service connects but before any auto-loading or other service activities.
	 */
	public void onServiceConnected();
}
