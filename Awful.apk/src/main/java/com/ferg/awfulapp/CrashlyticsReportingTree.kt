package com.ferg.awfulapp

import com.crashlytics.android.Crashlytics
import timber.log.Timber


class CrashlyticsReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Crashlytics.log(priority, tag, message)

        t?.let {
            Crashlytics.logException(it)
        }
    }
}