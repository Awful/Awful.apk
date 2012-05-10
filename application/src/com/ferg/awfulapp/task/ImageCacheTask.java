package com.ferg.awfulapp.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulEmote;

public class ImageCacheTask extends AwfulTask {
	private String mUrl;
	public ImageCacheTask(AwfulSyncService sync, int id, int hash, String url) {
		super(sync, id, hash, null, AwfulSyncService.MSG_GRAB_IMAGE);
		mUrl = url;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try{
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File baseCacheDir;
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO){
				baseCacheDir = new File(Environment.getExternalStorageDirectory(),"Android/data/com.ferg.awfulapp/cache/");
				Log.v(TAG,"CACHE DIR: "+baseCacheDir.toString());
			}else{
				baseCacheDir = mContext.getExternalCacheDir();
			}
			String folder = null;
			String fileName = null;
			String imgUrl = null;
			if(mUrl == null){
				ContentResolver resolv = mContext.getContentResolver();
				folder = "emotes/";
				Cursor data = resolv.query(ContentUris.withAppendedId(AwfulEmote.CONTENT_URI, mId), null, null, null, null);
				fileName = data.getString(data.getColumnIndex(AwfulEmote.CACHEFILE));
				imgUrl = data.getString(data.getColumnIndex(AwfulEmote.URL));
				data.close();
			}else{
				folder = "category/";
				Matcher fileNameMatcher = AwfulEmote.fileName_regex.matcher(mUrl);
				if(fileNameMatcher.find()){
					fileName = fileNameMatcher.group(1);
				}
				imgUrl = mUrl;
			}
			File cacheDir = new File(baseCacheDir,folder);
			if(!cacheDir.exists()){
				cacheDir.mkdirs();
			}
			InputStream is = NetworkUtils.getStream(imgUrl);
			Bitmap data = BitmapFactory.decodeStream(is);
			is.close();
			if(data != null){
				File image = new File(cacheDir,fileName);
				if(!image.exists()){
					FileOutputStream out = new FileOutputStream(image);
					if(data.compress(Bitmap.CompressFormat.PNG, 100, out)){
						Log.i(TAG,"IMAGE CACHED SUCCESSFULLY: "+fileName);
					}
					out.close();
				}
			}else{
				Log.e(TAG, "IMAGE DECODING FAILED: CATEGORICALLY FOOLISH");
				return false;
			}
		}else{
			return false;
		}
		}catch(Exception e){
			e.printStackTrace();
			Log.e(TAG, "IMAGE CACHE FAILED: TOO EMOTIONAL");
			return false;
		}
		return true;
	}

}
