package com.ferg.awfulapp.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper
import com.ferg.awfulapp.constants.Constants

class PreferencesBackupAgent : BackupAgentHelper() {
    companion object {
        //if changing package name, MAKE SURE TO GET THIS TOO.
        //com.example.appname_preferences
        private const val DEFAULT_PREFERENCES = "com.ferg.awfulapp_preferences"
        private const val BACKUP_KEY = "preferences_backup"
    }

    override fun onCreate() {
        val helper = SharedPreferencesBackupHelper(this, DEFAULT_PREFERENCES, Constants.COOKIE_PREFERENCE)
        addHelper(BACKUP_KEY, helper)
    }
}
