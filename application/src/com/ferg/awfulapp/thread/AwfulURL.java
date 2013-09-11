package com.ferg.awfulapp.thread;

import android.net.Uri;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulUtils;


public class AwfulURL {
	
	public static enum TYPE{FORUM,THREAD,POST,EXTERNAL,NONE};
	private long id;
	private long pageNum = 1;
	private int perPage = Constants.ITEMS_PER_PAGE;
	private String externalURL;
	private TYPE type = TYPE.NONE;
	private String gotoParam;
	private String fragment;
	
	public static AwfulURL forum(long id){
		return forum(id, 1);
	}
	
	public static AwfulURL forum(long id, long pageNum){
		AwfulURL aurl = new AwfulURL();
		aurl.type = TYPE.FORUM;
		aurl.id = id;
		aurl.pageNum = pageNum;
		aurl.perPage = Constants.THREADS_PER_PAGE;
		return aurl;
	}
	
	public static AwfulURL thread(long id){
		return thread(id, 1, Constants.ITEMS_PER_PAGE, null);
	}
	
	public static AwfulURL threadUnread(long id){
		return thread(id, 1, Constants.ITEMS_PER_PAGE, null).setGoto(Constants.VALUE_NEWPOST);
	}
	
	public static AwfulURL threadUnread(long id, int perPage){
		return thread(id, 1, perPage, null).setGoto(Constants.VALUE_NEWPOST);
	}
	
	public static AwfulURL threadLastPage(long id){
		return thread(id, 1, Constants.ITEMS_PER_PAGE, null).setGoto(Constants.VALUE_LASTPOST);
	}
	
	public static AwfulURL threadLastPage(long id, int perPage){
		return thread(id, 1, perPage, null).setGoto(Constants.VALUE_LASTPOST);
	}
	
	public static AwfulURL thread(long id, long pageNum){
		return thread(id, pageNum, Constants.ITEMS_PER_PAGE, null);
	}
	
	public static AwfulURL thread(long id, long pageNum, int perPage){
		return thread(id, pageNum, perPage, null);
	}
	
	public static AwfulURL thread(long id, long pageNum, int perPage, String goTo){
		AwfulURL aurl = new AwfulURL();
		aurl.type = TYPE.THREAD;
		aurl.id = id;
		aurl.pageNum = pageNum;
		aurl.perPage = perPage;
		aurl.gotoParam = goTo;
		return aurl;
	}
	
	public static AwfulURL post(long id){
		return post(id, Constants.ITEMS_PER_PAGE);
	}
	
	public static AwfulURL post(long id, int perPage){
		AwfulURL aurl = new AwfulURL();
		aurl.type = TYPE.POST;
		aurl.id = id;
		aurl.gotoParam = Constants.VALUE_POST;
		return aurl;
	}
	
