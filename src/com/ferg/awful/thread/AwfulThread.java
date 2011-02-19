package com.ferg.awful.thread;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;

public class AwfulThread {
    private static final String TAG = "AwfulThread";

    private String mThreadId;
    private ArrayList<AwfulPost> mPosts;

    public AwfulThread(String aThreadId) {
        mThreadId = aThreadId;
    }

    public ArrayList<AwfulPost> getPosts() {
        return mPosts;
    }

    public void setPosts(ArrayList<AwfulPost> aPosts) {
        mPosts = aPosts;
    }

    public static AwfulThread getThread(String aThreadId, int aPage) throws Exception {
        AwfulThread result = new AwfulThread(aThreadId);

        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_THREAD_ID, aThreadId);
        params.put(Constants.PARAM_PAGE, Integer.toString(aPage));

        result.setPosts(AwfulPost.parsePosts(
                    NetworkUtils.get(Constants.FUNCTION_THREAD, params)));

        return result;
    }
}
