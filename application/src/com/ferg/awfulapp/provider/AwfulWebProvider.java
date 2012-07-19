package com.ferg.awfulapp.provider;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.ferg.awfulapp.preferences.AwfulPreferences;

public class AwfulWebProvider extends ContentProvider {

	private static final String TAG = "AwfulWebProvider";
	
	private AwfulPreferences mPrefs;

	//Using this to serve fonts to webview, but eventually will use this to cache pictures on SD card (heh, maybe).
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.e(TAG,"delete STUB");
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		Log.e(TAG,"getType STUB");
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.e(TAG,"update STUB");
		return null;
	}

	@Override
	public boolean onCreate() {
		mPrefs = new AwfulPreferences(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Log.e(TAG,"query STUB");
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		Log.e(TAG,"update STUB");
		return 0;
	}

	@Override
	public AssetFileDescriptor openAssetFile(Uri uri, String mode)
			throws FileNotFoundException {
		try {
			return getContext().getAssets().openFd(mPrefs.preferredFont);//.mp3 as a shitty workaround for compressed asset issue
		} catch (IOException e) {
			e.printStackTrace();
			throw new FileNotFoundException();
		}
	}
}