	public static AwfulURL parse(String url){
		AwfulURL aurl = new AwfulURL();
		Uri uri = Uri.parse(url);
		if(uri.isRelative() || (uri.getHost() != null && uri.getHost().contains("forums.somethingawful.com"))){
			if(uri.getQueryParameter(Constants.PARAM_PAGE) != null){
				aurl.pageNum = AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_PAGE), 1);
			}
			if(uri.getQueryParameter(Constants.PARAM_PER_PAGE) != null){
				aurl.perPage = AwfulUtils.safeParseInt(uri.getQueryParameter(Constants.PARAM_PER_PAGE), Constants.ITEMS_PER_PAGE);
			}
			if(Constants.PATH_FORUM.contains(uri.getLastPathSegment())){
				aurl.type = TYPE.FORUM;
				aurl.perPage = Constants.THREADS_PER_PAGE;
				if(uri.getQueryParameter(Constants.PARAM_FORUM_ID) != null){
					aurl.id = AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_FORUM_ID), 1);
				}
			}else if(Constants.PATH_BOOKMARKS.contains(uri.getLastPathSegment()) || Constants.PATH_USERCP.contains(uri.getLastPathSegment())){
				aurl.type = TYPE.FORUM;
				aurl.perPage = Constants.THREADS_PER_PAGE;
				aurl.id = Constants.USERCP_ID;
			}else if(Constants.PATH_THREAD.contains(uri.getLastPathSegment())){
				aurl.type = TYPE.THREAD;
				if(uri.getQueryParameter(Constants.PARAM_THREAD_ID) != null){
					aurl.id = AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_THREAD_ID), 0);
				}
				if(uri.getQueryParameter(Constants.PARAM_GOTO) != null){
					aurl.gotoParam = uri.getQueryParameter(Constants.PARAM_GOTO);
					if(Constants.VALUE_POST.equalsIgnoreCase(aurl.gotoParam)){
						aurl.type = TYPE.POST;
						aurl.id = AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_POST_ID), 0);
					}
				}
				if(Constants.ACTION_SHOWPOST.equalsIgnoreCase(uri.getQueryParameter(Constants.PARAM_ACTION))){
					aurl.type = TYPE.POST;
					aurl.id = AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_POST_ID), 0);
				}
			}else{
				aurl.type = TYPE.EXTERNAL;
				aurl.externalURL = url;
			}
			aurl.fragment = uri.getFragment();
		}else{
			aurl.type = TYPE.EXTERNAL;
			aurl.externalURL = url;
		}
		Log.e("AwfulURL","Parsed URL: "+aurl.getURL());
		return aurl;
	}
	
	/**
	 * Returns the URL, assuming the default 40 items per page.
	 * @return URL
	 */
	public String getURL(){
		return getURL(perPage);
	}
	
	public String getURL(int postPerPage){
		Uri.Builder url = null;
		switch(type){
		case FORUM:
			if(id == Constants.USERCP_ID){
				url = Uri.parse(Constants.FUNCTION_BOOKMARK).buildUpon();
			}else{
				url = Uri.parse(Constants.FUNCTION_FORUM).buildUpon();
			}
			url.appendQueryParameter(Constants.PARAM_FORUM_ID, Long.toString(id));
			url.appendQueryParameter(Constants.PARAM_PAGE, Long.toString(pageNum));
			break;
		case THREAD:
			url = Uri.parse(Constants.FUNCTION_THREAD).buildUpon();
			url.appendQueryParameter(Constants.PARAM_THREAD_ID, Long.toString(id));
			url.appendQueryParameter(Constants.PARAM_PER_PAGE, Integer.toString(postPerPage));
			if(gotoParam != null){
				url.appendQueryParameter(Constants.PARAM_GOTO, gotoParam);//goto=newpost, ect
			}else{
				url.appendQueryParameter(Constants.PARAM_PAGE, Long.toString(convertPerPage(pageNum, perPage, postPerPage)));
			}
			break;
		case POST:
			url = Uri.parse(Constants.FUNCTION_THREAD).buildUpon();
			url.appendQueryParameter(Constants.PARAM_GOTO, Constants.VALUE_POST);
			url.appendQueryParameter(Constants.PARAM_PER_PAGE, Integer.toString(postPerPage));
			url.appendQueryParameter(Constants.PARAM_POST_ID, Long.toString(id));
			break;
		case EXTERNAL:
			return externalURL;
		}
		return (url == null? "" : url.toString());
	}
	
	public static long convertPerPage(long originalPageNum, long originalPerPage, long newPerPage){
		long pageNum = originalPageNum;
		if(originalPerPage != newPerPage){
			pageNum = (long) Math.ceil((double)(originalPageNum*originalPerPage) / newPerPage);
		}
		return pageNum;
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
	
	public long getPage(int postPerPage){
		return convertPerPage(pageNum, perPage, postPerPage);
	}
	
	public long getPerPage(){
		return perPage;
	}
	
	public String getFragment(){
		return (fragment != null ? fragment : "");
	}

	public boolean isRedirect() {
		return gotoParam != null;
	}

	@Override
	public String toString() {
		return getURL();
	}
	
	public AwfulURL setGoto(String goTo){
		gotoParam = goTo;
		return this;
	}

	public boolean isForum() {
		return type == TYPE.FORUM;
	}

	public boolean isThread() {
		return type == TYPE.THREAD;
	}

	public boolean isPost() {
		return type == TYPE.POST;
	}

	public boolean isExternal() {
		return type == TYPE.EXTERNAL;
	}

	public AwfulURL setPerPage(int postPerPage) {
		perPage = postPerPage;
		return this;
	}
	
}
