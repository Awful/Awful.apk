package com.ferg.awful.constants;

public class Constants {
    public static final String BASE_URL = "http://forums.somethingawful.com/";

    public static final String FUNCTION_LOGIN  = BASE_URL + "account.php";
    public static final String FUNCTION_USERCP = BASE_URL + "usercp.php";
    public static final String FUNCTION_FORUM  = BASE_URL + "forumdisplay.php";
    public static final String FUNCTION_THREAD = BASE_URL + "showthread.php";

    public static final String PARAM_USERNAME  = "username";
    public static final String PARAM_PASSWORD  = "password";
    public static final String PARAM_ACTION    = "action";
    public static final String PARAM_THREAD_ID = "threadid";
    public static final String PARAM_PAGE      = "pagenumber";
    public static final String PARAM_FORUM_ID  = "forumid";

    // Intent parameters
    public static final String THREAD_ID = "thread_id";
    public static final String PAGE      = "page";

	public static final String PREFERENCES   = "prefs";
	public static final String PREF_USERNAME = "username";
	public static final String PREF_PASSWORD = "password";
}
