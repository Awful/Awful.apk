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

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.TextView
import com.android.volley.toolbox.NetworkImageView
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils.imageLoader
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.DatabaseHelper
import org.jsoup.nodes.Document
import java.sql.Timestamp
import java.util.regex.Pattern
import androidx.core.net.toUri

object AwfulEmote {
    const val TAG: String = "AwfulEmote"
    const val PATH: String = "/emote"
    @JvmField
    val CONTENT_URI: Uri = ("content://" + Constants.AUTHORITY + PATH).toUri()

    const val ID: String = "_id"
    const val TEXT: String = "text"
    const val SUBTEXT: String = "emote_subtext" //hover text
    const val URL: String = "url"
    const val INDEX: String = "emote_index"

    @JvmField
    var fileName_regex: Pattern = Pattern.compile("/([^/]+)$")

    fun getView(current: View, aPref: AwfulPreferences?, data: Cursor) {
        val emoteText = current.findViewById<View?>(R.id.emote_text) as TextView
        emoteText.text = data.getString(data.getColumnIndexOrThrow(TEXT))
        emoteText.setTextColor(current.resources.getColor(R.color.default_post_font))
        val emoteImage = current.findViewById<View?>(R.id.emote_icon) as NetworkImageView
        emoteImage.setImageUrl(data.getString(data.getColumnIndexOrThrow(URL)), imageLoader)
    }


    fun parseEmotes(data: Document): ArrayList<ContentValues?> {
        val update_time = Timestamp(System.currentTimeMillis()).toString()
        val results = ArrayList<ContentValues?>()
        var index = 1
        for (group in data.getElementsByClass("smilie_group")) {
            Log.e(TAG, "Parsing group.")
            for (smilie in group.getElementsByClass("smilie")) {
                Log.e(TAG, "Parsing item.")
                try {
                    val emote = ContentValues()
                    val text = smilie.getElementsByClass("text")
                    emote.put(ID, index++) //intentional post-increment
                    emote.put(TEXT, text.text().trim { it <= ' ' })
                    val img = smilie.getElementsByAttribute("src")
                    emote.put(SUBTEXT, img.attr("title"))
                    val url = img.attr("src")
                    emote.put(URL, url)
                    emote.put(INDEX, index)
                    //timestamp for DB trimming
                    emote.put(DatabaseHelper.UPDATED_TIMESTAMP, update_time)
                    results.add(emote)
                } catch (e: Exception) {
                    e.printStackTrace()
                    continue
                }
            }
        }
        return results
    }
}
