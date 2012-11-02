/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
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

package com.ferg.awfulapp.thread;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;

public class AwfulEmote {
	public static final String TAG = "AwfulEmote";
    public static final String PATH     = "/emote";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

	public static final String ID = "_id";
	public static final String TEXT = "text";
	public static final String SUBTEXT = "emote_subtext";//hover text
	public static final String URL = "url";
	public static final String INDEX = "emote_index";
	public static final String CACHEFILE = "cachefile";//location of cached file or null if not cached yet.
	
	public static Pattern fileName_regex = Pattern.compile("/([^/]+)$");
	
	public static void getView(View current, AwfulPreferences aPref, Cursor data, AQuery aq) {
		aq.recycle(current);//I love AQ
		aq.find(R.id.emote_text).text(Html.fromHtml(data.getString(data.getColumnIndex(TEXT)))).textColor(aPref.postFontColor);
		aq.find(R.id.emote_icon).image(data.getString(data.getColumnIndex(URL)), true, true);
	}

	
	public static ArrayList<ContentValues> parseEmotes(Document data){
        String update_time = new Timestamp(System.currentTimeMillis()).toString();
		ArrayList<ContentValues> results = new ArrayList<ContentValues>();
		int index = 1;
		for(Element group : data.getElementsByClass("smilie_group")){
			Log.e(TAG,"Parsing group.");
			for(Element smilie : group.getElementsByClass("smilie")){
				Log.e(TAG,"Parsing item.");
				try{
					ContentValues emote = new ContentValues();
					Elements text = smilie.getElementsByClass("text");
					emote.put(ID, index++);//intentional post-increment
					emote.put(TEXT, text.text().trim());
					Elements img = smilie.getElementsByAttribute("src");
					emote.put(SUBTEXT, img.attr("title"));
					String url = img.attr("src");
					emote.put(AwfulEmote.URL, url);
					emote.put(INDEX, index);
		        	//timestamp for DB trimming
					emote.put(AwfulProvider.UPDATED_TIMESTAMP, update_time);
					results.add(emote);
				}catch(Exception e){
					e.printStackTrace();
					continue;
				}
			}
		}
		return results;
	}
}
