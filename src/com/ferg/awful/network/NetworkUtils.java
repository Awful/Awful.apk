package com.ferg.awful.network;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.ferg.awful.constants.Constants;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    private static DefaultHttpClient sHttpClient;
    private static HtmlCleaner sCleaner;

    public static TagNode get(String aUrl) throws Exception {
        return get(aUrl, null);
    }

	public static TagNode get(String aUrl, HashMap<String, String> aParams) throws Exception {
        TagNode response = null;
        String parameters = getQueryStringParameters(aParams);

        HttpGet httpGet = new HttpGet(aUrl + parameters);

        HttpResponse httpResponse = sHttpClient.execute(httpGet);

        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {
            response = sCleaner.clean(new InputStreamReader(entity.getContent()));
        }

		return response;
	}

	public static String post(String aUrl, HashMap<String, String> aParams) throws Exception {
        String response = null;

        HttpPost httpPost = new HttpPost(aUrl);
        httpPost.setEntity(
            new UrlEncodedFormEntity(getPostParameters(aParams)));  

        HttpResponse httpResponse = sHttpClient.execute(httpPost);

        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {
            response = EntityUtils.toString(entity);
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

    private static String getQueryStringParameters(HashMap<String, String> aParams) {
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
}
