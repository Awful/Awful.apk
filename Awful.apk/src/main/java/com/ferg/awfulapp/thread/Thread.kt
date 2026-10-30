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
package com.ferg.awfulapp.thread

import android.content.ContentValues
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

object Thread {


    @Throws(AwfulError::class)
    fun processThread(page: Document, forumId: Int): ContentValues {
        val newThread = ContentValues()
        newThread.put(AwfulMessage.ID, forumId)
        getFormData(page, newThread)
        newThread.put(AwfulPost.FORM_BOOKMARK, getBookmarkOption(page))
        newThread.put(AwfulPost.FORM_SIGNATURE, getSignatureOption(page))
        newThread.put(AwfulPost.FORM_DISABLE_SMILIES, getDisableEmotesOption(page))
        return newThread
    }

    fun getBookmarkOption(data: Document): String {
        val formBookmark = data.getElementsByAttributeValue("name", "bookmark").first()
        return if (formBookmark?.hasAttr("checked") == true) {
            "checked"
        } else {
            ""
        }
    }

    fun getDisableEmotesOption(data: Document): String {
        val formDisableEmotes = data.getElementsByAttributeValue("name", AwfulMessage.REPLY_DISABLE_SMILIES).first()
        return if (formDisableEmotes?.hasAttr("checked") == true) {
            "checked"
        } else {
            ""
        }
    }

    fun getSignatureOption(data: Document): String {
        val formSignature = data.getElementsByAttributeValue("name", AwfulMessage.REPLY_SIGNATURE).first()
        return if (formSignature?.hasAttr("checked") == true) {
            "checked"
        } else {
            ""
        }
    }

    @Throws(AwfulError::class)
    fun getFormData(data: Document, results: ContentValues): ContentValues {
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
