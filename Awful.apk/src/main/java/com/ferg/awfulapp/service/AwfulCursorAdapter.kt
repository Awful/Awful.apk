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
package com.ferg.awfulapp.service

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cursoradapter.widget.CursorAdapter
import com.ferg.awfulapp.AwfulActivity
import com.ferg.awfulapp.AwfulFragment
import com.ferg.awfulapp.R
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.AwfulPreferences.Companion.getInstance
import com.ferg.awfulapp.thread.AwfulEmote
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulThread

class AwfulCursorAdapter(
    private val mParent: AwfulActivity,
    c: Cursor?,
    private var mId: Int,
    private val mFragment: AwfulFragment?
) : CursorAdapter(
    mParent, c, 0
) {
    private val mPrefs: AwfulPreferences = getInstance(mParent)
    private val inf: LayoutInflater = mParent.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    constructor(context: AwfulActivity, c: Cursor?, fragment: AwfulFragment?) : this(
        context,
        c,
        0,
        fragment
    )

    override fun bindView(current: View, context: Context?, data: Cursor) {
        if (data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0) { //unique to threads
            AwfulThread.setDataOnThreadListItem(current, mPrefs, data, mFragment)
        } else if (data.getColumnIndex(AwfulForum.PARENT_ID) >= 0) { //unique to forums
            assert(false)
        } else if (data.getColumnIndex(AwfulMessage.DATE) >= 0) {
            AwfulMessage.getView(current, mPrefs, data, false)
        } else if (data.getColumnIndex(AwfulEmote.INDEX) >= 0) {
            AwfulEmote.getView(current, mPrefs, data)
        }
        mParent.setPreferredFont(current)
    }

    override fun newView(context: Context?, data: Cursor, parent: ViewGroup?): View {
        val row: View
        if (data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0) { //unique to threads
            row = inf.inflate(R.layout.thread_item, parent, false)
            AwfulThread.setDataOnThreadListItem(row, mPrefs, data, mFragment)
        } else if (data.getColumnIndex(AwfulMessage.UNREAD) >= 0) {
            row = inf.inflate(R.layout.thread_item, parent, false)
            AwfulMessage.getView(row, mPrefs, data, false)
        } else if (data.getColumnIndex(AwfulEmote.INDEX) >= 0) {
            row = inf.inflate(R.layout.emote_grid_item, parent, false)
            AwfulEmote.getView(row, mPrefs, data)
        } else {
            row = inf.inflate(R.layout.loading, parent, false)
        }
        mParent.setPreferredFont(row)
        return row
    }

    fun getInt(id: Long, column: String?): Int {
        val tmpcursor = getRow(id)
        val col = tmpcursor?.getColumnIndex(column) ?: return 0
        return tmpcursor.getInt(col)
    }

    fun getString(id: Long, column: String?): String? {
        val tmpcursor = getRow(id)
        val col = tmpcursor?.getColumnIndex(column) ?: return null
        return tmpcursor.getString(col)
    }

    /**
     * Returns a cursor pointing to the row containing the specified ID or null if not found.
     * DO NOT CLOSE THE CURSOR.
     * @param id
     * @return cursor with specified row or null. DO NOT CLOSE.
     */
    fun getRow(id: Long): Cursor? {
        val tmpcursor = cursor
        if (tmpcursor != null && tmpcursor.moveToFirst()) {
            do {
                if (tmpcursor.getLong(tmpcursor.getColumnIndexOrThrow(AwfulThread.ID)) == id) { //contentprovider id tables are required to be _id
                    return UncloseableCursor(tmpcursor)
                }
            } while (tmpcursor.moveToNext())
        }
        return null
    }

    companion object {
        private const val TAG = "AwfulCursorAdapter"
    }
}
