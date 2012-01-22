package com.ferg.awful;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulCursorAdapter;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulMessage;

import android.app.ActionBar;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PrivateMessageListFragment extends Fragment implements
		AwfulUpdateCallback {
	

    private static final String TAG = "PrivateMessageList";

    private ImageButton mHome;
    private ImageButton mNewPM;
    private ListView mPMList;
    private TextView mTitle;
    private AwfulPreferences mPrefs;
    private ImageButton mRefresh;

	private AwfulCursorAdapter mCursorAdapter;

	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
        	Log.i(TAG, "Received Message:"+aMsg.what+" "+aMsg.arg1+" "+aMsg.arg2);
            switch (aMsg.arg1) {
                case AwfulSyncService.Status.OKAY:
                	loadingSucceeded();
                	if(aMsg.what == AwfulSyncService.MSG_FETCH_PM_INDEX){
                		getActivity().getSupportLoaderManager().restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, mPMDataCallback);
                	}
                    break;
                case AwfulSyncService.Status.WORKING:
                	loadingStarted();
                    break;
                case AwfulSyncService.Status.ERROR:
                	loadingFailed();
                    break;
                default:
                    super.handleMessage(aMsg);
            }
        }
    };
    private Messenger mMessenger = new Messenger(mHandler);
    private PMIndexCallback mPMDataCallback = new PMIndexCallback(mHandler);
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        mPrefs = new AwfulPreferences(this.getActivity());
        
        View result = aInflater.inflate(R.layout.private_message_fragment, aContainer, false);

        mPMList = (ListView) result.findViewById(R.id.message_listview);

        if (AwfulActivity.useLegacyActionbar()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();
            mHome          = (ImageButton) actionbar.findViewById(R.id.home);
            mNewPM          = (ImageButton) actionbar.findViewById(R.id.new_pm);
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
            mTitle.setText(getString(R.string.private_message));
            mRefresh       = (ImageButton) actionbar.findViewById(R.id.refresh);
        } else {
            setActionBar();
        }
        
        return result;
    }
    
    private void setActionBar() {
        ActionBar action = getActivity().getActionBar();
        action.setBackgroundDrawable(getResources().getDrawable(R.drawable.bar));
        action.setDisplayHomeAsUpEnabled(true);
        action.setTitle("Awful - Private Messages");
    }
    
    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setRetainInstance(true);


        if (AwfulActivity.useLegacyActionbar()) {
            mHome.setOnClickListener(onButtonClick);
            mNewPM.setOnClickListener(onButtonClick);
            mRefresh.setOnClickListener(onButtonClick);
        }
        
        updateColors(mPrefs);
        mPMList.setCacheColorHint(mPrefs.postBackgroundColor);

        mPMList.setOnItemClickListener(onPMSelected);
        
        mCursorAdapter = new AwfulCursorAdapter((AwfulActivity) getActivity(), null);
        mPMList.setAdapter(mCursorAdapter);
    }
    
    private void updateColors(AwfulPreferences pref){
    	if(mPMList != null){
    		mPMList.setBackgroundColor(pref.postBackgroundColor);
    		mPMList.setCacheColorHint(pref.postBackgroundColor);
    	}
    }
    
    @Override
    public void onStart(){
    	super.onStart();
        ((AwfulActivity) getActivity()).registerSyncService(mMessenger, Constants.PRIVATE_MESSAGE_THREAD);
		getActivity().getSupportLoaderManager().restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, mPMDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulForum.CONTENT_URI, true, mPMDataCallback);
        syncPMs();
    }
    
    private void syncPMs() {
    	((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_FETCH_PM_INDEX, Constants.PRIVATE_MESSAGE_THREAD, 0);
	}

	@Override
    public void onResume() {
        super.onResume();
    }
	
	@Override
	public void onStop(){
		super.onStop();
        ((AwfulActivity) getActivity()).unregisterSyncService(mMessenger, Constants.PRIVATE_MESSAGE_THREAD);
		getActivity().getSupportLoaderManager().destroyLoader(Constants.PRIVATE_MESSAGE_THREAD);
		getActivity().getContentResolver().unregisterContentObserver(mPMDataCallback);
	}
    
    @Override
    public void onDetach() {
        super.onDetach();
        mPrefs.unRegisterListener();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() == 0){
            inflater.inflate(R.menu.private_message_menu, menu);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.new_pm:
        	if(getActivity() instanceof PrivateMessageActivity){
                ((PrivateMessageActivity) getActivity()).showMessage(null, 0);
        	}
        	break;
        case R.id.send_pm:
        	if(getActivity() instanceof PrivateMessageActivity){
                ((PrivateMessageActivity) getActivity()).sendMessage();
        	}
        	break;
        case R.id.refresh:
        	syncPMs();
        	break;
        case R.id.settings:
        	startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
        	break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
    
    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.home:
                    startActivity(new Intent().setClass(getActivity(), ForumsIndexActivity.class));
                    break;
                case R.id.new_pm:
                    startActivity(new Intent().setClass(getActivity(), MessageDisplayActivity.class));
                    break;
                case R.id.refresh:
                	syncPMs();
                    break;
            }
        }
    };
    
    private AdapterView.OnItemClickListener onPMSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            if(getActivity() instanceof PrivateMessageActivity){
            	((PrivateMessageActivity) getActivity()).showMessage(null, (int)aId);
            }else{
            	startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, (int) aId));
            }
        }
    };

	@Override
    public void loadingFailed() {
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setAnimation(null);
            mRefresh.setImageResource(android.R.drawable.ic_dialog_alert);
          //TODO mRefresh.startAnimation(adapt.getBlinkingAnimation());
        }else{
        	if(getActivity()!= null){
            	getActivity().setProgressBarIndeterminateVisibility(false);
        	}
        }
        if(getActivity()!= null){
        	Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void loadingStarted() {
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setImageResource(R.drawable.ic_menu_refresh);
          //TODO mRefresh.startAnimation(adapt.getRotateAnimation());
        }else{
        	getActivity().setProgressBarIndeterminateVisibility(true);
        }
    }

    @Override
    public void loadingSucceeded() {
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setAnimation(null);
            mRefresh.setVisibility(View.GONE);
        }else{
        	getActivity().setProgressBarIndeterminateVisibility(false);
        }
    }

    private boolean isTablet() {
        return ((AwfulActivity) getActivity()).isTablet();
    }

	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		updateColors(mPrefs);
	}
	private class PMIndexCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {
        public PMIndexCallback(Handler handler) {
			super(handler);
		}

		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			Log.i(TAG,"Load PM Cursor.");
            return new CursorLoader(getActivity(), 
            						AwfulMessage.CONTENT_URI, 
            						AwfulProvider.PMProjection, 
            						null, 
            						null,
            						AwfulMessage.ID+" DESC");
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"PM load finished, populating: "+aData.getCount());
        	mCursorAdapter.swapCursor(aData);
        }
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        	mCursorAdapter.swapCursor(null);
        }
        
        @Override
        public void onChange (boolean selfChange){
        	Log.i(TAG,"PM Data update.");
        	if(getActivity() != null){
        		getActivity().getSupportLoaderManager().restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, this);
        	}
        }
    }
}
