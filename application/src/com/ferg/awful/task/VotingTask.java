package com.ferg.awful.task;

import java.util.HashMap;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulSyncService;

public class VotingTask extends AwfulTask {
	/**
	 * @param id Thread id.
	 * @param arg1 Vote (1-5)
	 */
	public VotingTask(AwfulSyncService sync, int id, int arg1) {
		super(sync, id, arg1, null, AwfulSyncService.MSG_VOTE);
	}

	@Override
	protected Boolean doInBackground(Void... p) {
		if (!isCancelled()) {
        	
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(Constants.PARAM_THREAD_ID, String.valueOf(mId));
			params.put(Constants.PARAM_VOTE, String.valueOf(mArg1));

            try {
            	NetworkUtils.post(Constants.FUNCTION_RATE_THREAD, params);
            } catch (Exception e) {
                e.printStackTrace();
            	return false;
            }
        }
        return true;
	}

}
