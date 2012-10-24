package com.ferg.awfulapp.thread;

import java.util.HashMap;

import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;


public class AwfulURL {
	public static enum TYPE{FORUM,THREAD,POST,EXTERNAL};
	private long id;
	private long pageNum = Constants.ITEMS_PER_PAGE;
	private int perPage;
	private String externalURL;
	private TYPE type;
	private String gotoParam;
	private String fragment;
	
	public static AwfulURL parse(String url){
		AwfulURL aurl = new AwfulURL();
		Uri uri = Uri.parse(url);
		if(uri.isRelative() || uri.getHost().contains("forums.somethingawful.com")){
			if(uri.getQueryParameter(Constants.PARAM_PAGE) != null){
				aurl.pageNum = Constants.safeParseLong(uri.getQueryParameter(Constants.PARAM_PAGE), 1);
			}
			if(uri.getQueryParameter(Constants.PARAM_PER_PAGE) != null){
				aurl.perPage = Constants.safeParseInt(uri.getQueryParameter(Constants.PARAM_PER_PAGE), Constants.ITEMS_PER_PAGE);
			}
			if(Constants.PATH_FORUM.contains(uri.getLastPathSegment())){
				aurl.type = TYPE.FORUM;
				if(uri.getQueryParameter(Constants.PARAM_FORUM_ID) != null){
					aurl.id = Constants.safeParseLong(uri.getQueryParameter(Constants.PARAM_FORUM_ID),1);
				}
			}else if(Constants.PATH_THREAD.contains(uri.getLastPathSegment())){
				aurl.type = TYPE.THREAD;
				if(uri.getQueryParameter(Constants.PARAM_THREAD_ID) != null){
					aurl.id = Constants.safeParseLong(uri.getQueryParameter(Constants.PARAM_THREAD_ID),0);
				}
				if(uri.getQueryParameter(Constants.PARAM_GOTO) != null){
					aurl.gotoParam = uri.getQueryParameter(Constants.PARAM_GOTO);
					if(Constants.VALUE_POST.equalsIgnoreCase(aurl.gotoParam)){
						aurl.type = TYPE.POST;
						aurl.id = Constants.safeParseLong(uri.getQueryParameter(Constants.PARAM_POST_ID),0);
					}
				}
				if(Constants.ACTION_SHOWPOST.equalsIgnoreCase(uri.getQueryParameter(Constants.PARAM_ACTION))){
					aurl.type = TYPE.POST;
					aurl.id = Constants.safeParseLong(uri.getQueryParameter(Constants.PARAM_POST_ID),0);
				}
			}
			aurl.fragment = uri.getFragment();
		}else{
			aurl.type = TYPE.EXTERNAL;
			aurl.externalURL = url;
		}
		return aurl;
	}
	
	/**
	 * Returns the URL, assuming the default 40 items per page.
	 * @return URL
	 */
	public String getURL(){
		return getURL(Constants.ITEMS_PER_PAGE);
	}
	
	public String getURL(int postPerPage){
		Uri.Builder url = null;
		switch(type){
		case FORUM:
			url = Uri.parse(Constants.FUNCTION_FORUM).buildUpon();
			url.appendQueryParameter(Constants.PARAM_FORUM_ID, Long.toString(id));
			url.appendQueryParameter(Constants.PARAM_PAGE, Long.toString(pageNum));
			break;
		case THREAD:
			url = Uri.parse(Constants.FUNCTION_THREAD).buildUpon();
			url.appendQueryParameter(Constants.PARAM_THREAD_ID, Long.toString(id));
			if(gotoParam != null){
				url.appendQueryParameter(Constants.PARAM_GOTO, gotoParam);//goto=newpost, ect
			}else{
				url.appendQueryParameter(Constants.PARAM_PAGE, Long.toString(convertPerPage(pageNum, perPage, postPerPage)));
				url.appendQueryParameter(Constants.PARAM_PER_PAGE, Integer.toString(postPerPage));
			}
			break;
		case POST:
			url = Uri.parse(Constants.FUNCTION_THREAD).buildUpon();
			url.appendQueryParameter(Constants.PARAM_GOTO, Constants.VALUE_POST);
			url.appendQueryParameter(Constants.PARAM_POST_ID, Long.toString(id));
			break;
		case EXTERNAL:
			return externalURL;
		}
		return (url == null? "" : url.toString());
	}
	
	public static long convertPerPage(long originalPageNum, long originalPerPage, long newPerPage){
		if(originalPerPage != newPerPage){
			return (long) Math.ceil((double)(originalPageNum*originalPerPage) / newPerPage);
		}else{
			return originalPageNum;
		}
	}

	public TYPE getType() {
		return type;
	}
	
	public long getId(){
		return id;
	}
	
	public long getPage(){
		return pageNum;
	}
	
	public long getPerPage(){
		return perPage;
	}
	
	public String getFragment(){
		return fragment;
	}

	public boolean isRedirect() {
		return gotoParam != null;
	}
}
