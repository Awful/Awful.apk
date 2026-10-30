/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
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

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.util.AwfulError
import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.nodes.Document
import java.util.Locale
import androidx.core.net.toUri

/**
 * SA Private Messages.
 * @author Geekner
 */
object AwfulMessage : AwfulPagedItem() {
    private const val TAG = "AwfulMessage"

    const val PATH: String = "/privatemessages"
    val CONTENT_URI: Uri = "content://${Constants.AUTHORITY}${PATH}".toUri()
    const val PATH_REPLY: String = "/draftreplies"
    val CONTENT_URI_REPLY: Uri = "content://${Constants.AUTHORITY}${PATH_REPLY}".toUri()
    const val PATH_THREAD: String = "/draftthreads"
    val CONTENT_URI_THREAD: Uri = "content://${Constants.AUTHORITY}${PATH_THREAD}".toUri()

    const val ID: String = "_id"
    const val TITLE: String = "title"
    const val AUTHOR: String = "author"
    const val CONTENT: String = "content"
    const val DATE: String = "message_date"
    const val EPOC_TIMESTAMP: String = "epoc_timestamp"
    const val TYPE: String = "message_type"
    const val ICON: String = "icon"
    const val UNREAD: String = "unread_message"
    const val RECIPIENT: String = "recipient"
    const val REPLY_CONTENT: String = "reply_content"
    const val REPLY_ICON: String = "reply_icon"
    const val REPLY_TITLE: String = "reply_title"
    const val REPLY_ATTACHMENT: String = "attachment"
    const val REPLY_ATTACHMENT_ACTION: String = "attachment_action"
    const val REPLY_SIGNATURE: String = "signature"
    const val REPLY_DISABLE_SMILIES: String = "disablesmilies"

    const val POST_CONTENT: String = "post_content"
    const val POST_SUBJECT: String = "post_subject"
    const val POST_ICON_ID: String = "post_icon_id"
    const val POST_ICON_URL: String = "post_icon_url"
    const val FOLDER: String = "folder"

    const val TYPE_PM: Int = 1
    const val TYPE_NEW_REPLY: Int = 2
    const val TYPE_QUOTE: Int = 3
    const val TYPE_EDIT: Int = 4

    /**
     * Generates List view items for PM list.
     */
    fun getView(current: View, aPref: AwfulPreferences?, data: Cursor, selected: Boolean): View {
        val title = current.findViewById<View?>(R.id.title) as TextView
        val t = data.getString(data.getColumnIndexOrThrow(TITLE))
        current.findViewById<View>(R.id.unread_count).visibility = View.GONE
        if (t != null) {
            title.text = t
            title.setTextColor(ColorProvider.PRIMARY_TEXT.color)
        }
        val author = current.findViewById<View?>(R.id.thread_info) as TextView
        val auth = data.getString(data.getColumnIndexOrThrow(AUTHOR))
        val date = data.getString(data.getColumnIndexOrThrow(DATE))
        if (auth != null && date != null) {
            author.text = "$auth - $date"
            author.setTextColor(ColorProvider.ALT_TEXT.color)
        }

        val unreadPM = current.findViewById<View?>(R.id.thread_tag) as ImageView
        val overlay = current.findViewById<View?>(R.id.thread_tag_overlay) as ImageView
        overlay.visibility = View.GONE

        unreadPM.visibility = View.VISIBLE
        val iconResource: Int = when (data.getInt(data.getColumnIndexOrThrow(UNREAD))) {
            0 -> R.drawable.ic_drafts_dark //unread
            1 -> R.drawable.ic_mail_dark //read
            2 -> R.drawable.ic_reply_dark //replied
            else -> R.drawable.ic_drafts_dark
        }
        val icon = data.getString(data.getColumnIndexOrThrow(ICON))
        if (icon != null && icon.isNotEmpty()) {
            val localFileName = "@drawable/" + icon.substring(icon.lastIndexOf('/') + 1, icon.lastIndexOf('.'))
                    .replace('-', '_').lowercase(Locale.getDefault())
            val imageID = current.resources.getIdentifier(localFileName, null, current.context.packageName)
            if (imageID == 0) {
                unreadPM.setImageResource(iconResource)
            } else {
                unreadPM.setImageResource(imageID)
                overlay.setImageResource(iconResource)
                overlay.setBackgroundResource(R.drawable.overlay_background)
                overlay.visibility = View.VISIBLE
            }
        } else {
            unreadPM.setImageResource(iconResource)
        }
        return current
    }

