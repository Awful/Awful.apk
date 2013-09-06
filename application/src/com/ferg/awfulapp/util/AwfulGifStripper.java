package com.ferg.awfulapp.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.ferg.awfulapp.AwfulFragment;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.util.Log;

/**
 * Author: Matthew Shepard
 * Date: 4/13/13 - 1:56 PM
 */
public class AwfulGifStripper extends ByteArrayInputStream {
    private String TAG = "AwfulGif-";
    private String mUrl;
    private int done = 0;
    private ByteArrayInputStream realIS;

    public AwfulGifStripper(String url, AwfulFragment fragment){
    	super(new byte[0]);
        TAG = TAG + url;
        mUrl = url;
        Listener<Bitmap> success = new Listener<Bitmap>() {

			@Override
			public void onResponse(Bitmap response) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
				response.compress(CompressFormat.PNG, 100, baos);
				realIS = new ByteArrayInputStream(baos.toByteArray());
				done = 1;
			}
		};
		ErrorListener failure = new ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError error) {
				done = -1;
				Log.e(TAG, "loading "+mUrl+" failed");
			}
		};
		
        ImageRequest get = new ImageRequest(url, success, 0, 0, Config.RGB_565, failure);
        fragment.queueRequest(get, true);
    }
    
    @Override
    public int read(byte[] buffer) throws IOException {
    	while(done == 0){
    		
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				done = -1;
				break;
			}
    	}
    	if(done == -1){
    		return done;
    	}
        return realIS.read(buffer);

    }
}
