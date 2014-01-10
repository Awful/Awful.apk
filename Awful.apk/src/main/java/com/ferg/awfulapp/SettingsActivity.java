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
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.FeatureRequest;
import com.ferg.awfulapp.util.AwfulUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple, purely xml driven preferences. Access using
 * {@link PreferenceManager#getDefaultSharedPreferences(android.content.Context)}
 */
public class SettingsActivity extends PreferenceActivity implements AwfulPreferences.AwfulPreferenceUpdate {
    protected static String TAG = "SettingsActivity";
	private static final int DIALOG_ABOUT = 1;
	private static final int SETTINGS_FILE = 2;
	protected SettingsActivity mThis = this;
	private Dialog mFontSizeDialog;
	private Dialog mFeatureFetchDialog;
	private Dialog mP2RDistanceDialog;
	private TextView mFontSizeText;
	private TextView mP2RDistanceText;
	private AwfulPreferences mPrefs;
	private ActivityConfigurator mConf;

    private boolean oldMode = true; // old-style preferences (all we got right now)
    private boolean hierarchyLoaded = false;  // to ensure preferences are ready for manipulatin'

	// ---------------------------------------------- //
	// ---------------- LIFECYCLE ------------------- //
	// ---------------------------------------------- //
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mConf = new ActivityConfigurator(this);
		mConf.onCreate();

		mPrefs = AwfulPreferences.getInstance(this,this);
        if (oldMode) {
            // This setup allows older Android versions to build a PreferenceScreen-based,
            // multi-page layout using the same XML files as a PreferenceFragment system

            // Build the PreferenceScreen hierarchy
            addPreferencesFromResource(R.xml.settings);
            addSectionDivider(getString(R.string.settings_divider_customisation));
            addPreferencesFromResource(R.xml.threadinfosettings);
            addPreferencesFromResource(R.xml.postsettings);
            addPreferencesFromResource(R.xml.imagesettings);
            addPreferencesFromResource(R.xml.themesettings);
            addSectionDivider(getString(R.string.settings_divider_misc));
            addPreferencesFromResource(R.xml.miscsettings);
            addSectionDivider(getString(R.string.settings_divider_account));
            addPreferencesFromResource(R.xml.accountsettings);
            addSectionDivider(getString(R.string.settings_divider_backup));
            addPreferencesFromResource(R.xml.backupsettings);

            // Since the full Preference hierarchy is being built in this activity, we can
            // initialise and handle everything in here. Fragments could contain the
            // individual method groups as appropriate

            // Initialise whatever needs initialising from each file
            initRootSettings();    setRootListeners();
            initThreadSettings();  setThreadListeners();
            initPostSettings();    setPostListeners();
            initImageSettings();   setImageListeners();
            initThemeSettings();   setThemeListeners();
            initMiscSettings();    setMiscListeners();
            initAccountSettings(); setAccountListeners();
            initBackupSettings();  setBackupListeners();
            hierarchyLoaded = true;
            setSummaries();
        }
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
		setSummaries();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mConf.onPause();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mConf.onStop();
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
            // Build the text for the About dialog
            Resources res = getResources();
            String aboutText = getString(R.string.about_contributors_title) + "\n\n";
            aboutText += StringUtils.join(res.getStringArray(R.array.about_contributors_array), '\n');
            aboutText += "\n\n" + getString(R.string.about_libraries_title) + "\n\n";
            aboutText += StringUtils.join(res.getStringArray(R.array.about_libraries_array), '\n');

			return new AlertDialog.Builder(this)
				.setTitle(app_version)
				.setMessage(aboutText)
				.setNeutralButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}})
				.create();
		default:
			return super.onCreateDialog(dialogId);
		}
	}

	// All keys representing int values whose Summaries should be set to their values
