package com.ferg.awful.thread;

import java.util.ArrayList;

public class AwfulPrivateMessages extends AwfulPagedItem {
	private ArrayList<AwfulMessage> messages;
	public AwfulPrivateMessages(){
		messages = new ArrayList<AwfulMessage>();
		mLastPage = 1;//just support one page for now.
	}

	@Override
	public AwfulDisplayItem getChild(int page, int ix) {
		return messages.get(ix);
	}

	@Override
	public ArrayList<? extends AwfulDisplayItem> getChildren(int page) {
		return messages;
	}

	@Override
	public int getChildrenCount(int page) {
		return messages.size();
	}

	@Override
	public int getID() {
		return 0;
	}

	@Override
	public boolean isPageCached(int page) {
		return messages.size()>0;
	}
	
	public ArrayList<AwfulMessage> getMessageList(){
		return messages;
	}

	public void setMessageList(ArrayList<AwfulMessage> mList) {
		messages = mList;
	}

}
