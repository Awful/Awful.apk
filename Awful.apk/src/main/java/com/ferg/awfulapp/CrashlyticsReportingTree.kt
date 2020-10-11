package com.ferg.awfulapp

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber


class CrashlyticsReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()

        crashlytics.log("$priority/$tag:$message")

        if (throwable != null) {
            crashlytics.log(throwable.localizedMessage)
        }
    }
}