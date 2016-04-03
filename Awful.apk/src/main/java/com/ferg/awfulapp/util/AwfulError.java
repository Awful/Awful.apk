package com.ferg.awfulapp.util;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.Animation;
import android.webkit.CookieManager;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.AwfulLoginActivity;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AwfulError
 * This is an error class that encompasses all the predictable error states we will encounter from SA server responses.
 * This currently covers Logged out, some site down messages, and probation status.
 */
public class AwfulError extends VolleyError{
    private static final String TAG = "AwfulError";
    private int errorCode = 0;
    private String errorMessage = null;

    public AwfulError() {
        this(ERROR_GENERIC_FAILURE, null);
    }

    public AwfulError(int code) {
        this(code, null);
    }
    public AwfulError(String message){
        this(AwfulError.ERROR_GENERIC_FAILURE, message);
    }
    public AwfulError(int code, String message) {
        errorCode = code;
        errorMessage = message;
        Log.e(TAG, "Error: "+code+" - "+getMessage());
    }

    /**
     * If a custom message is registered with a code, it will be returned here.
     * If no custom message is provided, a generic message for that error type is provided.
     * @return A user-friendly error message.
     */
    @Override
    public String getMessage(){
        if(!TextUtils.isEmpty(errorMessage)){
            return errorMessage;
        }
        switch (errorCode){
            case ERROR_LOGGED_OUT:
                return "Error - Not Logged In";
            case ERROR_FORUM_CLOSED:
                return "Error - Forums Closed (Site Down)";
            case ERROR_PROBATION:
                return "You are under probation.";
            case ERROR_GENERIC_FAILURE:
                return "Failed to load!";
        }
        return null;
    }

    public int getErrorCode(){
        return errorCode;
    }

    /**
     * Quick check to see if this type of error is typically unrecoverable.
     * Short-cut for handleError() callback in AwfulRequest.
     * @return true if this error type is normally unrecoverable and we should skip processing the response.
     */
    public boolean isCritical(){
        return errorCode != ERROR_PROBATION;
    }

    /**
     * Checks a page for forum errors.
     * Detects forum closures, logged-out state, and banned/probate status.
     * Automatically used in AwfulRequest handling process, see AwfulRequest.handleError for more.
     * (Method moved from AwfulPagedItem)
     * @param page Full HTML page to check.
     * @param prefs An AwfulPreference object to reference or update preferences.
     * @return AwfulError object if an error is detected, null otherwise.
     */
    public static AwfulError checkPageErrors(Document page, AwfulPreferences prefs) {
        AwfulError error = null;

        if(null != page.getElementById("notregistered")){
            error = new AwfulError(ERROR_LOGGED_OUT);
            Log.e(TAG, "!!!Page says not registered - You are now LOGGED OUT");
            Log.w(TAG, "NetworkUtils Cookie Headers dump:");
            Map<String, String> headerMap = new HashMap<>();
            NetworkUtils.setCookieHeaders(headerMap);
            for (Map.Entry header : headerMap.entrySet()) {
                Log.w(TAG, "Header key: " + header.getKey() + " value: " + header.getValue());
            }

            Log.w(TAG, "HttpClient CookieStore dump:");
            NetworkUtils.logCookies();

            CookieManager ckiemonster = CookieManager.getInstance();
            String cookie = ckiemonster.getCookie(Constants.COOKIE_DOMAIN);
            Log.w(TAG, "WebView CookieManager cookie for COOKIE_DOMAIN:" + cookie);

            // TODO fix the actual problem, probably repeated network requests in a short space of time
            if (!NetworkUtils.dodgeLogoutBullet()) {
                NetworkUtils.clearLoginCookies(prefs.getContext());
                prefs.getContext().startActivity(new Intent().setClass(prefs.getContext(), AwfulLoginActivity.class));
                Log.e(TAG,"ERROR_LOGGED_OUT");
            }
        } else {
            NetworkUtils.resetDodges();
        }

        if(null != page.getElementById("closemsg")){
            String msg = page.getElementsByClass("reason").text().trim();
            if(msg.length() > 0){
                error = new AwfulError(ERROR_FORUM_CLOSED, "Forums Closed - "+msg);
            }else{
                error = new AwfulError(ERROR_FORUM_CLOSED);
            }
        }
        Element probation = page.getElementById("probation_warn");
        if(probation != null){
            error = new AwfulError(ERROR_PROBATION);
            Date probDate = null;
            try {
                Element userlink = probation.getElementsByTag("a").first();
                int userId = Integer.parseInt(userlink.attr("href").substring(userlink.attr("href").lastIndexOf("=")+1));
                prefs.setPreference(Keys.USER_ID, userId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Pattern p = Pattern.compile("(.*)until\\s(([\\s\\w:,])+).\\sYou(.*)");
                Matcher m = p.matcher(probation.text());
                m.find();
                String date = m.group(2);
                //for example January 11, 2013 10:35 AM CST
                SimpleDateFormat probationFormat = new SimpleDateFormat("MMMM d, yyyy hh:mm aa z", Locale.US);
                //TODO this might have timezone issues?
                probDate = probationFormat.parse(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(null != probDate){
                long probTimestamp = probDate.getTime();
                //FUCK PRE ICS
                try {
                    prefs.setPreference(Keys.PROBATION_TIME, probTimestamp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else{
            if(AwfulPreferences.getInstance().probationTime > 0L) {
                try {
                    prefs.setPreference(Keys.PROBATION_TIME, 0L);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return error;
    }

    public static final int ERROR_LOGGED_OUT = 0x00000001;
    public static final int ERROR_FORUM_CLOSED = 0x00000002;
    public static final int ERROR_PROBATION = 0x00000004;
    public static final int ERROR_GENERIC_FAILURE = 0x00000008;
    //public static final int ERROR_ = 0x00000010;

    public String getSubMessage() {
        switch (errorCode){
            case ERROR_GENERIC_FAILURE:
                return "Check your network connection and try again.";
        }
        return null;
    }

    public int getAlertTime() {
        return 3000;
    }

    public int getIconResource() {
        return R.drawable.ic_error;
    }

    public Animation getIconAnimation() {
        return null;
    }
}
