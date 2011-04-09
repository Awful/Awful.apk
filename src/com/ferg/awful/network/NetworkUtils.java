/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
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

package com.ferg.awful.network;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.ferg.awful.constants.Constants;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    private static DefaultHttpClient sHttpClient;
    private static HtmlCleaner sCleaner;

    /**
     * Attempts to initialize the HttpClient with cookie values
     * stored in the given Context's SharedPreferences through the
     * {@link #saveLoginCookies(Context)} method.
     * 
     * @return Whether stored cookie values were found & initialized
     */
    public static boolean restoreLoginCookies(Context ctx) {
    	SharedPreferences prefs = ctx.getSharedPreferences(
    			Constants.COOKIE_PREFERENCE, 
    			Context.MODE_PRIVATE);
    	String useridCookieValue   = prefs.getString(Constants.COOKIE_PREF_USERID,   null);
    	String passwordCookieValue = prefs.getString(Constants.COOKIE_PREF_PASSWORD, null);
    	long expiry                = prefs.getLong  (Constants.COOKIE_PREF_EXPIRY_DATE, -1);
    	
    	if(useridCookieValue != null && passwordCookieValue != null && expiry != -1) {
    		Date expiryDate = new Date(expiry);
    		
    		BasicClientCookie useridCookie = new BasicClientCookie(Constants.COOKIE_NAME_USERID, useridCookieValue);
    		useridCookie.setDomain(Constants.COOKIE_DOMAIN);
    		useridCookie.setExpiryDate(expiryDate);
    		useridCookie.setPath(Constants.COOKIE_PATH);
    		
    		BasicClientCookie passwordCookie = new BasicClientCookie(Constants.COOKIE_NAME_PASSWORD, passwordCookieValue);
    		passwordCookie.setDomain(Constants.COOKIE_DOMAIN);
    		passwordCookie.setExpiryDate(expiryDate);
    		passwordCookie.setPath(Constants.COOKIE_PATH);
    		
    		CookieStore jar = new BasicCookieStore();
    		jar.addCookie(useridCookie);
    		jar.addCookie(passwordCookie);
    		sHttpClient.setCookieStore(jar);
    		
    		return true;
    	}
    	
    	return false;
    }
    
    /**
     * Clears cookies from both the current client's store and
     * the persistent SharedPreferences. Effectively, logs out.
     */
    public static void clearLoginCookies(Context ctx) {
    	// First clear out the persistent preferences...
    	SharedPreferences prefs = ctx.getSharedPreferences(
    			Constants.COOKIE_PREFERENCE, 
    			Context.MODE_PRIVATE);
    	prefs.edit().clear().commit();
    	
    	// Then the memory store
    	sHttpClient.getCookieStore().clear();
    }
    
    /**
     * Saves SomethingAwful login cookies that the client has received
     * during this session to the given Context's SharedPreferences. They
     * can be later restored with {@link #restoreLoginCookies(Context)}.
     * 
     * @return Whether any login cookies were successfully saved
     */
    public static boolean saveLoginCookies(Context ctx) {
    	SharedPreferences prefs = ctx.getSharedPreferences(
    			Constants.COOKIE_PREFERENCE,
    			Context.MODE_PRIVATE);
    	
    	String useridValue = null;
    	String passwordValue = null;
    	Date expires = null;
    	
    	List<Cookie> cookies = sHttpClient.getCookieStore().getCookies();
    	for(Cookie cookie : cookies) {
    		if(cookie.getDomain().equals(Constants.COOKIE_DOMAIN)) {
    			if(cookie.getName().equals(Constants.COOKIE_NAME_USERID)) {
    				useridValue = cookie.getValue();
    				expires = cookie.getExpiryDate();
    			} else if(cookie.getName().equals(Constants.COOKIE_NAME_PASSWORD)) {
    				passwordValue = cookie.getValue();
    				expires = cookie.getExpiryDate();
    			}
    		}
    	}
    	
    	if(useridValue != null && passwordValue != null) {
    		Editor edit = prefs.edit();
    		edit.putString(Constants.COOKIE_PREF_USERID, useridValue);
    		edit.putString(Constants.COOKIE_PREF_PASSWORD, passwordValue);
    		edit.putLong(Constants.COOKIE_PREF_EXPIRY_DATE, expires.getTime());
    		return edit.commit();
    	}
    	
    	return false;
    }
    
    public static TagNode get(String aUrl) throws Exception {
        return get(aUrl, null);
    }

	public static TagNode get(String aUrl, HashMap<String, String> aParams) throws Exception {
        TagNode response = null;
        String parameters = getQueryStringParameters(aParams);

		Log.i(TAG, "Fetching "+ aUrl + parameters);

        HttpGet httpGet = new HttpGet(aUrl + parameters);

        HttpResponse httpResponse = sHttpClient.execute(httpGet);

        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {
            response = sCleaner.clean(new InputStreamReader(entity.getContent()));
        }
        
        Log.i(TAG, "Fetched "+ aUrl + parameters);
        return response;
	}

	public static TagNode post(String aUrl, HashMap<String, String> aParams) throws Exception {
        TagNode response = null;

		Log.i(TAG, aUrl);

        HttpPost httpPost = new HttpPost(aUrl);
        httpPost.setEntity(
            new UrlEncodedFormEntity(getPostParameters(aParams)));  

        HttpResponse httpResponse = sHttpClient.execute(httpPost);

        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {
            response = sCleaner.clean(new InputStreamReader(entity.getContent()));
        }

		return response;
	}

    private static ArrayList<NameValuePair> getPostParameters(HashMap<String, String> aParams) {
        // Append parameters
        ArrayList<NameValuePair> result = new ArrayList<NameValuePair>();  

        if (aParams != null) {
            Iterator<?> iter = aParams.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<String, String> param = (Map.Entry<String, String>) iter.next();

                result.add(new BasicNameValuePair(param.getKey(), param.getValue()));  
            }
        }

        return result;
    }

    public static String getQueryStringParameters(HashMap<String, String> aParams) {
        StringBuffer result = new StringBuffer("?");

        if (aParams != null) {
            try {
                // Loop over each parameter and add it to the query string
                Iterator<?> iter = aParams.entrySet().iterator();

                while (iter.hasNext()) {
                    @SuppressWarnings("unchecked")
                        Map.Entry<String, String> param = (Map.Entry<String, String>) iter.next();

                    result.append(param.getKey() + "=" + URLEncoder.encode((String) param.getValue(), "UTF-8"));

                    if (iter.hasNext()) {
                        result.append("&");
                    }
                }
            } catch (UnsupportedEncodingException e) {
                Log.i(TAG, e.toString());
            }
        } else {
            return "";
        }
		
        return result.toString();
    }
    
    static {
        if (sHttpClient == null) {
            sHttpClient = new DefaultHttpClient(); 
        }

        sCleaner = new HtmlCleaner();
        CleanerProperties properties = sCleaner.getProperties();
        properties.setOmitComments(true);
    }

	public static void logCookies() {
		Log.i(TAG, "---BEGIN COOKIE DUMP---");
		List<Cookie> cookies = sHttpClient.getCookieStore().getCookies();
		for(Cookie c : cookies) {
			Log.i(TAG, c.toString());
		}
		Log.i(TAG, "---END COOKIE DUMP---");
	}
}
