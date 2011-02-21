package com.ferg.awful;

import android.app.AlertDialog;
import android.app.Application;
import android.util.Log;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.ferg.awful.thumbnail.ThumbnailBus;
import com.ferg.awful.thumbnail.ThumbnailMessage;

/**
 * Allows application-wide access to the global image cache
 * 
 * @author brosmike
 */
public class AwfulApplication extends Application {
	private static String TAG="AwfulApplication";
	
	private ThumbnailBus mImageBus=new ThumbnailBus();
	private SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> mImageCache=
							new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(null, null, 101, mImageBus);
	
	ThumbnailBus getImageBus() {
		return(mImageBus);
	}
	
	SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> getImageCache() {
		return(mImageCache);
	}
}
