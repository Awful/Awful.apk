package com.ferg.awful.misc;

import android.view.KeyEvent;
import android.widget.ListView;

public class VolumeKeyHandler {
	/**
	 * Scrolls to the next/previous Item with the volume keys
	 * @param event : a KeyEvent. Only KEYCODE_VOLUME_UP and KEYCODE_VOLUME_DOWN are handled
	 * @param view : The view to scroll
	 * @return true if the event was handled, else false
	 */
	public static boolean handle(KeyEvent event, ListView view) {
		//TODO: Read preferences to enable/disable this		
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (view.getFirstVisiblePosition() != 0) {
					view.setSelection(view.getFirstVisiblePosition() - 1);
					view.clearFocus();
				}
			}
			return true;

		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (view.getFirstVisiblePosition() != view.getCount() - 1) {
					view.setSelection(view.getFirstVisiblePosition() + 1);
					view.clearFocus();
				}
			}
			return true;

		}
		return false;
	}

}
