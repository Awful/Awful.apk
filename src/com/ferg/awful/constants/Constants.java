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
    public static final String PARAM_GOTO      = "goto";

    // Intent parameters
    public static final String THREAD_ID = "thread_id";
    public static final String PAGE      = "page";

	public static final String PREFERENCES   = "prefs";
	public static final String PREF_USERNAME = "username";
	public static final String PREF_PASSWORD = "password";
}
