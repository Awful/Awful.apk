package com.ferg.awfulapp.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

import com.ferg.awfulapp.constants.Constants;

public class PreferencesBackupAgent extends BackupAgentHelper {
	//if changing package name, MAKE SURE TO GET THIS TOO.
	//com.example.appname_preferences
    private static final String DEFAULT_PREFERENCES = "com.ferg.awfulapp_preferences";
    private static final String BACKUP_KEY          = "preferences_backup";

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = 
            new SharedPreferencesBackupHelper(this, DEFAULT_PREFERENCES, Constants.COOKIE_PREFERENCE);
        addHelper(BACKUP_KEY, helper);
    }
}
