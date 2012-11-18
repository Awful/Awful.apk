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

package com.ferg.awfulapp.task;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulPagedItem;
import com.ferg.awfulapp.widget.AwfulFragmentPagerAdapter.AwfulPagerFragment;

public class IndexTask extends AwfulTask {

	public IndexTask(AwfulSyncService sync, Message aMsg, AwfulPreferences aPrefs) {
		super(sync, aMsg, aPrefs, AwfulSyncService.MSG_SYNC_INDEX);
	}

	@Override
	protected String doInBackground(Void... params) {
		if (!isCancelled()) {
            try {
                replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, 0, 10));
                Document response = NetworkUtils.get(Constants.BASE_URL);
                String error = AwfulPagedItem.checkPageErrors(response, replyTo);
                if(error != null){
                	return error;
                }
                replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, 0, 50));
                AwfulForum.getForumsFromRemote(response, mContext.getContentResolver());
                replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, 0, 90));
                Elements pmBlock = response.getElementsByAttributeValue("id", "pm");
                try{
                    if(pmBlock.size() >0){
                    	Elements bolded = pmBlock.first().getElementsByTag("b");
                    	if(bolded.size() > 1){
                    		String name = bolded.first().text().split("'")[0];
                    		String unread = bolded.get(1).text();
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
                return "Failed to load Forum List!";
            }
        }
        return null;
	}

}
