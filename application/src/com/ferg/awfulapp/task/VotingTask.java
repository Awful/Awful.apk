package com.ferg.awfulapp.task;

import java.util.HashMap;

import android.content.Context;
import android.os.Message;
import android.widget.Toast;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.service.AwfulSyncService;

public class VotingTask extends AwfulTask {
	protected Context ApplicationContext;
	/**
	 * @param id Thread id.
	 * @param arg1 Vote (1-5)
	 */
	public VotingTask(AwfulSyncService sync, Message aMsg, Context cxt) {
		super(sync, aMsg, null, AwfulSyncService.MSG_VOTE);
		this.ApplicationContext = cxt;
	}

	@Override
	protected Boolean doInBackground(Void... p) {
		if (!isCancelled()) {
        	
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(Constants.PARAM_THREAD_ID, String.valueOf(mId));
			params.put(Constants.PARAM_VOTE, String.valueOf(mArg1+1));

            try {
            	NetworkUtils.postIgnoreBody(Constants.FUNCTION_RATE_THREAD, params);
            } catch (Exception e) {
                e.printStackTrace();
            	return false;
            }
        }
        return true;
	}

	@Override
	public void onPostExecute(Boolean success){
		if(!isCancelled()){
			if(success){
				mContext.updateStatus(replyTo, TYPE, AwfulSyncService.Status.OKAY, mId, mArg1);
				Toast successToast = Toast.makeText(ApplicationContext, String.format(this.mContext.getString(R.string.vote_succeeded), mArg1+1), Toast.LENGTH_LONG);
				successToast.show();
			}else{
				mContext.updateStatus(replyTo, TYPE, AwfulSyncService.Status.ERROR, mId, mArg1);
				Toast errorToast = Toast.makeText(ApplicationContext, R.string.vote_failed, Toast.LENGTH_LONG);
				errorToast.show();
			}
			mContext.taskFinished(this);
		}
	}
}
