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

package com.ferg.awfulapp.network;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CleanerTransformations;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagTransformation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.reply.Reply;
import com.ferg.awfulapp.service.AwfulSyncService;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    private static final String CHARSET = "windows-1252";
    
    private static final Pattern unencodeCharactersPattern = Pattern.compile("&#(\\d+);");
    private static final Pattern encodeCharactersPattern = Pattern.compile("([^\\x00-\\x7F])");

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
    	String sessionidCookieValue = prefs.getString(Constants.COOKIE_PREF_SESSIONID, null);
    	String sessionhashCookieValue = prefs.getString(Constants.COOKIE_PREF_SESSIONHASH, null);
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
    		
    		BasicClientCookie sessionidCookie = new BasicClientCookie(Constants.COOKIE_NAME_SESSIONID, sessionidCookieValue);
    		sessionidCookie.setDomain(Constants.COOKIE_DOMAIN);
    		sessionidCookie.setExpiryDate(expiryDate);
    		sessionidCookie.setPath(Constants.COOKIE_PATH);
    		
    		BasicClientCookie sessionhashCookie = new BasicClientCookie(Constants.COOKIE_NAME_SESSIONHASH, sessionhashCookieValue);
    		sessionhashCookie.setDomain(Constants.COOKIE_DOMAIN);
    		sessionhashCookie.setExpiryDate(expiryDate);
    		sessionhashCookie.setPath(Constants.COOKIE_PATH);
    		
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.edit().clear().apply();
        } else {
            prefs.edit().clear().commit();
        }
    	
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
    	String sessionId = null;
    	String sessionHash = null;
    	Date expires = null;
    	
    	List<Cookie> cookies = sHttpClient.getCookieStore().getCookies();
    	for(Cookie cookie : cookies) {
    		if(cookie.getDomain().contains(Constants.COOKIE_DOMAIN)) {
    			if(cookie.getName().equals(Constants.COOKIE_NAME_USERID)) {
    				useridValue = cookie.getValue();
    				expires = cookie.getExpiryDate();
    			} else if(cookie.getName().equals(Constants.COOKIE_NAME_PASSWORD)) {
    				passwordValue = cookie.getValue();
    				expires = cookie.getExpiryDate();
    			} else if(cookie.getName().equals(Constants.COOKIE_NAME_SESSIONID)) {
    				sessionId = cookie.getValue();
    				expires = cookie.getExpiryDate();
    			} else if(cookie.getName().equals(Constants.COOKIE_NAME_SESSIONHASH)) {
    				sessionHash = cookie.getValue();
    				expires = cookie.getExpiryDate();
    			}
    		}
    	}
    	
    	if(useridValue != null && passwordValue != null) {
    		Editor edit = prefs.edit();
    		edit.putString(Constants.COOKIE_PREF_USERID, useridValue);
    		edit.putString(Constants.COOKIE_PREF_PASSWORD, passwordValue);
    		edit.putString(Constants.COOKIE_PREF_SESSIONID, sessionId);
    		edit.putString(Constants.COOKIE_PREF_SESSIONHASH, sessionHash);
    		edit.putLong(Constants.COOKIE_PREF_EXPIRY_DATE, expires.getTime());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                edit.apply();
            } else {
                edit.commit();
            }

            return true;
    	}
    	
    	return false;
    }
    
    public static TagNode get(String aUrl) throws Exception {
        return get(aUrl, null);
    }

	public static TagNode get(String aUrl, HashMap<String, String> aParams) throws Exception {
		return get(aUrl,aParams,null,0);
	}
	
	public static Document getJSoup(String aUrl, HashMap<String, String> aParams, Messenger statusCallback, int midpointPercent) throws Exception {
		Document response = null;
        String parameters = getQueryStringParameters(aParams);
        URI location = new URI(aUrl + parameters);

        Log.i(TAG, "Fetching " + location);

        HttpGet httpGet;
        HttpResponse httpResponse;

        httpGet = new HttpGet(location);
        httpResponse = sHttpClient.execute(httpGet);

        HttpEntity entity = httpResponse.getEntity();

        if(statusCallback != null){
	        //notify user we have gotten message body
	        statusCallback.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, 0, midpointPercent));
        }
	    
        if (entity != null) {
            //response = sCleaner.clean(new InputStreamReader(entity.getContent(), CHARSET));
        	response = Jsoup.parse(entity.getContent(), CHARSET, Constants.BASE_URL);
        }
        
        Log.i(TAG, "Fetched " + location);
        return response;
	}
	
	public static TagNode get(String aUrl, HashMap<String, String> aParams, Messenger statusCallback, int midpointPercent) throws Exception {
        TagNode response = null;
        String parameters = getQueryStringParameters(aParams);
        URI location = new URI(aUrl + parameters);

        Log.i(TAG, "Fetching " + location);

        HttpGet httpGet;
        HttpResponse httpResponse;

        httpGet = new HttpGet(location);
        httpResponse = sHttpClient.execute(httpGet);

        HttpEntity entity = httpResponse.getEntity();

        if(statusCallback != null){
	        //notify user we have gotten message body
	        statusCallback.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, 0, midpointPercent));
        }
	    
        if (entity != null) {
            response = sCleaner.clean(new InputStreamReader(entity.getContent(), CHARSET));
        }
        
        Log.i(TAG, "Fetched " + location);
        return response;
    }
	
	public static String getRedirect(String aUrl, HashMap<String, String> aParams) throws Exception{
        String redirect = null;
        URI location;
        if(aParams != null){
	        location = new URI(aUrl + getQueryStringParameters(aParams));
        }else{
	        location = new URI(aUrl);
        }

        HttpGet httpGet = new HttpGet(location);
        HttpResponse httpResponse = sHttpClient.execute(httpGet);
        Header redirectLocation = httpResponse.getFirstHeader("Location");
        if(redirectLocation != null){
        	redirect = redirectLocation.getValue();
        }
		return redirect;
	}
	
	public static InputStream getStream(String aUrl) throws Exception{
		URI location = new URI(aUrl);

        Log.i(TAG, "Fetching " + location);

        HttpGet httpGet;
        HttpResponse httpResponse;
        httpGet = new HttpGet(location);
        httpResponse = sHttpClient.execute(httpGet);
        return httpResponse.getEntity().getContent();
	}

	public static TagNode post(String aUrl, HashMap<String, String> aParams) throws Exception {
        TagNode response = null;

		Log.i(TAG, aUrl);

        HttpPost httpPost = new HttpPost(aUrl);

        MultipartEntity post = new MultipartEntity();
        ArrayList<NameValuePair> paramdata = getPostParameters(aParams);
        for(NameValuePair data : paramdata ){
        	if(data.getName() == "attachment"){
        		post.addPart(data.getName(), new FileBody(new File(data.getValue())));
        	}else{
        		post.addPart(data.getName(), new StringBody(data.getValue()));
        	}
        }
        httpPost.setEntity(post);
        HttpResponse httpResponse = sHttpClient.execute(httpPost);

        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {
            response = sCleaner.clean(new InputStreamReader(entity.getContent(), CHARSET));
        }

		return response;
	}
	
	/**
	 * POSTs a message to the selected URL, but ignores any response body. Useful for quick-actions that we don't need the body content from.
	 * @param aUrl
	 * @param aParams
	 * @return HTTP Request status code (200 = success, ect)
	 * @throws Exception
	 */
	public static int postIgnoreBody(String aUrl, HashMap<String, String> aParams) throws Exception {

		Log.i(TAG, aUrl);
        HttpPost httpPost = new HttpPost(aUrl);
        httpPost.setEntity(
            new UrlEncodedFormEntity(getPostParameters(aParams)));  

        HttpResponse httpResponse = sHttpClient.execute(httpPost);
        return httpResponse.getStatusLine().getStatusCode();
	}

    public static TagNode postImage(String aUrl, HashMap<String, String> aParams, String aBitmapKey, 
            Bitmap aBitmap) throws Exception
	{
        TagNode response = null;

        HttpPost httpPost = new HttpPost(aUrl);

        MultipartEntity reqEntity = new MultipartEntity();

        if (aBitmap != null) {
            ByteArrayOutputStream bitmapOutputStream = new ByteArrayOutputStream();
            aBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmapOutputStream);

            ByteArrayBody bitmapBody = 
                new ByteArrayBody(bitmapOutputStream.toByteArray(), "image/jpg", "snippet.jpg");

            reqEntity.addPart(aBitmapKey, bitmapBody);
        }
		
		// Now write the form data 
		Iterator<?> iter = aParams.entrySet().iterator();

		while (iter.hasNext()) {
			@SuppressWarnings("unchecked")
			Map.Entry<String, String> param = (Map.Entry<String, String>) iter.next();

			reqEntity.addPart(param.getKey(), new StringBody(param.getValue()));
		}

		httpPost.setEntity(reqEntity);

        HttpResponse httpResponse = sHttpClient.execute(httpPost);
        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {
            response = sCleaner.clean(new InputStreamReader(entity.getContent(), CHARSET));
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
        	HttpParams httpPar = new BasicHttpParams();
        	HttpConnectionParams.setConnectionTimeout(httpPar, 10000);//10 second timeout when connecting. does not apply to data transfer
        	HttpConnectionParams.setSoTimeout(httpPar, 20000);//timeout to wait if no data transfer occurs //TODO bumped to 20, but we need to monitor cell status now
        	HttpConnectionParams.setSocketBufferSize(httpPar, 65548);
        	HttpClientParams.setRedirecting(httpPar, false);
            sHttpClient = new DefaultHttpClient(httpPar);
        }

        sCleaner = new HtmlCleaner();
        CleanerTransformations ct = new CleanerTransformations();
        ct.addTransformation(new TagTransformation("script"));
        ct.addTransformation(new TagTransformation("meta"));
        ct.addTransformation(new TagTransformation("head"));
        sCleaner.setTransformations(ct);
        CleanerProperties properties = sCleaner.getProperties();
        properties.setOmitComments(true);
        properties.setRecognizeUnicodeChars(false);
        properties.setUseEmptyElementTags(false);
    }

	public static void logCookies() {
		Log.i(TAG, "---BEGIN COOKIE DUMP---");
		List<Cookie> cookies = sHttpClient.getCookieStore().getCookies();
		for(Cookie c : cookies) {
			Log.i(TAG, c.toString());
		}
		Log.i(TAG, "---END COOKIE DUMP---");
	}

	public static String getAsString(TagNode pc) {
		return sCleaner.getInnerHtml(pc);
	}
	
	/**
	 * Parses all html-escaped characters to a regular Java string. Does not handle html tags.
	 * @param html
	 * @return unencoded text.
	 */
	public static String unencodeHtml(String html){
		if(html == null){
			return "";
		}
		String processed = StringEscapeUtils.unescapeHtml4(html);
		StringBuffer unencodedContent = new StringBuffer(processed.length());
		Matcher fixCharMatch = unencodeCharactersPattern.matcher(processed);
		while(fixCharMatch.find()){
			fixCharMatch.appendReplacement(unencodedContent, Character.toString((char) Integer.parseInt(fixCharMatch.group(1))));
			}
		fixCharMatch.appendTail(unencodedContent);
		return unencodedContent.toString();
	}
	
	/**
	 * Parses a Java string into html-escaped characters. Does not handle html tags.
	 * @param html
	 * @return unencoded text.
	 */
	public static String encodeHtml(String str){
		StringBuffer unencodedContent = new StringBuffer(str.length());
		Matcher fixCharMatch = encodeCharactersPattern.matcher(str);
		while(fixCharMatch.find()){
			fixCharMatch.appendReplacement(unencodedContent, "&#"+fixCharMatch.group(1).codePointAt(0)+";");
			}
		fixCharMatch.appendTail(unencodedContent);
		return unencodedContent.toString();
	}
}
