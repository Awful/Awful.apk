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
import android.widget.AdapterView;
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

public class ForumDisplayActivity extends Activity {
    private static final String TAG = "LoginActivity";

	private final ImageDownloader mImageDownloader = new ImageDownloader();

    private ListView mThreadList;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forum_display);
		
        mPrefs = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        mThreadList = (ListView) findViewById(R.id.forum_list);

		String username = mPrefs.getString(Constants.PREF_USERNAME, null);
		String password = mPrefs.getString(Constants.PREF_PASSWORD, null);

		if (username == null || password == null) {
			startActivity(new Intent().setClass(this, AwfulLoginActivity.class));
		}

        new LoginTask().execute(username, password);
    }

    private class LoginTask extends AsyncTask<String, Void, ArrayList<AwfulThread>> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(ForumDisplayActivity.this, "Loading", 
                "Hold on...", true);
        }

        public ArrayList<AwfulThread> doInBackground(String... aParams) {
            ArrayList<AwfulThread> result = new ArrayList<AwfulThread>();

            HashMap<String, String> params = new HashMap<String, String>();
            params.put(Constants.PARAM_USERNAME, aParams[0]);
            params.put(Constants.PARAM_PASSWORD, aParams[1]);
            params.put(Constants.PARAM_ACTION, "login");
            
            try {
                NetworkUtils.post(Constants.FUNCTION_LOGIN, params);

                result = AwfulThread.getForumThreads("192");
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, e.toString());
            }

            return result;
        }

        public void onPostExecute(ArrayList<AwfulThread> aResult) {
            mThreadList.setAdapter(new AwfulThreadAdapter(ForumDisplayActivity.this, 
                        R.layout.thread_item, aResult));

            mThreadList.setOnItemClickListener(onThreadSelected);

            mDialog.dismiss();
        }
    }

	private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulThreadAdapter adapter = (AwfulThreadAdapter) mThreadList.getAdapter();
            AwfulThread thread = adapter.getItem(aPosition);

            Intent viewThread = new Intent().setClass(ForumDisplayActivity.this, ThreadDisplayActivity.class);
            viewThread.putExtra(Constants.THREAD_ID, thread.getThreadId());

            startActivity(viewThread);
		}
	};

    public class AwfulThreadAdapter extends ArrayAdapter<AwfulThread> {
        private ArrayList<AwfulThread> mThreads;
        private int mViewResource;
        private LayoutInflater mInflater;

        public AwfulThreadAdapter(Context aContext, int aViewResource, ArrayList<AwfulThread> aThreads) {
            super(aContext, aViewResource, aThreads);

            mInflater     = LayoutInflater.from(aContext);
            mThreads        = aThreads;
            mViewResource = aViewResource;
        }

        @Override
        public View getView(int aPosition, View aConvertView, ViewGroup aParent) {
            View inflatedView = aConvertView;

            if (inflatedView == null) {
                inflatedView = mInflater.inflate(mViewResource, null);
            }

            AwfulThread current = getItem(aPosition);

            TextView title       = (TextView) inflatedView.findViewById(R.id.title);
            TextView pages       = (TextView) inflatedView.findViewById(R.id.pages);
            TextView unreadCount = (TextView) inflatedView.findViewById(R.id.unread_count);
            ImageView sticky     = (ImageView) inflatedView.findViewById(R.id.sticky_icon);

            title.setText(Html.fromHtml(current.getTitle()));
            pages.setText("Pages: 4");
            unreadCount.setText(Integer.toString(current.getUnreadCount()));

            if (current.isSticky()) {
                sticky.setImageResource(R.drawable.sticky);
            } else {
                sticky.setImageDrawable(null);
            }

            return inflatedView;
        }
    }
}
