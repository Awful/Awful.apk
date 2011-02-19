package com.ferg.awful;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import com.ferg.awful.async.ImageDownloader;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;

public class ThreadDisplayActivity extends Activity {
    private static final String TAG = "LoginActivity";

	private final ImageDownloader mImageDownloader = new ImageDownloader();

    private ListView mPostList;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
        mPrefs = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        mPostList = (ListView) findViewById(R.id.thread_posts);

		String username = mPrefs.getString(Constants.PREF_USERNAME, null);
		String password = mPrefs.getString(Constants.PREF_PASSWORD, null);

		if (username == null || password == null) {
			startActivity(new Intent().setClass(this, AwfulLoginActivity.class));
		}

        new LoginTask().execute(username, password);
    }

    private class LoginTask extends AsyncTask<String, Void, AwfulThread> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(ThreadDisplayActivity.this, "Loading", 
                "Hold on...", true);
        }

        public AwfulThread doInBackground(String... aParams) {
            AwfulThread result = null;

            HashMap<String, String> params = new HashMap<String, String>();
            params.put(Constants.PARAM_USERNAME, aParams[0]);
            params.put(Constants.PARAM_PASSWORD, aParams[1]);
            params.put(Constants.PARAM_ACTION, "login");
            
            try {
                NetworkUtils.post(Constants.FUNCTION_LOGIN, params);

                result = AwfulThread.getThread("3362097", 82);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, e.toString());
            }

            return result;
        }

        public void onPostExecute(AwfulThread aResult) {
            mPostList.setAdapter(new AwfulPostAdapter(ThreadDisplayActivity.this, 
                        R.layout.post_item, aResult.getPosts()));

            mDialog.dismiss();
        }
    }

    public class AwfulPostAdapter extends ArrayAdapter<AwfulPost> {
        private ArrayList<AwfulPost> mPosts;
        private int mViewResource;
        private LayoutInflater mInflater;

        public AwfulPostAdapter(Context aContext, int aViewResource, ArrayList<AwfulPost> aPosts) {
            super(aContext, aViewResource, aPosts);

            mInflater     = LayoutInflater.from(aContext);
            mPosts        = aPosts;
            mViewResource = aViewResource;
        }

        @Override
        public View getView(int aPosition, View aConvertView, ViewGroup aParent) {
            View inflatedView = aConvertView;

            if (inflatedView == null) {
                inflatedView = mInflater.inflate(mViewResource, null);
            }

            AwfulPost current = getItem(aPosition);

            TextView username = (TextView) inflatedView.findViewById(R.id.username);
            TextView postDate = (TextView) inflatedView.findViewById(R.id.post_date);
            TextView postBody = (TextView) inflatedView.findViewById(R.id.postbody);
            ImageView avatar  = (ImageView) inflatedView.findViewById(R.id.avatar);

            username.setText(current.getUsername());
            postDate.setText("Posted on " + current.getDate());
            postBody.setText(Html.fromHtml(current.getContent()));

            // TODO: Why is this crashing when using the cache? Seems to be gif related.
            mImageDownloader.download(current.getAvatar(), avatar);

            return inflatedView;
        }
    }
}
