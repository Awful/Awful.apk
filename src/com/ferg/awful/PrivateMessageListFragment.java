package com.ferg.awful;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulServiceConnection.GenericListAdapter;
import com.ferg.awful.thread.AwfulMessage;

import android.app.ActionBar;
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
            setActionBar();
        } else {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();
            mHome          = (ImageButton) actionbar.findViewById(R.id.home);
            mNewPM          = (ImageButton) actionbar.findViewById(R.id.new_pm);
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
            mTitle.setText(getString(R.string.private_message));
            mRefresh       = (ImageButton) actionbar.findViewById(R.id.refresh);
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


        if (!isHoneycomb()) {
            mHome.setOnClickListener(onButtonClick);
            mNewPM.setOnClickListener(onButtonClick);
            mRefresh.setOnClickListener(onButtonClick);
        }
        
        updateColors();

        mPMList.setOnItemClickListener(onPMSelected);
        

        adapt = ((AwfulActivity) getActivity()).getServiceConnection().createGenericAdapter(Constants.PRIVATE_MESSAGE, Constants.PRIVATE_MESSAGE_THREAD, this);
        mPMList.setAdapter(adapt);
    }
    
    private void updateColors(){
    	if(mPMList != null){
    		mPMList.setBackgroundColor(mPrefs.postBackgroundColor);
            mPMList.setCacheColorHint(mPrefs.postBackgroundColor);
    	}
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
        	adapt.fetchPrivateMessages();
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
                    adapt.fetchPrivateMessages();
                    break;
            }
        }
    };
    
    private AdapterView.OnItemClickListener onPMSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulMessage message = (AwfulMessage) adapt.getItem(aPosition);
            if(getActivity() instanceof PrivateMessageActivity){
                ((PrivateMessageActivity) getActivity()).showMessage(null, message.getID());
            }else{
            	startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, message.getID()));
            }
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
        if (!isHoneycomb()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setImageResource(R.drawable.ic_menu_refresh);
            mRefresh.startAnimation(adapt.getRotateAnimation());
        }else{
        	getActivity().setProgressBarIndeterminateVisibility(true);
        }
    }

    @Override
    public void loadingSucceeded() {
        if (!isHoneycomb()) {
            mRefresh.setAnimation(null);
            mRefresh.setVisibility(View.GONE);
        }else{
        	getActivity().setProgressBarIndeterminateVisibility(false);
        }
    }
	
	private boolean isHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

	@Override
	public void onServiceConnected() {
		adapt.fetchPrivateMessages();
	}

	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		updateColors();
	}

}
