package com.ferg.awful.task;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;

import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulForum;

public class IndexTask extends AwfulTask {

	public IndexTask(AwfulSyncService sync, int id, int arg1, AwfulPreferences aPrefs) {
		super(sync, id, arg1, aPrefs, AwfulSyncService.MSG_SYNC_INDEX);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (!isCancelled()) {
            try {
                TagNode response = NetworkUtils.get(Constants.BASE_URL);
                AwfulForum.getForumsFromRemote(response, mContext.getContentResolver());
                TagNode[] pmBlock = response.getElementsByAttValue("id", "pm", true, true);
                try{
                    if(pmBlock.length >0){
                    	TagNode[] bolded = pmBlock[0].getElementsByName("b", true);
                    	if(bolded.length > 1){
                    		String name = bolded[0].getText().toString().split("'")[0];
                    		String unread = bolded[1].getText().toString();
                    		Pattern findUnread = Pattern.compile("(\\d+)\\s+unread");
                    		Matcher matchUnread = findUnread.matcher(unread);
                    		int unreadCount = -1;
                    		if(matchUnread.find()){
                    			unreadCount = Integer.parseInt(matchUnread.group(1));
                    		}
                        	Log.v(TAG,"text: "+name+" - "+unreadCount);
                        	if(name != null && name.length() > 0){
                        		mPrefs.setUsername(name);
                        	}
                    	}
                    }
                }catch(Exception e){
                	//this chunk is optional, no need to fail everything if it doens't work out.
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
	}

}
