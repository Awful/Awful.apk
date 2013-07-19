/********************************************************************************
 * Copyright (c) 2011, Dan Bjorge
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
 * THIS SOFTWARE IS PROVIDED BY DAN BJORGE ''AS IS'' AND ANY
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

package com.ferg.awfulapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;

import org.apache.commons.lang3.text.WordUtils;

import java.io.File;
import java.util.LinkedList;

/**
 * Simple, purely xml driven preferences. Access using
 * {@link PreferenceManager#getDefaultSharedPreferences(android.content.Context)}
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, ServiceConnection {
    protected static String TAG = "SettingsActivity";
	private static final int DIALOG_ABOUT = 1;
	private static final int SETTINGS_FILE = 2;
	private Preference mAboutPreference;
	private Preference mFeaturesPreference;
	private Preference mThreadPreference;
	private Preference mImagePreference;
	private Preference mInfoPreference;
	private Preference mColorsPreference;
	private Preference mFontSizePreference;
	private Preference mUsernamePreference;
	private Preference mExportPreference;
	private Preference mImportPreference;
	protected SettingsActivity mThis = this;
	private Dialog mFontSizeDialog;
	private Dialog mFeatureFetchDialog;
	private TextView mFontSizeText;
	
	private Handler fetchHandler= new Handler() {
        @Override
        public void handleMessage(Message aMsg) {

	        	AwfulSyncService.debugLogReceivedMessage(TAG, aMsg);
	        	if(aMsg.what == AwfulSyncService.MSG_ERROR || aMsg.what == AwfulSyncService.MSG_ERR_NOT_LOGGED_IN){
                     Toast.makeText(mThis, "An error occured", Toast.LENGTH_LONG).show();
	        	}else{
		            switch (aMsg.arg1) {
		                case AwfulSyncService.Status.WORKING:
		                	break;
		                case AwfulSyncService.Status.OKAY:
		                	mFeatureFetchDialog.dismiss();
		            		mThis.updateFeatures();
		                    break;
		                case AwfulSyncService.Status.ERROR:
		                     Toast.makeText(mThis, "An error occured", Toast.LENGTH_LONG).show();
		                    break;
		            };
	        	
        	}
        }
	};
	private SharedPreferences mPrefs;
	private ActivityConfigurator mConf;
	
    private Messenger mService = null;
    private LinkedList<Message> mMessageQueue = new LinkedList<Message>();

	// ---------------------------------------------- //
	// ---------------- LIFECYCLE ------------------- //
	// ---------------------------------------------- //
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		mConf = new ActivityConfigurator(this);
		mConf.onCreate();
		
		addPreferencesFromResource(R.xml.settings);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		this.bindService(new Intent(this, AwfulSyncService.class), this, BIND_AUTO_CREATE);
		
		findPreference("inline_youtube").setEnabled(Constants.isICS());
		findPreference("enable_hardware_acceleration").setEnabled(Constants.isHoneycomb());	
		findPreference("enable_hardware_acceleration").setDefaultValue(Constants.isJellybean());		
		boolean tab = Constants.canBeWidescreen(this);
		findPreference("page_layout").setEnabled(tab);
		if(!tab){
			findPreference("page_layout").setSummary(getString(R.string.page_layout_summary_disabled));
		}
		
		mAboutPreference = getPreferenceScreen().findPreference("about");
		mAboutPreference.setOnPreferenceClickListener(onAboutListener);
		mThreadPreference = getPreferenceScreen().findPreference("open_thread");
		mThreadPreference.setOnPreferenceClickListener(onThreadListener);
		mColorsPreference = getPreferenceScreen().findPreference("colors");
		mColorsPreference.setOnPreferenceClickListener(onColorsListener);
		mImagePreference = getPreferenceScreen().findPreference("image_settings");
		mImagePreference.setOnPreferenceClickListener(onImagesListener);
		mInfoPreference = getPreferenceScreen().findPreference("threadinfo");
		mInfoPreference.setOnPreferenceClickListener(onInfoListener);
		mFontSizePreference = getPreferenceScreen().findPreference("default_post_font_size_dip");
		mFontSizePreference.setOnPreferenceClickListener(onFontSizeListener);
		mExportPreference = getPreferenceScreen().findPreference("export_settings");
		mExportPreference.setOnPreferenceClickListener(onExportListener);
		mImportPreference = getPreferenceScreen().findPreference("import_settings");
		mImportPreference.setOnPreferenceClickListener(onImportListener);

		mFeaturesPreference = getPreferenceScreen().findPreference("account_features");
		mFeaturesPreference.setOnPreferenceClickListener(onFeaturesListener);
		this.updateFeatures();
		
		mUsernamePreference = getPreferenceScreen().findPreference("username");
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mConf.onStart();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mConf.onResume();
        bindService(new Intent(this, AwfulSyncService.class), this, BIND_AUTO_CREATE);
		
		setSummaries();
		
		mPrefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mConf.onPause();
		
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mConf.onStop();
        unbindService(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mConf.onDestroy();
	}
	
	
	// ---------------------------------------------- //
	// ------------- OTHER LISTENERS ---------------- //
	// ---------------------------------------------- //
	
	@Override
	protected Dialog onCreateDialog(int dialogId) {
		switch(dialogId) {
		case DIALOG_ABOUT:
			CharSequence app_version = getText(R.string.app_name);
			try {
				app_version = app_version + " " +
					getPackageManager().getPackageInfo(getPackageName(), 0)
					.versionName;
			} catch (NameNotFoundException e) {
				// rather unlikely, just show app_name without version
			}

			return new AlertDialog.Builder(this)
				.setTitle(app_version)
				.setMessage(R.string.about_message)
				.setNeutralButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}})
				.create();
		default:
			return super.onCreateDialog(dialogId);
		}
	}
	
	private OnPreferenceClickListener onAboutListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			showDialog(DIALOG_ABOUT);
			return true;
		}
	};
	
	private OnPreferenceClickListener onExportListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			AwfulPreferences.getInstance().exportSettings();
			Toast.makeText(mThis, "Settings exported", Toast.LENGTH_LONG).show();
			return true;
		}
	};	
	
	private OnPreferenceClickListener onImportListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
    	    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    	    intent.setType("file/*");
    	    startActivityForResult(Intent.createChooser(intent,
                    "Select Settings File"), SETTINGS_FILE);
			return true;
		}
	};	
	
	private OnPreferenceClickListener onThreadListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			Intent openThread = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FUNCTION_THREAD+'?'+Constants.PARAM_THREAD_ID+"="+Constants.AWFUL_THREAD_ID));
			startActivity(openThread);
			return true;
		}
	};

	private OnPreferenceClickListener onColorsListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			startActivity(new Intent().setClass(mThis, ThemeSettingsActivity.class));
			return true;
		}
	};
	
	private OnPreferenceClickListener onImagesListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			startActivity(new Intent().setClass(mThis, ImageSettingsActivity.class));
			return true;
		}
	};
	
	private OnPreferenceClickListener onInfoListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			startActivity(new Intent().setClass(mThis, ThreadInfoSettingsActivity.class));
			return true;
		}
	};
	
	private OnPreferenceClickListener onFeaturesListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			//TODO: add something to refresh account features
			mFeatureFetchDialog = ProgressDialog.show(mThis, "Loading", "Fetching Account Features", true);
			mThis.sendMessage(new Messenger(fetchHandler), AwfulSyncService.MSG_FETCH_FEATURES, 0, 0, null);
			return true;
		}
	};
	
	private OnPreferenceClickListener onFontSizeListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			mFontSizeDialog = new Dialog(mThis);

			mFontSizeDialog.setContentView(R.layout.font_size);
			mFontSizeDialog.setTitle("Set Default Font Size");

			mFontSizeText = (TextView) mFontSizeDialog.findViewById(R.id.fontSizeText);
			SeekBar bar = (SeekBar) mFontSizeDialog.findViewById(R.id.fontSizeBar);
			Button click = (Button) mFontSizeDialog.findViewById(R.id.fontSizeButton);
			
			click.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					mFontSizeDialog.dismiss();
				}
			});
			
	        bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					Editor sizeEdit = mPrefs.edit();
					sizeEdit.putInt("default_post_font_size_dip", seekBar.getProgress()+10);
					sizeEdit.commit();
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					mFontSizeText.setText((progress+10)+ "  Get out");
			        mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (progress+10));
				}
			});
	        bar.setProgress(mPrefs.getInt("default_post_font_size_dip", Constants.DEFAULT_FONT_SIZE)-10);
	        mFontSizeText.setText((bar.getProgress()+10)+ "  Get out");
	        mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (bar.getProgress()+10));
	        mFontSizeDialog.show();
			return true;
		}
	};
	
	// All keys representing int values whose Summaries should be set to their values
	private static final String[] VALUE_SUMMARY_KEYS_INT = { 
		"default_post_font_size_dip",
		"post_per_page"
		};
	private static final int[] VALUE_SUMMARY_DEFAULTS_INT = { 
		Constants.DEFAULT_FONT_SIZE,
		Constants.ITEMS_PER_PAGE
		};
	
	private static final String[] VALUE_SUMMARY_KEYS_LIST = {
		"orientation"
	};
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		setSummaries(); 
	}
	
	private void setSummaries() {
		for(int x=0;x<VALUE_SUMMARY_KEYS_INT.length;x++) {
			findPreference(VALUE_SUMMARY_KEYS_INT[x]).setSummary(String.valueOf(mPrefs.getInt(VALUE_SUMMARY_KEYS_INT[x], VALUE_SUMMARY_DEFAULTS_INT[x])));
		}
		for(String key : VALUE_SUMMARY_KEYS_LIST) {
			ListPreference p = (ListPreference) findPreference(key);
			p.setSummary(p.getEntry());
		}
		mUsernamePreference.setSummary(mPrefs.getString("username", "Not Set"));
		mColorsPreference.setSummary(WordUtils.capitalize(mPrefs.getString("themes", "Default"))+" Theme");
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(TAG, "Service Connected!");
        mService = new Messenger(service);
        for(Message msg : mMessageQueue){
        	try {
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
        }
        mMessageQueue.clear();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "Service Disconnected!");
		mService = null;
	}
	
	public void sendMessage(Messenger callback, int messageType, int id, int arg1, Object obj){
		try {
            Message msg = Message.obtain(null, messageType, id, arg1);
            msg.replyTo = callback;
            msg.obj = obj;
    		if(mService != null){
    			mService.send(msg);
    		}else{
    			mMessageQueue.add(msg);
    		}
        } catch (RemoteException e) {
            e.printStackTrace();
        }
	}

	public void updateFeatures(){
		String platinum = (mPrefs.getBoolean("has_platinum", false)) ? "Yes" : "No";
		String archives = (mPrefs.getBoolean("has_archives", false)) ? "Yes" : "No";
		String noAds = (mPrefs.getBoolean("has_no_ads", false)) ? "Yes" : "No";
		mFeaturesPreference.setSummary("Platinum: "+platinum+" | Archives: "+archives+" | No Ads: "+noAds);
	}
	
   @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SETTINGS_FILE) {
    			Toast.makeText(mThis, "importing settings", Toast.LENGTH_SHORT).show();
                Uri selectedSetting = data.getData();
                String path = getFilePath(selectedSetting);
                if(path != null){
	                File settingsfile = new File(path);
	                AwfulPreferences.getInstance(this).importSettings(settingsfile);
	                this.finish();
                }
            }
        }

    }
   
   public String getFilePath(Uri uri) {
	   try{
	       String[] projection = { MediaStore.Images.Media.DATA };
	       Cursor cursor = this.getContentResolver().query(uri, projection, null, null, null);
	       if(cursor!=null)
	       {
	           //HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
	           //THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
	           int column_index = cursor
	           .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	           cursor.moveToFirst();
	           return cursor.getString(column_index);
	       }
	       else{
			   Toast.makeText(this, "Your file explorer sent incompatible data, please try a different way", Toast.LENGTH_LONG).show();
	    	   return null;
	       }
	   }catch(NullPointerException e){
		   Toast.makeText(this, "Your file explorer sent incompatible data, please try a different way", Toast.LENGTH_LONG).show();
		   e.printStackTrace();
		   return null;
	   }
   }
}
