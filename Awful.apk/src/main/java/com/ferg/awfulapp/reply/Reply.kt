/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
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
 */
package com.ferg.awfulapp.reply

import android.content.ContentValues
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

object Reply {
    private const val TAG = "Reply"


    @Throws(AwfulError::class)
    fun processReply(page: Document, threadId: Int): ContentValues {
        val newReply = ContentValues()
        newReply.put(AwfulMessage.ID, threadId)
        newReply.put(AwfulMessage.TYPE, AwfulMessage.TYPE_NEW_REPLY)
        getReplyData(page, newReply)
        newReply.put(AwfulPost.FORM_BOOKMARK, getBookmarkOption(page))
        newReply.put(AwfulPost.FORM_SIGNATURE, getSignatureOption(page))
        return newReply
    }

    @Throws(AwfulError::class)
    fun processQuote(response: Document, threadId: Int, postId: Int): ContentValues {
        val quote = ContentValues()
        quote.put(AwfulMessage.ID, threadId)
        quote.put(AwfulMessage.TYPE, AwfulMessage.TYPE_QUOTE)
        getReplyData(response, quote)
        quote.put(AwfulPost.FORM_BOOKMARK, getBookmarkOption(response))
        quote.put(AwfulPost.FORM_SIGNATURE, getSignatureOption(response))
        quote.put(AwfulMessage.REPLY_CONTENT, getMessageContent(response))
        quote.put(AwfulPost.REPLY_ORIGINAL_CONTENT, quote.getAsString(AwfulMessage.REPLY_CONTENT))
        return quote
    }

    @Throws(AwfulError::class)
    fun processEdit(response: Document, threadId: Int, postId: Int): ContentValues {
        val edit = ContentValues()
        edit.put(AwfulMessage.ID, threadId)
        edit.put(AwfulMessage.TYPE, AwfulMessage.TYPE_EDIT)
        edit.put(AwfulMessage.REPLY_CONTENT, getMessageContent(response))
        edit.put(AwfulMessage.REPLY_ATTACHMENT, getAttachment(response))
        edit.put(AwfulPost.FORM_BOOKMARK, getBookmarkOption(response))
        edit.put(AwfulPost.FORM_SIGNATURE, getSignatureOption(response))
        edit.put(AwfulPost.FORM_DISABLE_SMILIES, getDisableEmotesOption(response))
        edit.put(AwfulPost.REPLY_ORIGINAL_CONTENT, edit.getAsString(AwfulMessage.REPLY_CONTENT))
        edit.put(AwfulPost.EDIT_POST_ID, postId)
        return edit
    }

    @Throws(AwfulError::class)
    fun getMessageContent(data: Document): String {
        try {
            val formContent = data.getElementsByAttributeValue("name", "message").first()
            return formContent!!.text().trim { it <= ' ' }
        } catch (e: Exception) {
            throw AwfulError("Failed to load quote")
        }
    }

    @Throws(AwfulError::class)
    fun getAttachment(data: Document): String? {
        try {
            val attachmentAction =
                data.getElementsByAttributeValue("name", "attachmentaction").first() ?: return null
            return attachmentAction.nextElementSibling()!!.text()
        } catch (e: Exception) {
            throw AwfulError("Failed to load quote")
        }
    }

    fun getBookmarkOption(data: Document): String {
        val formBookmark = data.getElementsByAttributeValue("name", "bookmark").first()
        return if (formBookmark!!.hasAttr("checked")) {
            "checked"
        } else {
            ""
        }
    }

    fun getDisableEmotesOption(data: Document): String {
        val formDisableEmotes =
            data.getElementsByAttributeValue("name", AwfulMessage.REPLY_DISABLE_SMILIES).first()
        return if (formDisableEmotes!!.hasAttr("checked")) {
            "checked"
        } else {
            ""
        }
    }

    fun getSignatureOption(data: Document): String {
        val formSignature =
            data.getElementsByAttributeValue("name", AwfulMessage.REPLY_SIGNATURE).first()
        return if (formSignature!!.hasAttr("checked")) {
            "checked"
        } else {
            ""
        }
    }

    @Throws(AwfulError::class)
    fun getReplyData(data: Document, results: ContentValues): ContentValues {
        try {
            val formKey = data.getElementsByAttributeValue("name", "formkey").first()
            val formCookie = data.getElementsByAttributeValue("name", "form_cookie").first()
            results.put(AwfulPost.FORM_KEY, formKey!!.`val`())
            results.put(AwfulPost.FORM_COOKIE, formCookie!!.`val`())
        } catch (e: Exception) {
            throw AwfulError("Failed to load reply")
        }
        return results
    }
}
