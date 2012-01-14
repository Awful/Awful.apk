package com.ferg.awful.thread;

import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;
import org.htmlcleaner.TagNode;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.preferences.ColorPickerPreference;
/**
 * SA Private Messages.
 * @author Geekner
 */
public class AwfulMessage extends AwfulPagedItem {
	
	private static final String TAG = "AwfulMessage";
	
    public static final String PATH     = "/privatemessages";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);
    public static final String PATH_REPLY     = "/draftreplies";
    public static final Uri CONTENT_URI_REPLY = Uri.parse("content://" + Constants.AUTHORITY + PATH_REPLY);
	
	public static final String ID = "_id";
    public static final String TITLE 		="title";
    public static final String AUTHOR 		="author";
    public static final String CONTENT 	="content";
    public static final String DATE 	="message_date";
	public static final String TYPE = "message_type";
	public static final String UNREAD = "unread_message";
	
	private String mTitle;
	private String mAuthor;
	private String mContent;
	private String mDate;
	private String mReplyText;
	private boolean unread;
	private boolean mLoaded;
	private int mId;
	private String mReplyTitle;

	public AwfulMessage(int id) {
		mId = id;
		mDate = "";
		mLoaded = false;
	}

	/**
	 * Generates List view items for PM list.
	 */
	public static View getView(View current, AwfulPreferences aPref, Cursor data) {
		TextView title = (TextView) current.findViewById(R.id.title);
		title.setText(data.getString(data.getColumnIndex(TITLE)));
		TextView author = (TextView) current.findViewById(R.id.subtext);
		author.setText(data.getString(data.getColumnIndex(AUTHOR)) +" - "+data.getString(data.getColumnIndex(DATE)));

        ImageView unreadPM = (ImageView) current.findViewById(R.id.sticky_icon);

		if (data.getInt(data.getColumnIndex(TITLE))>0) {
			unreadPM.setVisibility(View.VISIBLE);
		}else{
			unreadPM.setVisibility(View.GONE);
		}

		if(aPref != null){
			title.setTextColor(aPref.postFontColor);
			author.setTextColor(aPref.postFontColor2);
		}

		return current;
	}
	
	public static void processMessageList(ContentResolver contentInterface, TagNode data) throws Exception{
		ArrayList<ContentValues> msgList = new ArrayList<ContentValues>();
		
		/**METHOD One: Parse PM links. Easy, but only contains id+title.**/
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
		
		/**METHOD Two: Parse table structure, hard and quick to break.**/
		TagNode[] messagesParent = data.getElementsByAttValue("name", "form", true, true);
		if(messagesParent.length > 0){
			TagNode[] messages = messagesParent[0].getElementsByName("tr", true);
			for(TagNode msg : messages){
				ContentValues pm = new ContentValues();
				//fuck i hate scraping shit.
				//no usable identifiers on the PM list, no easy method to find author/post date.
				//this will break if they change the display structure.
				TagNode[] row = msg.getChildTags();
				if(row != null && row.length > 4){
					//TODO abandon hope, all ye who enter
					//row[0] - icon, newpm.gif - sublevel
					//row[1] - post icon TODO if we ever add icon support - sublevel
					//row[2] - pm subject/link - sublevel
					//row[3] - sender
					//row[4] - date
					TagNode href = row[2].getChildTags()[0];
					pm.put(ID, Integer.parseInt(href.getAttributeByName("href").replaceAll("\\D", "")));
					pm.put(TITLE, href.getText().toString());
					pm.put(AUTHOR, row[3].getText().toString());
					pm.put(DATE, row[4].getText().toString());
					if(row[0].getChildTags()[0].getAttributeByName("src").contains("newpm.gif")){
						pm.put(UNREAD, 1);
					}else{
						pm.put(UNREAD, 0);
					}
					msgList.add(pm);
				}
			}
		}else{
			throw new Exception("Failed to parse message parent");
		}
		contentInterface.bulkInsert(CONTENT_URI, msgList.toArray(new ContentValues[msgList.size()]));
	}
	
	public static AwfulMessage processMessage(TagNode data, AwfulMessage msg){
		TagNode[] auth = data.getElementsByAttValue("class", "author", true, true);
		if(auth.length > 0){
			msg.mAuthor = auth[0].getText().toString();
		}else{
			Log.e(TAG, "Failed parse: author.");
			return null;//we'll use this to show that the load failed. i am lazy.
		}
		TagNode[] content = data.getElementsByAttValue("class", "postbody", true, true);
		if(content.length > 0){
			msg.mContent = NetworkUtils.getAsString(content[0]);
		}else{
			Log.e(TAG, "Failed parse: content.");
			return null;//i should probably put an exception throw here.
		}
		TagNode[] date = data.getElementsByAttValue("class", "postdate", true, true);
		if(date.length > 0){
			msg.mDate = date[0].getText().toString().replaceAll("\"", "").trim();
		}else{
			Log.e(TAG, "Failed parse: date.");
			return null;
		}
		return msg;
	}

	public static void processReplyMessage(TagNode pmReplyData, AwfulMessage pm) {
		TagNode[] message = pmReplyData.getElementsByAttValue("name", "message", true, false);
		if(message.length >0){
			String quoteText = StringEscapeUtils.unescapeHtml4(message[0].getText().toString().replaceAll("[\\r\\f]", ""));
			pm.setReplyText(quoteText);
		}
		TagNode[] title = pmReplyData.getElementsByAttValue("name", "title", true, false);
		if(title.length >0){
			String quoteTitle = StringEscapeUtils.unescapeHtml4(title[0].getAttributeByName("value"));
			pm.setReplyTitle(quoteTitle);
		}
	}

	private void setReplyTitle(String quoteTitle) {
		mReplyTitle = quoteTitle;
	}
	
	public String getReplyTitle() {
		return mReplyTitle;
	}


	private synchronized void setReplyText(String string) {
		mReplyText = string;
	}
	
	public synchronized String getReplyText(){
		return mReplyText;
	}
	
	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String mTitle) {
		this.mTitle = mTitle;
	}

	public String getAuthor() {
		return mAuthor;
	}

	public void setAuthor(String mAuthor) {
		this.mAuthor = mAuthor;
	}

	public String getContent() {
		return mContent;
	}

	public void setContent(String mContent) {
		this.mContent = mContent;
	}

	public String getDate() {
		return mDate;
	}

	public void setDate(String mDate) {
		this.mDate = mDate;
	}
	
	public void setLoaded(boolean loaded){
		mLoaded = loaded;
	}
	
	public String getMessageHtml(AwfulPreferences pref){
		if(mContent!=null){
			StringBuffer buff = new StringBuffer(mContent.length());
			buff.append("<div class='pm_body'style='color: " + ColorPickerPreference.convertToARGB(pref.postFontColor) + "; font-size: " + pref.postFontSize + ";'>");
			buff.append(mContent.replaceAll("<blockquote>", "<div style='margin-left: 20px'>").replaceAll("</blockquote>", "</div>"));//babbys first CSS hack
			buff.append("</div>");
			return buff.toString();
		}
		return "";
	}
}
