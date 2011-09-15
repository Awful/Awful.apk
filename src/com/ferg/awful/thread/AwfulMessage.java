package com.ferg.awful.thread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.htmlcleaner.TagNode;

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
public class AwfulMessage implements AwfulDisplayItem {
	
	private String mTitle;
	private String mAuthor;
	private String mContent;
	private String mDate;
	private int mId;

	@Override
	public int getID() {
		return mId;
	}

	@Override
	public DISPLAY_TYPE getType() {
		return null;
	}

	/**
	 * Generates List view for PM list.
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
		return current;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
	
	public static ArrayList<AwfulMessage> processMessageList(TagNode data){
		ArrayList<AwfulMessage> msgList = new ArrayList<AwfulMessage>();
		TagNode[] messagesParent = data.getElementsByAttValue("class", "standard full", true, true);
		if(messagesParent.length > 0){
			TagNode[] messages = messagesParent[0].getElementsByName("tr", true);
			for(TagNode msg : messages){
				//alright, this is gonna have magic numbers because these fields lack proper identifying classes
				//TODO sorry in advance when this eventually breaks.
				if(msg.getChildren().size() > 4){
					List<TagNode> items = msg.getChildren();
					AwfulMessage pm = new AwfulMessage();
					TagNode[] title = items.get(2).getElementsByName("a", true);
					if(title.length > 0){
						pm.mTitle = title[0].getText().toString();
						pm.mId = Integer.getInteger(title[0].getAttributeByName("href").replaceAll("\\D", ""));
					}
					pm.mAuthor = items.get(3).getText().toString();
					msgList.add(pm);
				}
			}
		}else{
			return null;//we'll use this to show that the load failed. i am still lazy.
		}
		return msgList;
	}
	
	public static AwfulMessage processMessage(TagNode data, AwfulMessage msg){
		if(msg == null){
			msg = new AwfulMessage();
			//we shouldn't receive this null, 
			//as we have no easy way to determine the message ID at this point
		}
		TagNode[] auth = data.getElementsByAttValue("class", "author", true, true);
		if(auth.length > 0){
			msg.mAuthor = auth[0].getText().toString();
		}else{
			return null;//we'll use this to show that the load failed. i am lazy.
		}
		TagNode[] content = data.getElementsByAttValue("class", "postbody", true, true);
		if(content.length > 0){
			msg.mContent = NetworkUtils.getAsString(content[0]);
		}else{
			return null;//i should probably put an exception throw here.
		}
		TagNode[] date = data.getElementsByAttValue("class", "postdate", true, true);
		if(date.length > 0){
			msg.mDate = date[0].getText().toString().replaceAll("\"", "").trim();
		}else{
			return null;
		}
		return msg;
	}

}
