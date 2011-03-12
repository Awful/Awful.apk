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

package com.ferg.awful.reply;

import android.util.Log;

import java.util.HashMap;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;

public class Reply {
    private static final String TAG = "Reply";

    private static final String FORMKEY = "//input[@name='formkey']";
    private static final String QUOTE   = "//textarea[@name='message']";

    private static final String PARAM_ACTION      = "action";
    private static final String PARAM_THREADID    = "threadid";
    private static final String PARAM_POSTID      = "postid";
    private static final String PARAM_FORMKEY     = "formkey";
    private static final String PARAM_FORM_COOKIE = "form_cookie";
    private static final String PARAM_MESSAGE     = "message";
    // TODO: Do we need the checkbox options here?
    
    private static final String VALUE_ACTION      = "postreply";
    private static final String VALUE_EDIT        = "updatepost";
    private static final String VALUE_POSTID      = "";
    private static final String VALUE_FORM_COOKIE = "formcookie";

    public static final TagNode edit(String aMessage, String aFormKey, String aThreadId, String aPostId) 
        throws Exception 
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, VALUE_EDIT);
        params.put(PARAM_THREADID, aThreadId);
        params.put(PARAM_POSTID, aPostId);
        params.put(PARAM_FORMKEY, aFormKey);
        params.put(PARAM_FORM_COOKIE, VALUE_FORM_COOKIE);
        params.put(PARAM_MESSAGE, aMessage);

        return NetworkUtils.post(Constants.FUNCTION_EDIT_POST, params);
    }

    public static final TagNode post(String aMessage, String aFormKey, String aThreadId) 
        throws Exception 
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, VALUE_ACTION);
        params.put(PARAM_THREADID, aThreadId);
        params.put(PARAM_POSTID, VALUE_POSTID);
        params.put(PARAM_FORMKEY, aFormKey);
        params.put(PARAM_FORM_COOKIE, VALUE_FORM_COOKIE);
        params.put(PARAM_MESSAGE, aMessage);

        return NetworkUtils.post(Constants.FUNCTION_POST_REPLY, params);
    }

    public static final String getFormKey(String aThreadId) throws Exception {
        String result = null;

        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, "newreply");
        params.put(PARAM_THREADID, aThreadId);

        TagNode response = NetworkUtils.get(Constants.FUNCTION_POST_REPLY, params);

        Object[] formkey = response.evaluateXPath(FORMKEY);
        if (formkey.length > 0) {
            result = ((TagNode) formkey[0]).getAttributeByName("value");
        }

        return result;
    }

    public static final String getPost(String aPostId) throws Exception {
        String result = null;

        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, "editpost");
        params.put(PARAM_POSTID, aPostId);

        TagNode response = NetworkUtils.get(Constants.FUNCTION_EDIT_POST, params);

        Object[] formkey = response.evaluateXPath(QUOTE);
        if (formkey.length > 0) {
            result = ((TagNode) formkey[0]).getText().toString();
        }

        return result;
    }

    public static final String getQuote(String aPostId) throws Exception {
        String result = null;

        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, "newreply");
        params.put(PARAM_POSTID, aPostId);

        TagNode response = NetworkUtils.get(Constants.FUNCTION_POST_REPLY, params);

        Object[] formkey = response.evaluateXPath(QUOTE);
        if (formkey.length > 0) {
            result = ((TagNode) formkey[0]).getText().toString();
        }

        return result;
    }
}
