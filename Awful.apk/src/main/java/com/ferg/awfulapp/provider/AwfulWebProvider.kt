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
package com.ferg.awfulapp.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.ferg.awfulapp.preferences.AwfulPreferences
import java.io.FileNotFoundException
import java.io.IOException

class AwfulWebProvider : ContentProvider() {
    private var mPrefs: AwfulPreferences? = null

    //Using this to serve fonts to webview, but eventually will use this to cache pictures on SD card (heh, maybe).
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String?>?): Int {
        Log.e(TAG, "delete STUB")
        return 0
    }

    override fun getType(uri: Uri): String? {
        Log.e(TAG, "getType STUB")
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.e(TAG, "update STUB")
        return null
    }

    override fun onCreate(): Boolean {
        mPrefs = AwfulPreferences.getInstance(context!!)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String?>?, selection: String?,
        selectionArgs: Array<String?>?, sortOrder: String?
    ): Cursor? {
        Log.e(TAG, "query STUB")
        return null
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String?>?
    ): Int {
        Log.e(TAG, "update STUB")
        return 0
    }

    @Throws(FileNotFoundException::class)
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
        try {
            //.mp3 as a shitty workaround for compressed asset issue
            return context!!.assets.openFd(mPrefs?.preferredFont!!)
        } catch (e: IOException) {
            e.printStackTrace()
            throw FileNotFoundException()
        }
    }

    companion object {
        private const val TAG = "AwfulWebProvider"
    }
}
