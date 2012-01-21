package com.ferg.awful.thread;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.preferences.AwfulPreferences;

public class AwfulEmote {
    public static final String PATH     = "/thread";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

	public static final String ID = "_id";
	public static final String TEXT = "text";
	public static final String SUBTEXT = "subtext";//hover text
	public static final String URL = "url";
	public static final String CACHEFILE = "cachefile";//location of cached file or null if not cached yet.
	
	private static Pattern fileName_regex = Pattern.compile("/([^/]+)$");
	
	public static void getView(View current, AwfulPreferences aPref, Cursor data) {
		TextView title = (TextView) current.findViewById(R.id.title);//TODO add proper emote list element
		TextView sub = (TextView) current.findViewById(R.id.subtext);
		if(aPref != null){
			title.setTextColor(aPref.postFontColor);
			sub.setTextColor(aPref.postFontColor2);
		}
		title.setText(Html.fromHtml(data.getString(data.getColumnIndex(TEXT))));
		sub.setText(data.getString(data.getColumnIndex(SUBTEXT)));
	}

	
	public static ArrayList<ContentValues> parseEmotes(TagNode data){
		ArrayList<ContentValues> results = new ArrayList<ContentValues>();
		int index = 1;
		TagNode[] groups = data.getElementsByAttValue("class", "smilie_group", true, false);
		for(TagNode group : groups){
			TagNode[] smilies = group.getElementsByAttValue("class", "smilie", true, false);
			for(TagNode smilie : smilies){
				try{
					ContentValues emote = new ContentValues();
					TagNode text = smilie.findElementByAttValue("class", "text", true, false);
					emote.put(AwfulEmote.ID, index++);//intentional post-increment
					emote.put(AwfulEmote.TEXT, text.getText().toString().trim());
					TagNode img = smilie.findElementByName("img", true);
					emote.put(AwfulEmote.SUBTEXT, img.getAttributeByName("title"));
					String url = img.getAttributeByName("src");
					emote.put(AwfulEmote.URL, url);
					Matcher fileName = fileName_regex.matcher(url);
					if(fileName.find()){
						emote.put(AwfulEmote.CACHEFILE, fileName.group(1));
						results.add(emote);
					}else{
						continue;
					}
				}catch(Exception e){
					continue;
				}
			}
		}
		return results;
	}
}
