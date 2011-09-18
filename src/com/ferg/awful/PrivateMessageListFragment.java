package com.ferg.awful;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulServiceConnection.GenericListAdapter;
import com.ferg.awful.thread.AwfulMessage;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
    private GenericListAdapter adapt;
    private AwfulPreferences mPrefs;
    private ImageButton mRefresh;
    
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

        if (isHoneycomb()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar_blank)).inflate();
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
        } else {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();
            mHome          = (ImageButton) actionbar.findViewById(R.id.home);
            mNewPM          = (ImageButton) actionbar.findViewById(R.id.new_pm);
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
            mRefresh       = (ImageButton) actionbar.findViewById(R.id.refresh);
        }
        
        return result;
    }
    
    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setRetainInstance(true);

        mTitle.setText(getString(R.string.private_message));

        if (!isHoneycomb()) {
            mHome.setOnClickListener(onButtonClick);
            mNewPM.setOnClickListener(onButtonClick);
            mRefresh.setOnClickListener(onButtonClick);
        }

        mPMList.setOnItemClickListener(onPMSelected);
        mPMList.setBackgroundColor(mPrefs.postBackgroundColor);
        mPMList.setCacheColorHint(mPrefs.postBackgroundColor);

        adapt = ((AwfulActivity) getActivity()).getServiceConnection().createGenericAdapter(Constants.PRIVATE_MESSAGE, Constants.PRIVATE_MESSAGE_THREAD, this);
        mPMList.setAdapter(adapt);
    }
    
    @Override
    public void onResume() {
        super.onResume();
		adapt.fetchPrivateMessages();
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        if(mPMList != null){
        	mPMList.setAdapter(null);
        }
        mPrefs.unRegisterListener();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() == 0){
            inflater.inflate(R.menu.forum_index_options, menu);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
                return true;
            case R.id.logout:
                new LogOutDialog(getActivity()).show();
                break;
            case R.id.refresh:
                adapt.fetchPrivateMessages();
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
                    adapt.fetchPrivateMessages();
                    break;
            }
        }
    };
    
    private AdapterView.OnItemClickListener onPMSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulMessage message = (AwfulMessage) adapt.getItem(aPosition);
            
            Intent viewPM = new Intent().setClass(getActivity(), MessageDisplayActivity.class);
            viewPM.putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, message.getID());

            startActivity(viewPM);
        }
    };

	@Override
	public void dataUpdate(boolean pageChange, Bundle extras) {
	}

	@Override
    public void loadingFailed() {
        if (!isHoneycomb()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setAnimation(null);
            mRefresh.setImageResource(android.R.drawable.ic_dialog_alert);
            mRefresh.startAnimation(adapt.getBlinkingAnimation());
        }
        Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void loadingStarted() {
        if (!isHoneycomb()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setImageResource(R.drawable.ic_menu_refresh);
            mRefresh.startAnimation(adapt.getRotateAnimation());
        }
    }

    @Override
    public void loadingSucceeded() {
        if (!isHoneycomb()) {
            mRefresh.setAnimation(null);
            mRefresh.setVisibility(View.GONE);
        }
    }
	
	private boolean isHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

	@Override
	public void onServiceConnected() {
		adapt.fetchPrivateMessages();
	}

}
