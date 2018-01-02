package com.ferg.awfulapp

import com.crashlytics.android.Crashlytics
import timber.log.Timber

/**
 * Created by joseph on 1/2/2018.
 */


class CrashlyticsReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Crashlytics.log(priority, tag, message)

        t?.let {
            Crashlytics.logException(it)
        }
    }
}