//	private static final String[] VALUE_SUMMARY_KEYS_INT = { 
//		"default_post_font_size_dip",
//		"post_per_page"
//		};
//	private static final int[] VALUE_SUMMARY_DEFAULTS_INT = { 
//		Constants.DEFAULT_FONT_SIZE,
//		Constants.ITEMS_PER_PAGE
//		};
	
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

    // This is called before the XML is loaded on a fresh install, so setSummaries checks the
    // hierarchy is loaded before trying to do anything (otherwise there's NPEs)
	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		setSummaries();
	}


    /*
    My stuff
     */

    /** Helper method to throw in a section divider preference while inflating XMLs */
    private void addSectionDivider(CharSequence title) {
        PreferenceScreen screen = (PreferenceScreen) findPreference("root_pref_screen");
        PreferenceCategory divider = new PreferenceCategory(mThis);
        divider.setTitle(title);
        try {
            screen.addPreference(divider);
        }
        catch (NullPointerException e) {
            Log.w(TAG, "Can't find root preference screen");
        }
    }

    private void setSummaries() {
        if (oldMode && hierarchyLoaded) {
            setRootSummaries();
            setMiscSummaries();
            setPostSummaries();
            setAccountSummaries();
            setImageSummaries();
            setThreadSummaries();
            setThemeSummaries();
            setBackupSummaries();
        }
    }


    /*
        SETTINGS.XML
     */

    /** Initialise preferences on the root settings page */
    private void initRootSettings() {

    }

    private void setRootSummaries() {

    }

    private void setRootListeners() {
        Preference tempPref;
        tempPref = getPreferenceScreen().findPreference("about");
        tempPref.setOnPreferenceClickListener(onAboutListener);
        tempPref = getPreferenceScreen().findPreference("open_thread");
        tempPref.setOnPreferenceClickListener(onThreadListener);
    }

    /* Associated methods */

    /** Listener for the 'About...' option */
    private OnPreferenceClickListener onAboutListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            showDialog(DIALOG_ABOUT);
            return true;
        }
    };

    /** Listener for 'Go to the Awful thread' option */
    private OnPreferenceClickListener onThreadListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent openThread = new Intent().setClass(mThis, ForumsIndexActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Constants.THREAD_ID, Constants.AWFUL_THREAD_ID)
                    .putExtra(Constants.THREAD_PAGE, 1)
                    .putExtra(Constants.FORUM_ID, Constants.USERCP_ID)
                    .putExtra(Constants.FORUM_PAGE, 1);
            mThis.finish();
            startActivity(openThread);
            return true;
        }
    };


    /*
        BACKUPSETTINGS.XML
     */

    /** Initialise preferences on the root settings page */
    private void initBackupSettings() {

    }

    private void setBackupSummaries() {

    }

    private void setBackupListeners() {
        Preference tempPref;
        tempPref = getPreferenceScreen().findPreference("export_settings");
        tempPref.setOnPreferenceClickListener(onExportListener);
        tempPref = getPreferenceScreen().findPreference("import_settings");
        tempPref.setOnPreferenceClickListener(onImportListener);
    }

    /* Associated methods */

    /** Listener for the 'Export settings' option */
    private OnPreferenceClickListener onExportListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            AwfulPreferences.getInstance().exportSettings();
            Toast.makeText(mThis, "Settings exported", Toast.LENGTH_LONG).show();
            return true;
        }
    };

    /** Listener for the 'Import settings' option */
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



    /*
        MISCSETTINGS.XML
     */

    /** Initialise preferences on Misc Settings page */
    private void initMiscSettings() {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference("enable_hardware_acceleration");
        if (pref != null) {
            pref.setEnabled(AwfulUtils.isHoneycomb());
            // Not for you
            if (!AwfulUtils.isJellybean()) pref.setChecked(false);
        }
        pref = (CheckBoxPreference) findPreference("disable_gifs2");
        if (pref != null) {
            pref.setEnabled(AwfulUtils.isHoneycomb());
            // Not for you
            if (!AwfulUtils.isHoneycomb()) pref.setChecked(false);
        }

        findPreference("immersion_mode").setEnabled(AwfulUtils.isKitKat());
        boolean tab = AwfulUtils.canBeWidescreen(this);
        findPreference("page_layout").setEnabled(tab);
        if(!tab){
            findPreference("page_layout").setSummary(getString(R.string.page_layout_summary_disabled));
        }
    }

    private void setMiscSummaries() {
        final String[] VALUE_SUMMARY_KEYS_LIST = { "orientation" };
        final String[] VERSION_DEPENDENT_KEYS_LIST = { "disable_gifs2",
                                                       "enable_hardware_acceleration",
                                                       "immersion_mode"};
        // set summaries to their selected entries
        for (String key : VALUE_SUMMARY_KEYS_LIST) {
            ListPreference p = (ListPreference) findPreference(key);
            p.setSummary(p.getEntry());
        }
        // set summaries for unavailable options
        for (String key : VERSION_DEPENDENT_KEYS_LIST) {
            Preference p = (Preference) findPreference(key);
            if (!p.isEnabled()){
                p.setSummary(getString(R.string.not_available_on_your_version));
            }
        }
    }

    private void setMiscListeners() {
        Preference tempPref;
        tempPref = getPreferenceScreen().findPreference("pull_to_refresh_distance");
        tempPref.setOnPreferenceClickListener(onP2RDistanceListener);
    }

    /* Associated methods */

    /** Listener for the 'Pull-to-refresh distance' option */
    private OnPreferenceClickListener onP2RDistanceListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            mP2RDistanceDialog = new Dialog(mThis);

            mP2RDistanceDialog.setContentView(R.layout.p2rdistance);
            mP2RDistanceDialog.setTitle("Set Pull-to-refresh distance");

            mP2RDistanceText = (TextView) mP2RDistanceDialog.findViewById(R.id.p2rdistanceText);
            SeekBar bar = (SeekBar) mP2RDistanceDialog.findViewById(R.id.p2rdistanceBar);
            Button click = (Button) mP2RDistanceDialog.findViewById(R.id.p2rdistanceButton);

            click.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mP2RDistanceDialog.dismiss();
                }
            });

            bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    float distanceFloat = seekBar.getProgress();
                    mPrefs.setFloatPreference("pull_to_refresh_distance", (distanceFloat/100));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mP2RDistanceText.setText(progress+ "%"+((progress<20||progress>75)?" (not recommended)":""));
                }
            });
            bar.setProgress(Math.round(mPrefs.p2rDistance*100));
            mP2RDistanceText.setText(bar.getProgress()+ "%"+((bar.getProgress()<20||bar.getProgress()>75)?" (not recommended)":""));
            mP2RDistanceDialog.show();
            return true;
        }
    };


    /*
        POSTSETTINGS.XML
     */

    /** Initialise preferences on Post Settings page */
    private void initPostSettings() {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference("inline_youtube");
        if (pref != null) {
            pref.setEnabled(AwfulUtils.isICS());
            // Not for you
            if (!AwfulUtils.isICS()) pref.setChecked(false);
        }
    }

    private void setPostSummaries() {
        final String[] VERSION_DEPENDENT_KEYS_LIST = { "inline_youtube" };

        findPreference("default_post_font_size_dip").setSummary(String.valueOf(mPrefs.postFontSizeDip));
        findPreference("post_per_page").setSummary(String.valueOf(mPrefs.postPerPage));
        // set summaries for unavailable options
        for (String key : VERSION_DEPENDENT_KEYS_LIST) {
            Preference p = (Preference) findPreference(key);
            if (!p.isEnabled()){
                p.setSummary(getString(R.string.not_available_on_your_version));
            }
        }
    }

    private void setPostListeners() {
        Preference tempPref;
        tempPref = getPreferenceScreen().findPreference("default_post_font_size_dip");
        tempPref.setOnPreferenceClickListener(onFontSizeListener);
    }

    /* Associated methods */

    /** Listener for the 'Default post font size' option */
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
                    mFontSizeDialog.dismiss();
                }
            });

            bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mPrefs.setIntegerPreference("default_post_font_size_dip", seekBar.getProgress()+10);
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
            bar.setProgress(mPrefs.postFontSizeDip-10);
            mFontSizeText.setText((bar.getProgress()+10)+ "  Get out");
            mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (bar.getProgress()+10));
            mFontSizeDialog.show();
            return true;
        }
    };


    /*
        ACCOUNTSETTINGS.XML
     */

    /** Initialise preferences on Account Settings page */
    private void initAccountSettings() {

    }

    private void setAccountSummaries() {
        Preference tempPref;
        tempPref = getPreferenceScreen().findPreference("username");
        tempPref.setSummary(mPrefs.username);

        //Set summary for the 'Refresh account options' option
        String platinum = (mPrefs.hasPlatinum) ? "Yes" : "No";
        String archives = (mPrefs.hasArchives) ? "Yes" : "No";
        String noAds = (mPrefs.hasNoAds) ? "Yes" : "No";
        tempPref = getPreferenceScreen().findPreference("account_features");
        tempPref.setSummary("Platinum: "+platinum+"\nArchives: "+archives+"\nNo Ads: "+noAds);
    }

    private void setAccountListeners() {
        Preference tempPref;
        tempPref = getPreferenceScreen().findPreference("account_features");
        tempPref.setOnPreferenceClickListener(onFeaturesListener);
        setAccountSummaries();
    }

    /* Associated methods */

    /** Listener for the 'Refresh account features' option */
    private OnPreferenceClickListener onFeaturesListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            mFeatureFetchDialog = ProgressDialog.show(mThis, "Loading", "Fetching Account Features", true);
            ((AwfulApplication)getApplication()).queueRequest(new FeatureRequest(SettingsActivity.this).build(null, new AwfulRequest.AwfulResultCallback<Void>() {
                @Override
                public void success(Void result) {
                    mFeatureFetchDialog.dismiss();
                    setAccountSummaries();
                }

                @Override
                public void failure(VolleyError error) {
                    mFeatureFetchDialog.dismiss();
                    Toast.makeText(mThis, "An error occured", Toast.LENGTH_LONG).show();
                }
            }));
            return true;
        }
    };


    /*
        IMAGESETTINGS.XML
     */

    /** Initialise preferences on Image Settings page */
    private void initImageSettings() {

    }

    private void setImageSummaries() {
        final String[] VALUE_SUMMARY_KEYS_LIST = { "imgur_thumbnails" };

        // set summaries to their selected entries
        for (String key : VALUE_SUMMARY_KEYS_LIST) {
            ListPreference p = (ListPreference) findPreference(key);
            p.setSummary(p.getEntry());
        }
    }

    private void setImageListeners() {

    }


    /*
        THREADINFOSETTINGS.XML
     */

    /** Initialise preferences on Thread Settings page */
    private void initThreadSettings() {

    }

    private void setThreadSummaries() {

    }

    private void setThreadListeners() {

    }


    /*
        THEMESETTINGS.XML
     */

    /** Initialise preferences on Theme Settings page */
    private void initThemeSettings() {
        Pattern fontFilename = Pattern.compile("fonts/(.*).ttf.mp3", Pattern.CASE_INSENSITIVE);
        String lastTheme = mPrefs.theme;

        ListPreference themePref = (ListPreference) findPreference("theme");
        ListPreference layoutPref = (ListPreference) findPreference("layouts");

        File[] SDcard = Environment.getExternalStorageDirectory().listFiles();

        for (File folder: SDcard){
            if("awful".equals(folder.getName()) && folder.canRead()){
                File[] files = folder.listFiles();
                ArrayList<CharSequence> themes = new ArrayList<CharSequence>();
                ArrayList<CharSequence> themeValues = new ArrayList<CharSequence>();
                ArrayList<CharSequence> layouts = new ArrayList<CharSequence>();
                ArrayList<CharSequence> layoutValues = new ArrayList<CharSequence>();
                themes.addAll(Arrays.asList(themePref.getEntries()));
                themeValues.addAll(Arrays.asList(themePref.getEntryValues()));
                layouts.addAll(Arrays.asList(layoutPref.getEntries()));
                layoutValues.addAll(Arrays.asList(layoutPref.getEntryValues()));
                for(File folderFile: files){
                    if(folderFile.canRead() && folderFile.getName() != null){
                        String[] fileName = folderFile.getName().split("\\.");
                        if("css".equals(fileName[fileName.length-1])){
                            if(StringUtils.countMatches(folderFile.getName(), ".")>1){
                                themes.add(fileName[0]+" ("+fileName[fileName.length-2]+")");
                            }else{
                                themes.add(fileName[0]);
                            }
                            themeValues.add(folderFile.getName());
                        }
                        if("mustache".equals(fileName[fileName.length-1])){
                            layouts.add(fileName[0]);
                            layoutValues.add(folderFile.getName());
                        }
                    }
                }
                layoutPref.setEntries(layouts.toArray(new CharSequence[layouts.size()]));
                layoutPref.setEntryValues(layoutValues.toArray(new CharSequence[layoutValues.size()]));

                themePref.setEntries(themes.toArray(new CharSequence[themes.size()]));
                themePref.setEntryValues(themeValues.toArray(new CharSequence[themeValues.size()]));
            }
        }

        ListPreference f = (ListPreference) findPreference("preferred_font");
        String[] fontList = ((AwfulApplication)getApplication()).getFontList();
        String[] fontNames = new String[fontList.length];
        for(int x=0; x<fontList.length;x++){
            Matcher fontName = fontFilename.matcher(fontList[x]);
            if(fontName.find()){
                fontNames[x] = fontName.group(1).replaceAll("_", " ");
            }else{//if the regex fails, try our best to clean up the filename.
                fontNames[x] = fontList[x].replaceAll(".ttf.mp3", "").replaceAll("fonts/", "").replaceAll("_", " ");
            }
        }
        f.setEntries(fontNames);
        f.setEntryValues(fontList);
    }

    private void setThemeSummaries() {
        final String[] VALUE_SUMMARY_KEYS_LIST = { "theme", "layouts", "preferred_font" };

        findPreference("colors").setSummary(WordUtils.capitalize(mPrefs.theme)+" Theme");
        // Used to get the parent screen to notice the new summary
        // kind of awkward, Theme Settings is the only header that displays one
        ((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
        
        // set summaries to their selected entries
        for(String valueSummaryKey : VALUE_SUMMARY_KEYS_LIST) {
            ListPreference pl = (ListPreference) findPreference(valueSummaryKey);
            pl.setSummary(pl.getEntry());
        }
    }

    private void setThemeListeners() {

    }

}
