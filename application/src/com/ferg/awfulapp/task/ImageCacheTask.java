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
import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulEmote;

public class ImageCacheTask extends AwfulTask {
	private String mUrl;
	public ImageCacheTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_GRAB_IMAGE);
		mUrl = (String) aMsg.obj;
	}

	@Override
	protected String doInBackground(Void... params) {
		try{
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File baseCacheDir;
			baseCacheDir = mContext.getExternalCacheDir();
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
				return LOADING_FAILED;
			}
		}else{
			return LOADING_FAILED;
		}
		}catch(Exception e){
			e.printStackTrace();
			Log.e(TAG, "IMAGE CACHE FAILED: TOO EMOTIONAL");
			return LOADING_FAILED;
		}
		return null;
	}

}
