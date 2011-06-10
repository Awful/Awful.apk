package com.ferg.awful;

public interface AwfulUpdateCallback {
	/**
	 * This callback triggers when the data underlying this fragment has changed in some way.
	 * This does not guarantee that the data has completely loaded, this callback is also triggered by failed loads or when a service connects.
	 * @param pageChange Specifies if the current page number has changed since the last dataUpdate(), useful for resetting List position.
	 */
	public void dataUpdate(boolean pageChange);
}
