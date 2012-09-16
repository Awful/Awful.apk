package com.ferg.awfulapp.thread;

import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;
import org.htmlcleaner.TagNode;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.ColorPickerPreference;
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
	public static final String RECIPIENT = "recipient";
	public static final String REPLY_CONTENT = "reply_content";
	public static final String REPLY_TITLE = "reply_title";

	public static final int TYPE_PM = 1;
	public static final int TYPE_NEW_REPLY = 2;
	public static final int TYPE_QUOTE = 3;
	public static final int TYPE_EDIT = 4;

	/**
	 * Generates List view items for PM list.
	 * @param mIsSidebar 
	 */
	public static View getView(View current, AwfulPreferences aPref, Cursor data, boolean selected) {
		TextView title = (TextView) current.findViewById(R.id.title);
		String t = data.getString(data.getColumnIndex(TITLE));
		if(t != null){
			title.setText(t);
		}
		TextView author = (TextView) current.findViewById(R.id.subtext);
		String auth = data.getString(data.getColumnIndex(AUTHOR));
		String date = data.getString(data.getColumnIndex(AUTHOR));
		if(auth != null && date != null){
			author.setText(auth +" - "+date);
		}

        ImageView unreadPM = (ImageView) current.findViewById(R.id.sticky_icon);

		if (data.getInt(data.getColumnIndex(UNREAD))>0) {
			unreadPM.setVisibility(View.VISIBLE);
		}else{
			unreadPM.setVisibility(View.GONE);
		}

		if(aPref != null){
			title.setTextColor(aPref.postFontColor);
			author.setTextColor(aPref.postFontColor2);
		}
		if(selected){
			current.findViewById(R.id.selector).setVisibility(View.VISIBLE);
		}else{
			current.findViewById(R.id.selector).setVisibility(View.GONE);
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
					pm.put(CONTENT, " ");
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
	
	public static ContentValues processMessage(TagNode data, int id) throws Exception{
		ContentValues message = new ContentValues();
		message.put(ID, id);
		TagNode[] auth = data.getElementsByAttValue("class", "author", true, true);
		if(auth.length > 0){
			message.put(AUTHOR, auth[0].getText().toString());
		}else{
			throw new Exception("Failed parse: author.");
		}
		TagNode[] content = data.getElementsByAttValue("class", "postbody", true, true);
		if(content.length > 0){
			message.put(CONTENT, NetworkUtils.getAsString(content[0]));
		}else{
			throw new Exception("Failed parse: content.");
		}
		TagNode[] date = data.getElementsByAttValue("class", "postdate", true, true);
		if(date.length > 0){
			message.put(DATE, date[0].getText().toString().replaceAll("\"", "").trim());
		}else{
			throw new Exception("Failed parse: date.");
		}
		return message;
	}

	public static ContentValues processReplyMessage(TagNode pmReplyData, int id) {
		ContentValues reply = new ContentValues();
		reply.put(ID, id);
		reply.put(TYPE, TYPE_PM);
		TagNode[] message = pmReplyData.getElementsByAttValue("name", "message", true, false);
		if(message.length >0){
			String quoteText = StringEscapeUtils.unescapeHtml4(message[0].getText().toString().replaceAll("[\\r\\f]", ""));
			reply.put(REPLY_CONTENT, quoteText);
		}
		TagNode[] title = pmReplyData.getElementsByAttValue("name", "title", true, false);
		if(title.length >0){
			String quoteTitle = StringEscapeUtils.unescapeHtml4(title[0].getAttributeByName("value"));
			reply.put(TITLE, quoteTitle);
		}
		return reply;
	}
	
	public static String getMessageHtml(String content, AwfulPreferences pref){
		//String content = data.getString(data.getColumnIndex(CONTENT));
		if(content!=null){
			StringBuffer buff = new StringBuffer(content.length());
			buff.append("<div class='pm_body'style='color: " + ColorPickerPreference.convertToARGB(pref.postFontColor) + "; font-size: " + pref.postFontSizePx + ";'>");
			buff.append(content.replaceAll("<blockquote>", "<div style='margin-left: 20px'>").replaceAll("</blockquote>", "</div>"));//babbys first CSS hack
			buff.append("</div>");
			return buff.toString();
		}
		return "";
	}
}
