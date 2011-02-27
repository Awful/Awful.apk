/***
	Copyright (c) 2008-2009 CommonsWare, LLC
	
	Licensed under the Apache License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may obtain
	a copy of the License at
		http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/		

package com.ferg.awful.thumbnail;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.commonsware.cwac.adapter.AdapterWrapper;
import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.ferg.awful.R;

public class ThumbnailAdapter extends AdapterWrapper {
	private static final String TAG="ThumbnailAdapter";
	private int[] imageIds;
	private SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> cache=null;
	private Activity host=null;
	
	/**
		* Constructor wrapping a supplied ListAdapter
    */
	public ThumbnailAdapter(Activity host,
													ListAdapter wrapped,
													SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> cache,
													int[] imageIds) {
		super(wrapped);
		
		this.host=host;
		this.imageIds=imageIds;
		this.cache=cache;
		
		cache.getBus().register(getBusKey(), onCache);
	}
	
	public void close() {
		cache.getBus().unregister(onCache);
	}

	/**
		* Get a View that displays the data at the specified
		* position in the data set. In this case, if we are at
		* the end of the list and we are still in append mode,
		* we ask for a pending view and return it, plus kick
		* off the background task to append more data to the
		* wrapped adapter.
		* @param position Position of the item whose data we want
		* @param convertView View to recycle, if not null
		* @param parent ViewGroup containing the returned View
    */
	@Override
	public View getView(int position, View convertView,
											ViewGroup parent) {
		View result=super.getView(position, convertView, parent);
		
		processView(result);
		
		return(result);
	}
	
	private static final RotateAnimation mLoadingAnimation = 
		new RotateAnimation(
				0f, 360f,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
	static {
		mLoadingAnimation.setInterpolator(new LinearInterpolator());
		mLoadingAnimation.setRepeatCount(Animation.INFINITE);
		mLoadingAnimation.setDuration(700);
	}
	private void setLoadingImage(ImageView image) {
		image.setImageResource(android.R.drawable.ic_menu_rotate);
		image.startAnimation(mLoadingAnimation);
	}
	private void unsetLoadingImage(ImageView image) {
		image.setAnimation(null);
	}
	
	public void processView(View row) {
		for (int imageId : imageIds) {
			ImageView image=(ImageView)row.findViewById(imageId);
			
			if (image == null) continue;
			
			if (image.getTag() == null) {
				// Immediately set the imageview blank, no asynchronous action
				image.setImageResource(0);
			} else {
				// We need to asynchronously manage the image.
				
				// First we synchronously set a "loading" animation
				setLoadingImage(image);
				
				// Tell the cache to load the image and set it
				ThumbnailMessage msg=cache.getBus().createMessage(getBusKey());
																
				msg.setImageView(image);
				msg.setUrl(image.getTag().toString());
				
				try {
					cache.notify(msg.getUrl(), msg);
				}
				catch (Throwable t) {
					Log.e(TAG, "Exception trying to fetch image", t);
				}
			}
		}
	}
	
	private String getBusKey() {
		return(toString());
	}
	
	private ThumbnailBus.Receiver<ThumbnailMessage> onCache=
		new ThumbnailBus.Receiver<ThumbnailMessage>() {
		public void onReceive(final ThumbnailMessage message) {
			final ImageView image=message.getImageView();
			
			host.runOnUiThread(new Runnable() {
				public void run() {
					if (image.getTag()!=null &&
							image.getTag().toString().equals(message.getUrl())) {
						unsetLoadingImage(image); // end progress spinner
						image.setImageDrawable(cache.get(message.getUrl()));
					}
				}
			});
		}
	};
}