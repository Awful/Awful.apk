package com.ferg.awfulapp.thread;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;

public class AwfulEmote {
    public static final String PATH     = "/emote";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

	public static final String ID = "_id";
	public static final String TEXT = "text";
	public static final String SUBTEXT = "subtext";//hover text
	public static final String URL = "url";
	public static final String CACHEFILE = "cachefile";//location of cached file or null if not cached yet.
	
	public static Pattern fileName_regex = Pattern.compile("/([^/]+)$");
	
	public static void getView(View current, AwfulPreferences aPref, Cursor data) {
		TextView title = (TextView) current.findViewById(R.id.title);//TODO add proper emote list element
		TextView sub = (TextView) current.findViewById(R.id.subtext);
		ImageView img = (ImageView) current.findViewById(R.id.bookmark_icon);
		img.setVisibility(View.VISIBLE);
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
	
	public static Bitmap getEmote(Context aContext, String fileName){
		try{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				File cacheDir;
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO){
					cacheDir = new File(Environment.getExternalStorageDirectory(),"Android/data/com.ferg.awfulapp/cache/emotes/");
				}else{
					cacheDir = new File(aContext.getExternalCacheDir(),"emotes/");
				}
				File cachedImg = new File(cacheDir, fileName);
				if(cachedImg.exists() && cachedImg.canRead()){
					FileInputStream is = new FileInputStream(cachedImg);
					Bitmap data = BitmapFactory.decodeStream(is);
					is.close();
					return data;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
