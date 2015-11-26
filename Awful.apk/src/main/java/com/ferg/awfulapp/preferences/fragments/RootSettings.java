package com.ferg.awfulapp.preferences.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.Preference;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.ferg.awfulapp.ForumsIndexActivity;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.SettingsActivity;
import com.ferg.awfulapp.util.AwfulUtils;

/**
 * Created by baka kaba on 04/05/2015.
 *
 * The SettingsFragment that forms the root of the Settings hierarchy
 */
public class RootSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.rootsettings;

        SUBMENU_OPENING_KEYS = new String[] {
                "theme",
                "thread",
                "posts",
                "images",
                "misc",
                "account"
        };

        prefClickListeners.put(new AboutListener(), new String[] {
                "about"
        });
        prefClickListeners.put(new ThreadListener(), new String[] {
                "open_thread"
        });
        prefClickListeners.put(new ExportListener(), new String[]{
                "export_settings"
        });
        prefClickListeners.put(new ImportListener(), new String[] {
                "import_settings"
        });
    }


    /** Listener for the 'About...' option */
    private class AboutListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            getActivity().showDialog(SettingsActivity.DIALOG_ABOUT);
            return true;
        }
    }


    /** Listener for 'Go to the Awful thread' option */
    private class ThreadListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Activity activity = getActivity();
            Intent openThread = new Intent().setClass(activity, ForumsIndexActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Constants.THREAD_ID, Constants.AWFUL_THREAD_ID)
                    .putExtra(Constants.THREAD_PAGE, 1)
                    .putExtra(Constants.FORUM_ID, Constants.USERCP_ID)
                    .putExtra(Constants.FORUM_PAGE, 1);
            activity.finish();
            startActivity(openThread);
            return true;
        }
    }

    /** Listener for the 'Export settings' option */
    private class ExportListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if(AwfulUtils.isMarshmallow()){
                int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.AWFUL_PERMISSION_WRITE_EXTERNAL_STORAGE);
                }else{
                    mPrefs.exportSettings();
                }
            }else{
                mPrefs.exportSettings();
            }
            return true;
        }
    }

    /** Listener for the 'Import settings' option */
    private class ImportListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("file/*");
            getActivity().startActivityForResult(Intent.createChooser(intent,
                    "Select Settings File"), SettingsActivity.SETTINGS_FILE);
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Constants.AWFUL_PERMISSION_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    AwfulPreferences.getInstance().exportSettings();
                } else {
                    Toast.makeText(getActivity(), R.string.no_file_permission_settings_export, Toast.LENGTH_LONG).show();
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
