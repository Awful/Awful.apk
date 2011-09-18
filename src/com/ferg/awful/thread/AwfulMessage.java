package com.ferg.awful.thread;

import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlcleaner.TagNode;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ferg.awful.R;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
/**
 * SA Private Messages.
 * @author Geekner
 */
public class AwfulMessage extends AwfulPagedItem implements AwfulDisplayItem {
	
	private static final String TAG = "AwfulMessage";
	private String mTitle;
	private String mAuthor;
	private String mContent;
	private String mDate;
	private String mReplyText;
	private boolean unread;
	private int mId;

	public AwfulMessage(int id) {
		mId = id;
		mDate = "";
	}

	@Override
	public int getID() {
		return mId;
	}

	@Override
	public DISPLAY_TYPE getType() {
		return DISPLAY_TYPE.THREAD;
	}
	
	public boolean isUnread(){
		return unread;
	}

	/**
	 * Generates List view items for PM list.
	 */
	@Override
	public View getView(LayoutInflater inf, View current, ViewGroup parent,
			AwfulPreferences aPref) {
		if(current == null || current.getId() != R.layout.thread_item){
			current = inf.inflate(R.layout.thread_item, parent, false);
		}
		TextView title = (TextView) current.findViewById(R.id.title);
		title.setText(mTitle);
		TextView author = (TextView) current.findViewById(R.id.author);
		author.setText(mAuthor +" - "+mDate);
		TextView unreadCount = (TextView) current.findViewById(R.id.unread_count);
		unreadCount.setVisibility(View.GONE);
		ImageView unreadPM = (ImageView) current.findViewById(R.id.sticky_icon);
		if(unread){
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

	@Override
	public boolean isEnabled() {
		return true;
	}
	
	public static ArrayList<AwfulMessage> processMessageList(TagNode data){
		ArrayList<AwfulMessage> msgList = new ArrayList<AwfulMessage>();
		
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
					AwfulMessage pm = new AwfulMessage(Integer.parseInt(href.getAttributeByName("href").replaceAll("\\D", "")));
					pm.mTitle = href.getText().toString();
					pm.mAuthor = row[3].getText().toString();
					pm.mDate = row[4].getText().toString();
					pm.unread = row[0].getChildTags()[0].getAttributeByName("src").contains("newpm.gif");
					msgList.add(pm);
				}
			}
		}else{
			Log.e("AwfulMessage","Failed to parse message parent");
			return null;//we'll use this to show that the load failed. i am still lazy.
		}
		return msgList;
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
			String quoteText = StringEscapeUtils.unescapeHtml(message[0].getText().toString().replaceAll("[\\r\\f]", ""));
			pm.setReplyText(quoteText);
		}
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
	
	//extended awfulpageditem so messages can be put in the DB, I really need to rewrite the DB.
	//ignore the methods here, they'll never be used.
	@Override
	public AwfulDisplayItem getChild(int page, int ix) {
		return null;
	}

	@Override
	public ArrayList<? extends AwfulDisplayItem> getChildren(int page) {
		return null;
	}

	@Override
	public int getChildrenCount(int page) {
		return 0;
	}

	@Override
	public boolean isPageCached(int page) {
		return false;
	}

}
