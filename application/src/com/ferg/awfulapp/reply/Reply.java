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

package com.ferg.awfulapp.reply;

import java.util.HashMap;

import org.htmlcleaner.TagNode;

import android.content.ContentValues;
import android.graphics.Bitmap;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;

public class Reply {
    private static final String TAG = "Reply";

    private static final String FORMKEY    = "//input[@name='formkey']";
    private static final String FORMCOOKIE = "//input[@name='form_cookie']";
    private static final String QUOTE      = "//textarea[@name='message']";

    private static final String PARAM_ACTION      = "action";
    private static final String PARAM_THREADID    = "threadid";
    private static final String PARAM_POSTID      = "postid";
    private static final String PARAM_FORMKEY     = "formkey";
    private static final String PARAM_FORM_COOKIE = "form_cookie";
    private static final String PARAM_MESSAGE     = "message";
    private static final String PARAM_BOOKMARK    = "bookmark";
    private static final String PARAM_ATTACHMENT  = "attachment";
    
    private static final String VALUE_ACTION      = "postreply";
    private static final String VALUE_EDIT        = "updatepost";
    private static final String VALUE_POSTID      = "";
    private static final String VALUE_FORM_COOKIE = "formcookie";

    public static final TagNode edit(String aMessage, String aFormKey, String aFormCookie, String aThreadId, String aPostId, String aAttachment) 
        throws Exception 
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, VALUE_EDIT);
        params.put(PARAM_THREADID, aThreadId);
        params.put(PARAM_POSTID, aPostId);
        params.put(PARAM_FORMKEY, aFormKey);
        params.put(PARAM_FORM_COOKIE, aFormCookie);
        params.put(PARAM_MESSAGE, aMessage);
        params.put(PARAM_BOOKMARK, "yes");
        params.put(Constants.PARAM_PARSEURL, Constants.YES);
        if(aAttachment != null){
        	params.put(PARAM_ATTACHMENT, aAttachment);
        }

        return NetworkUtils.post(Constants.FUNCTION_EDIT_POST, params);
    }

    public static final TagNode post(String aMessage, String aFormKey, String aFormCookie, String aThreadId, String aAttachment) 
        throws Exception 
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, VALUE_ACTION);
        params.put(PARAM_THREADID, aThreadId);
        params.put(PARAM_POSTID, VALUE_POSTID);
        params.put(PARAM_FORMKEY, aFormKey);
        params.put(PARAM_FORM_COOKIE, aFormCookie);
        params.put(PARAM_MESSAGE, aMessage);
        params.put(Constants.PARAM_PARSEURL, Constants.YES);
        if(aAttachment != null){
        	params.put(PARAM_ATTACHMENT, aAttachment);
        }

        return NetworkUtils.post(Constants.FUNCTION_POST_REPLY, params);
    }

//    public static final TagNode post(String aMessage, String aFormKey, String aFormCookie, String aThreadId,
//            Bitmap aImage) throws Exception 
//    {
//        HashMap<String, String> params = new HashMap<String, String>();
//        params.put(PARAM_ACTION, VALUE_ACTION);
//        params.put(PARAM_THREADID, aThreadId);
//        params.put(PARAM_POSTID, VALUE_POSTID);
//        params.put(PARAM_FORMKEY, aFormKey);
//        params.put(PARAM_FORM_COOKIE, aFormCookie);
//        params.put(PARAM_MESSAGE, aMessage);
//        params.put(Constants.PARAM_PARSEURL, Constants.YES);
//
//        return NetworkUtils.post(Constants.FUNCTION_POST_REPLY, params);
//    }
    
    public static final ContentValues fetchPost(int threadId) throws Exception{
    	ContentValues newReply = new ContentValues();
    	newReply.put(AwfulMessage.ID, threadId);
    	newReply.put(AwfulMessage.TYPE, AwfulMessage.TYPE_NEW_REPLY);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, "newreply");
        params.put(PARAM_THREADID, Integer.toString(threadId));
        TagNode response = NetworkUtils.get(Constants.FUNCTION_POST_REPLY, params);
        getReplyData(response, newReply);
        
    	return newReply;
    }
    
    public static final ContentValues fetchQuote(int threadId, int postId) throws Exception{
    	ContentValues quote = new ContentValues();
    	quote.put(AwfulMessage.ID, threadId);
    	quote.put(AwfulMessage.TYPE, AwfulMessage.TYPE_QUOTE);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, "newreply");
        params.put(PARAM_POSTID, Integer.toString(postId));
        TagNode response = NetworkUtils.get(Constants.FUNCTION_POST_REPLY, params);
        getReplyData(response, quote);
        quote.put(AwfulMessage.REPLY_CONTENT, getMessageContent(response));
        quote.put(AwfulPost.REPLY_ORIGINAL_CONTENT, quote.getAsString(AwfulMessage.REPLY_CONTENT));
    	return quote;
    }
    
    public static final ContentValues fetchEdit(int threadId, int postId) throws Exception{
    	ContentValues edit = new ContentValues();
    	edit.put(AwfulMessage.ID, threadId);
    	edit.put(AwfulMessage.TYPE, AwfulMessage.TYPE_EDIT);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ACTION, "editpost");
        params.put(PARAM_POSTID, Integer.toString(postId));
        TagNode response = NetworkUtils.get(Constants.FUNCTION_EDIT_POST, params);
        edit.put(AwfulMessage.REPLY_CONTENT, getMessageContent(response));
        edit.put(AwfulPost.REPLY_ORIGINAL_CONTENT, edit.getAsString(AwfulMessage.REPLY_CONTENT));
        edit.put(AwfulPost.EDIT_POST_ID, postId);
    	return edit;
    }
    
    public static final String getMessageContent(TagNode data) throws Exception{
    	TagNode formContent = data.findElementByAttValue("name", "message", true, false);
        return formContent.getText().toString().trim();
    }

    public static final ContentValues getReplyData(TagNode data, ContentValues results) throws Exception {
    	TagNode formKey = data.findElementByAttValue("name", "formkey", true, false);
    	TagNode formCookie = data.findElementByAttValue("name", "form_cookie", true, false);
    	results.put(AwfulPost.FORM_KEY, formKey.getAttributeByName("value"));
    	results.put(AwfulPost.FORM_COOKIE, formCookie.getAttributeByName("value"));
        return results;
    }
}