    @Throws(AwfulError::class)
    fun processMessageList(contentInterface: ContentResolver, data: Document, folder: Int) {
        val msgList = ArrayList<ContentValues?>()

        /**METHOD One: Parse PM links. Easy, but only contains id+title. */
        /*TagNode[] messagesParent = data.getElementsByAttValue("name", "form", true, true);
		if(messagesParent.length > 0){
			TagNode[] messages = messagesParent[0].getElementsByName("a", true);
			for(TagNode msg : messages){
				String href = msg.getAttributeByName("href");
				if(href != null){
					AwfulMessage pm = new AwfulMessage(Integer.parseInt(href.replaceAll("\\D", "")));
					pm.mTitle = msg.getText().toString();
					pm.mAuthor = "";
					msgList.add(pm);
				}
			}
		}else{
			Log.e("AwfulMessage","Failed to parse message parent");
			return null;//we'll use this to show that the load failed. i am still lazy.
		}*/
        /**METHOD Two: Parse table structure, hard and quick to break. */
        val messagesParent = data.getElementsByAttributeValue("name", "form")
        if (messagesParent.isNotEmpty()) {
            val messages = messagesParent.first()!!.getElementsByTag("tr")
            for (msg in messages) {
                if (msg === messages[0]) {
                    continue
                }
                val pm = ContentValues()
                //fuck i hate scraping shit.
                //no usable identifiers on the PM list, no easy method to find author/post date.
                //this will break if they change the display structure.
                val row = msg.getElementsByTag("td")
                if (row != null && row.size > 4) {
                    //TODO abandon hope, all ye who enter
                    //row[0] - icon, newpm.gif - sublevel
                    //row[1] - post icon TODO if we ever add icon support - sublevel
                    //row[2] - pm subject/link - sublevel
                    //row[3] - sender
                    //row[4] - date
                    val href = row[2].getElementsByTag("a").first()
                    pm.put(ID, href!!.attr("href").replace("\\D".toRegex(), "").toInt())
                    pm.put(TITLE, href.text())
                    val icon = row[1].getElementsByTag("img").first()
                    if (icon == null) {
                        pm.put(ICON, "")
                    } else {
                        pm.put(ICON, icon.attr("src"))
                    }
                    pm.put(AUTHOR, row[3].text())
                    pm.put(DATE, row[4].text())
                    pm.put(CONTENT, " ")
                    if (row.first()!!.getElementsByTag("img").first()!!.attr("src").endsWith("newpm.gif")) {
                        pm.put(UNREAD, 1)
                    } else if (row.first()!!.getElementsByTag("img").first()!!.attr("src").endsWith("pmreplied.gif")) {
                        pm.put(UNREAD, 2)
                    } else {
                        pm.put(UNREAD, 0)
                    }
                    pm.put(FOLDER, folder)
                    msgList.add(pm)
                }
            }
        } else {
            throw AwfulError("Failed to parse message parent")
        }
        contentInterface.bulkInsert(CONTENT_URI, msgList.toTypedArray<ContentValues?>())
    }

    @Throws(AwfulError::class)
    fun processMessage(data: Document, id: Int): ContentValues {
        val message = ContentValues()
        message.put(ID, id)
        val auth = data.getElementsByClass("author")
        if (auth.isNotEmpty()) {
            message.put(AUTHOR, auth.first()!!.text())
        } else {
            throw AwfulError("Failed parse: author.")
        }
        val content = data.getElementsByClass("postbody")
        if (content.isNotEmpty()) {
            message.put(CONTENT, content.first()!!.html())
        } else {
            throw AwfulError("Failed parse: content.")
        }
        val date = data.getElementsByClass("postdate")
        if (date.isNotEmpty()) {
            message.put(DATE, date.first()!!.text().replace("\"".toRegex(), "").trim { it <= ' ' })
        } else {
            throw AwfulError("Failed parse: date.")
        }
        return message
    }

    fun processReplyMessage(pmReplyData: Document, id: Int): ContentValues {
        val reply = ContentValues()
        reply.put(ID, id)
        reply.put(TYPE, TYPE_PM)
        val message = pmReplyData.getElementsByAttributeValue("name", "message")
        if (message.isNotEmpty()) {
            val quoteText = StringEscapeUtils.unescapeHtml4(
                message.first()!!.text().replace("[\\r\\f]".toRegex(), "")
            )
            reply.put(REPLY_CONTENT, quoteText)
        }
        val title = pmReplyData.getElementsByAttributeValue("name", "title")
        if (title.isNotEmpty()) {
            val quoteTitle = StringEscapeUtils.unescapeHtml4(title.first()!!.attr("value"))
            reply.put(TITLE, quoteTitle)
        }
        val recipient = pmReplyData.getElementsByAttributeValue("name", "touser")
        if (title.isNotEmpty()) {
            val recip = StringEscapeUtils.unescapeHtml4(recipient.first()?.attr("value"))
            reply.put(RECIPIENT, recip)
        }
        return reply
    }


    fun getMessageHtml(content: String?): String {
        return String.format(
            "<article class='post'><section class='postcontent'>%s</section></article>",
            content ?: ""
        )
    }
}
