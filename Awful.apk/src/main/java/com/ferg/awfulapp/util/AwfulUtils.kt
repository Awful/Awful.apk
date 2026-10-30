package com.ferg.awfulapp.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences.Companion.getInstance
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.thread.AwfulEmote
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.AwfulThread
import timber.log.Timber.Forest.e
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Created by matt on 9/11/13.
 * 
 * 
 * General utility functions and access to app resources.
 */
object AwfulUtils {
    @JvmStatic
    fun isAtLeast(code: Int): Boolean {
        return Build.VERSION.SDK_INT >= code
    }

    val isTiramisu33: Boolean
        get() = isAtLeast(Build.VERSION_CODES.TIRAMISU)

    fun isTablet(cont: Context, forceCheck: Boolean): Boolean {
        if (!forceCheck) {
            if (getInstance().pageLayout == "phone") {
                return false
            }
            if (getInstance().pageLayout == "tablet") {
                return true
            }
        }
        return getScreenSizeInInch(cont) >= Constants.TABLET_MIN_SIZE
    }

    fun isTablet(cont: Context): Boolean {
        return isTablet(cont, false)
    }

    private fun getScreenSizeInInch(cont: Context): Double {
        val display =
            (cont.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size = Point()
        display.getSize(size)
        val dm = DisplayMetrics()
        display.getMetrics(dm)
        val x = (size.x / dm.xdpi).toDouble().pow(2.0)
        val y = (size.y / dm.ydpi).toDouble().pow(2.0)
        return sqrt(x + y)
    }

    /**
     * Parse an int from the given string, falling back to the provided number in case of failure.
     * 
     * @param number   String containing the int to be parsed.
     * @param fallback Number to return if parsing fails.
     * @return Either the parsed number or the fallback.
     */
    fun safeParseInt(number: String, fallback: Int): Int {
        return try {
            number.toInt()
        } catch (nfe: NumberFormatException) {
            fallback
        }
    }

    /**
     * Parse an long from the given string, falling back to the provided number in case of failure.
     * 
     * @param number   String containing the long to be parsed.
     * @param fallback Number to return if parsing fails.
     * @return Either the parsed number or the fallback.
     */
    fun safeParseLong(number: String, fallback: Long): Long {
        return try {
            number.toLong()
        } catch (nfe: NumberFormatException) {
            fallback
        }
    }

    fun trimDbEntries(cr: ContentResolver) {
        var rowCount = 0
        for (uri in arrayOf(
            AwfulThread.CONTENT_URI,
            AwfulThread.CONTENT_URI_UCP,
            AwfulPost.CONTENT_URI,
            AwfulEmote.CONTENT_URI
        )) {
            rowCount += cr.delete(
                uri,
                DatabaseHelper.UPDATED_TIMESTAMP + " < datetime('now','-7 days')",
                null
            )
        }
        Log.i("AwfulTrimDB", "Trimming DB older than 7 days, culled: $rowCount")
    }

    fun contains(intArray: IntArray, value: Int): Boolean {
        for (cur in intArray) {
            if (cur == value) {
                return true
            }
        }
        return false
    }


    /**
     * Logs a failure (including Crashlytics, if available) and throws a RuntimeException on debug builds.
     * 
     * Use this for failure cases that should never happen, e.g. that point to some error in the app
     * logic that needs to be fixed. This way the end user sees little disruption (ideally something
     * just "doesn't work") but the developers are immediately alerted to the issue.
     * 
     * @param e the cause of the failure, which will be logged (with its stacktrace)
     */
    fun failSilently(e: Exception) {
        e(e)
        if (Constants.DEBUG) {
            throw RuntimeException(e)
        }
    }
}
