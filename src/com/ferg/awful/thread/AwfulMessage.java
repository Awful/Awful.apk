package com.ferg.awful.thread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlcleaner.TagNode;

import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
		TextView unread = (TextView) current.findViewById(R.id.unread_count);
		unread.setVisibility(View.GONE);
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
		TagNode[] messagesParent = data.getElementsByAttValue("name", "form", true, true);
		if(messagesParent.length > 0){
			TagNode[] messages = messagesParent[0].getElementsByName("a", true);
			for(TagNode msg : messages){
				//alright, this is gonna have magic numbers because these fields lack proper identifying classes
				//TODO sorry in advance when this eventually breaks.
				//if(msg.getChildren().size() > 4){
					//List<TagNode> items = msg.getChildren();
					//TagNode[] title = items.get(2).getElementsByName("a", true);
					//if(title.length > 0){
				String href = msg.getAttributeByName("href");
				if(href != null){
					AwfulMessage pm = new AwfulMessage(Integer.parseInt(href.replaceAll("\\D", "")));
					pm.mTitle = msg.getText().toString();
					pm.mAuthor = "Dunno";
					msgList.add(pm);
				}
					//}
				//}
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
