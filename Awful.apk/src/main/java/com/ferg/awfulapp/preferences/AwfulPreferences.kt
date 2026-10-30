/**
 * *****************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 * 
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *****************************************************************************
 */
package com.ferg.awfulapp.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import android.util.TypedValue
import androidx.core.content.edit
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.util.Date
import java.util.WeakHashMap

/**
 * This class acts as a convenience wrapper and simple cache for commonly used preference values.
 * Any changes made to primitive values will not carry over or affect the saved preferences.
 * 
 */
class AwfulPreferences private constructor(
    /**
     * Only use in emergencies, terrible hack
     * @returns a context
     */
    val context: Context
) : OnSharedPreferenceChangeListener {
    val sharedPrefs: SharedPreferences


    private val mResources: Resources = context.resources
    private val mCallback = WeakHashMap<AwfulPreferenceUpdate?, Any?>()

    //GENERAL STUFF
	@JvmField
    var username: String? = null
    var userAvatarUrl: String? = null

    /** this is only set when the user is on probation! See [com.ferg.awfulapp.util.AwfulError.checkPageErrors]  */
	@JvmField
    var userId: Int = 0
    @JvmField
    var hasPlatinum: Boolean = false
    @JvmField
    var hasArchives: Boolean = false
    @JvmField
    var hasNoAds: Boolean = false
    var sendUsernameInReport: Boolean = false
    var scaleFactor: Float = 0f
    var orientation: String? = null
    @JvmField
    var pageLayout: String? = null

    //THEME STUFF
	@JvmField
    var postFontSizeSp: Int = 0
    @JvmField
    var postFixedFontSizeSp: Int = 0
    var postFontSizePx: Int = 0
    var lockScrolling: Boolean = false
    @JvmField
    var theme: String? = null
    var launcherIcon: String? = null
    @JvmField
    var forceForumThemes: Boolean = false
    @JvmField
    var layout: String? = null
    @JvmField
    var preferredFont: String? = null
    var alternateBackground: Boolean = false
    @JvmField
    var amberDefaultPos: Boolean = false

    //THREAD STUFF
	@JvmField
    var postPerPage: Int = 0
    var imagesEnabled: Boolean = false
    var no3gImages: Boolean = false
    var avatarsEnabled: Boolean = false
    @JvmField
    var showSmilies: Boolean = false
    @JvmField
    var hideOldImages: Boolean = false
    @JvmField
    var highlightUserQuote: Boolean = false
    @JvmField
    var highlightUsername: Boolean = false
    @JvmField
    var highlightSelf: Boolean = false
    @JvmField
    var highlightOP: Boolean = false
    @JvmField
    var showAllSpoilers: Boolean = false
    @JvmField
    var imgurAccount: String? = null
    @JvmField
    var imgurAccountToken: String? = null
    var imgurRefreshToken: String? = null
    @JvmField
    var imgurTokenExpires: Long = 0
    @JvmField
    var imgurThumbnails: String? = null
    var upperNextArrow: Boolean = false
    @JvmField
    var disableGifs: Boolean = false
    @JvmField
    var hideOldPosts: Boolean = false
    @JvmField
    var disableTimgs: Boolean = false
    var volumeScroll: Boolean = false
    @JvmField
    var coloredBookmarks: Boolean = false
    @JvmField
    var hideSignatures: Boolean = false
    @JvmField
    var hideIgnoredPosts: Boolean = false
    @JvmField
    var noFAB: Boolean = false
    var alwaysOpenUrls: Boolean = false
    var blockedAvatarUrls: MutableSet<String?>? = null
    @JvmField
    var hiddenThreadIds: MutableSet<String?>? = null
    var showHiddenThreads: Boolean = false

    //FORUM STUFF
    var newThreadsFirstUCP: Boolean = false
    var newThreadsFirstForum: Boolean = false
    @JvmField
    var threadInfo_Rating: Boolean = false
    @JvmField
    var threadInfo_Tag: Boolean = false
    @JvmField
    var highlightYourThreads: Boolean = false
    var forumIndexShowSections: Boolean = false
    var forumIndexShowSubtitles: Boolean = false
    var forumIndexHideSubforums: Boolean = false

    //EXPERIMENTAL STUFF
    var inlineYoutube: Boolean = false
    @JvmField
    var inlineTweets: Boolean = false
    @JvmField
    var inlineBluesky: Boolean = false
    var inlineTiktoks: Boolean = false
    @JvmField
    var inlineVines: Boolean = false
    @JvmField
    var inlineWebm: Boolean = false
    @JvmField
    var autostartWebm: Boolean = false
    @JvmField
    var disablePullNext: Boolean = false
    var probationTime: Long = 0
    var probationIgnore: Boolean = false
    var showIgnoreWarning: Boolean = false

    /** some user-specific validation key that's required when sending a request to ignore a user  */
    var ignoreFormkey: String? = null
    @JvmField
    var markedUsers: MutableSet<String?>? = null
    @JvmField
    var p2rDistance: Float? = null
    var immersionMode: Boolean = false
    @JvmField
    var transformer: String? = null

    var postWarningAccepted: Boolean = false

    // APP VERSION STUFF
    var alertIDShown: Int = 0
    var lastVersionSeen: Int = 0

    private var currPrefVersion = 0

    private val longKeys: HashSet<String?>


    interface AwfulPreferenceUpdate {
        fun onPreferenceChange(preferences: AwfulPreferences, key: String?)
    }

    /**
     * Constructs a new AwfulPreferences object, registers preference change listener, and updates values.
     * @param context
     */
    init {
        // this is sort of redundant with what's going on in updateValues(), but best to be sure eh
        SettingsActivity.setDefaultsFromXml(context)
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context)
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
        updateValues()
        upgradePreferences()

        longKeys = HashSet()
        longKeys.add(mResources.getString(R.string.pref_key_probation_time))
        longKeys.add(mResources.getString(R.string.pref_key_imgur_token_expires))
    }


    fun unRegisterListener() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    fun registerCallback(client: AwfulPreferenceUpdate?) {
        mCallback[client] = null
    }

    fun unregisterCallback(client: AwfulPreferenceUpdate?) {
        mCallback.remove(client)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        updateValues()
        for (auc in mCallback.keys) {
            auc?.onPreferenceChange(this, key!!)
        }
    }

    private fun updateValues() {
        val res = context.resources
        scaleFactor = res.displayMetrics.density
        username = getPreference(StringPreference.USERNAME, "Username")
        userAvatarUrl = getPreference(StringPreference.USER_AVATAR_URL, null as String?)
        hasPlatinum = getPreference(BooleanPreference.HAS_PLATINUM, false)
        hasArchives = getPreference(BooleanPreference.HAS_ARCHIVES, false)
        hasNoAds = getPreference(BooleanPreference.HAS_NO_ADS, false)
        postFontSizeSp = getPreference(IntPreference.POST_FONT_SIZE_SP, Constants.DEFAULT_FONT_SIZE_SP)
        postFixedFontSizeSp =
            getPreference(IntPreference.POST_FIXED_FONT_SIZE_SP, Constants.DEFAULT_FIXED_FONT_SIZE_SP)
        postFontSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            postFontSizeSp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
        theme = getPreference(StringPreference.THEME, "default.css")
        launcherIcon = getPreference(StringPreference.LAUNCHER_ICON, "frog")
        layout = getPreference(StringPreference.LAYOUT, "default")
        imagesEnabled = getPreference(BooleanPreference.IMAGES_ENABLED, true)
        no3gImages = getPreference(BooleanPreference.NO_3G_IMAGES, false)
        avatarsEnabled = getPreference(BooleanPreference.AVATARS_ENABLED, true)
        hideOldImages = getPreference(BooleanPreference.HIDE_OLD_IMAGES, false)
        showSmilies = getPreference(BooleanPreference.SHOW_SMILIES, true)
        postPerPage = getPreference(IntPreference.POST_PER_PAGE, Constants.ITEMS_PER_PAGE)
        alternateBackground = getPreference(BooleanPreference.ALTERNATE_BACKGROUND, false)
        highlightUserQuote = getPreference(BooleanPreference.HIGHLIGHT_USER_QUOTE, true)
        highlightUsername = getPreference(BooleanPreference.HIGHLIGHT_USERNAME, true)
        highlightSelf = getPreference(BooleanPreference.HIGHLIGHT_SELF, true)
        highlightOP = getPreference(BooleanPreference.HIGHLIGHT_OP, true)
        inlineYoutube = getPreference(BooleanPreference.INLINE_YOUTUBE, true)
        inlineTweets = getPreference(BooleanPreference.INLINE_TWEETS, true)
        inlineBluesky = getPreference(BooleanPreference.INLINE_BLUESKY, true)
        inlineTiktoks = getPreference(BooleanPreference.INLINE_TIKTOKS, false)
        inlineVines = getPreference(BooleanPreference.INLINE_VINES, false)
        inlineWebm = getPreference(BooleanPreference.INLINE_WEBM, true)
        autostartWebm = getPreference(BooleanPreference.AUTOSTART_WEBM, false)
        showAllSpoilers = getPreference(BooleanPreference.SHOW_ALL_SPOILERS, false)
        threadInfo_Rating = getPreference(BooleanPreference.THREAD_INFO_RATING, true)
        threadInfo_Tag = getPreference(BooleanPreference.THREAD_INFO_TAG, true)
        highlightYourThreads = getPreference(BooleanPreference.HIGHLIGHT_YOUR_THREADS, true)
        imgurAccount = getPreference(StringPreference.IMGUR_ACCOUNT, null as String?)
        imgurAccountToken = getPreference(StringPreference.IMGUR_ACCOUNT_TOKEN, null as String?)
        imgurRefreshToken = getPreference(StringPreference.IMGUR_REFRESH_TOKEN, null as String?)
        imgurTokenExpires = getPreference(LongPreference.IMGUR_TOKEN_EXPIRES, 0L)
        imgurThumbnails = getPreference(StringPreference.IMGUR_THUMBNAILS, "d")
        newThreadsFirstUCP = getPreference(BooleanPreference.NEW_THREADS_FIRST_UCP, false)
        newThreadsFirstForum = getPreference(BooleanPreference.NEW_THREADS_FIRST_FORUM, false)
        preferredFont = getPreference(StringPreference.PREFERRED_FONT, "default")
        upperNextArrow = getPreference(BooleanPreference.UPPER_NEXT_ARROW, false)
        sendUsernameInReport = getPreference(BooleanPreference.SEND_USERNAME_IN_REPORT, true)
        disableGifs = getPreference(BooleanPreference.DISABLE_GIFS, true)
        hideOldPosts = getPreference(BooleanPreference.HIDE_OLD_POSTS, true)
        alwaysOpenUrls = getPreference(BooleanPreference.ALWAYS_OPEN_URLS, false)
        blockedAvatarUrls = getPreference(StringSetPreference.BLOCKED_AVATAR_URLS, mutableSetOf())
        hiddenThreadIds = getPreference(StringSetPreference.HIDDEN_THREAD_IDS, mutableSetOf())
        showHiddenThreads = getPreference(BooleanPreference.SHOW_HIDDEN_THREADS, true)
        lockScrolling = getPreference(BooleanPreference.LOCK_SCROLLING, false)
        disableTimgs = getPreference(BooleanPreference.DISABLE_TIMGS, false)
        currPrefVersion = getPreference(IntPreference.CURR_PREF_VERSION, 0)
        disablePullNext = getPreference(BooleanPreference.DISABLE_PULL_NEXT, false)
        alertIDShown = getPreference(IntPreference.ALERT_ID_SHOWN, 0)
        lastVersionSeen = getPreference(IntPreference.LAST_VERSION_SEEN, 0)
        volumeScroll = getPreference(BooleanPreference.VOLUME_SCROLL, false)
        forceForumThemes = getPreference(BooleanPreference.FORCE_FORUM_THEMES, true)
        noFAB = getPreference(BooleanPreference.NO_FAB, false)
        probationTime = getPreference(LongPreference.PROBATION_TIME, 0L)
        probationIgnore = getPreference(BooleanPreference.PROBATION_IGNORE, false)
        userId = getPreference(IntPreference.USER_ID, 0)
        showIgnoreWarning = getPreference(BooleanPreference.SHOW_IGNORE_WARNING, true)
        ignoreFormkey = getPreference(StringPreference.IGNORE_FORMKEY, null as String?)
        orientation = getPreference(StringPreference.ORIENTATION, "default")
        pageLayout = getPreference(StringPreference.PAGE_LAYOUT, "auto")
        coloredBookmarks = getPreference(BooleanPreference.COLORED_BOOKMARKS, false)
        p2rDistance = getPreference(FloatPreference.P2R_DISTANCE, 0.5f)
        immersionMode = getPreference(BooleanPreference.IMMERSION_MODE, false)
        hideSignatures = getPreference(BooleanPreference.HIDE_SIGNATURES, false)
        transformer = getPreference(StringPreference.TRANSFORMER, "Default")
        amberDefaultPos = getPreference(BooleanPreference.AMBER_DEFAULT_POS, false)
        hideIgnoredPosts = getPreference(BooleanPreference.HIDE_IGNORED_POSTS, false)
        markedUsers = getPreference(StringSetPreference.MARKED_USERS, HashSet())
        forumIndexShowSections = getPreference(BooleanPreference.FORUM_INDEX_SHOW_SECTIONS, true)
        forumIndexShowSubtitles = getPreference(BooleanPreference.FORUM_INDEX_SHOW_SUBTITLES, true)
        forumIndexHideSubforums = getPreference(BooleanPreference.FORUM_INDEX_HIDE_SUBFORUMS, true)
        postWarningAccepted = getPreference(BooleanPreference.POST_WARNING_ACCEPTED, false)

        //I have never seen this before oh god
    }

    /*
		Type-checked preference getters

		Lint can't infer the correct signature by the type annotation, so if there's any
		ambiguity (e.g. int/long) then cast the value parameter according to the key type

		The @StringRes annotation is there to enforce storing keys as resource strings!
	 */
    fun getPreference(
        key: StringPreference,
        defaultValue: String?
    ): String? {
        return sharedPrefs.getString(mResources.getString(key.key), defaultValue)
    }

    fun getPreference(
        key: StringSetPreference,
        defaultValue: MutableSet<String?>
    ): MutableSet<String?> {
        return sharedPrefs.getStringSet(mResources.getString(key.key), defaultValue)!!
    }

    fun getPreference(key: BooleanPreference, defaultValue: Boolean): Boolean {
        return sharedPrefs.getBoolean(mResources.getString(key.key), defaultValue)
    }

    fun getPreference(key: IntPreference, defaultValue: Int): Int {
        return sharedPrefs.getInt(mResources.getString(key.key), defaultValue)
    }

    fun getPreference(key: LongPreference, defaultValue: Long): Long {
        return sharedPrefs.getLong(mResources.getString(key.key), defaultValue)
    }

    fun getPreference(key: FloatPreference, defaultValue: Float): Float {
        return sharedPrefs.getFloat(mResources.getString(key.key), defaultValue)
    }


    /*
		Type-checked preference setters

		Lint can't infer the correct signature by the type annotation, so if there's any
		ambiguity (e.g. int/long) then cast the value parameter according to the key type

		The @StringRes annotation is there to enforce storing keys as resource strings!
	 */
    fun setPreference(
        key: StringPreference,
        value: String?
    ) {
        sharedPrefs.edit { putString(mResources.getString(key.key), value) }
    }

    fun setPreference(
        key: StringSetPreference,
        value: MutableSet<String?>
    ) {
        sharedPrefs.edit { putStringSet(mResources.getString(key.key), value) }
    }

    fun setPreference(key: BooleanPreference, value: Boolean) {
        sharedPrefs.edit { putBoolean(mResources.getString(key.key), value) }
    }

    fun setPreference(key: IntPreference, value: Int) {
        sharedPrefs.edit { putInt(mResources.getString(key.key), value) }
    }

    fun setPreference(key: LongPreference, value: Long) {
        sharedPrefs.edit { putLong(mResources.getString(key.key), value) }
    }

    fun setPreference(key: FloatPreference, value: Float) {
        sharedPrefs.edit { putFloat(mResources.getString(key.key), value) }
    }


    fun upgradePreferences() {
        if (currPrefVersion < PREFERENCES_VERSION) {
            when (currPrefVersion) {
                0 -> {
                    // Get the current value of the obsolete preference, then remove it
                    val obsoleteKey = "new_threads_first"
                    val newThreadsFirst = sharedPrefs.getBoolean(obsoleteKey, false)
                    sharedPrefs.edit { remove(obsoleteKey) }
                    // transfer the value to the new key
                    setPreference(BooleanPreference.NEW_THREADS_FIRST_UCP, newThreadsFirst)
                    newThreadsFirstUCP = newThreadsFirst
                }

                else -> {}
            }

            //update the preferences so this doesn't run again
            setPreference(IntPreference.CURR_PREF_VERSION, PREFERENCES_VERSION)
            currPrefVersion = PREFERENCES_VERSION
        }
    }


    val resources: Resources
        get() = context.resources

    val isOnProbation: Boolean
        get() {
            if (probationTime == 0L || probationIgnore) {
                return false
            } else {
                if (Date(probationTime) < Date()) {
                    setPreference(LongPreference.PROBATION_TIME, 0L)
                    return false
                }
                return true
            }
        }

    fun canLoadImages(): Boolean {
        val conman = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return imagesEnabled && !(no3gImages && !conman.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!
            .isConnected)
    }

    fun canLoadAvatars(): Boolean {
        return avatarsEnabled && canLoadImages()
    }

    fun isBlockedAvatar(avatarUrl: String?): Boolean {
        return avatarUrl != null && blockedAvatarUrls!!.contains(avatarUrl)
    }

    /**
     * Export the app's current preferences to a user-picked location.
     * 
     * @param settingsUri the file/location to export to
     * @return false if the export failed
     * @see .importSettings
     */
    fun exportSettings(settingsUri: Uri): Boolean {
        val settings: MutableMap<*, *>? = sharedPrefs.all
        val gson = Gson()
        // serialise all SharedPreferences mappings to JSON
        val settingsJson = gson.toJson(settings)

        // save the JSON in binary format
        Log.i(TAG, "exporting settings to uri: " + settingsUri.lastPathSegment)
        try {
            this.context.contentResolver.openOutputStream(settingsUri).use { out ->
                out!!.write(settingsJson.toByteArray())
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }


    /**
     * Import an exported settings file, and apply it to the app, updating AwfulPreferences.
     * 
     * @param settingsUri the file to import
     * @return false if importing failed completely
     * @see .exportSettings
     */
    fun importSettings(settingsUri: Uri): Boolean {
        Log.i(TAG, "importing settings from file: " + settingsUri.lastPathSegment)
        val br: BufferedReader?
        try {
            val `in` = this.context.contentResolver.openInputStream(settingsUri)
            if (`in` == null) {
                Log.w(TAG, "importSettings: unable to get input stream for uri: $settingsUri")
                return false
            }
            br = BufferedReader(InputStreamReader(`in`))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        }

        // read settings JSON file and deserialise into the types SharedPreferences dumps as
        val settings: MutableMap<String?, Any?>
        try {
            settings = Gson().fromJson(
                br,
                object : TypeToken<MutableMap<String?, Any?>?>() {
                }.type
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        sharedPrefs.edit {

            // TODO: 15/12/2017 there's no checking here at all - need to handle any errors safely. What happens when a pref no longer exists, or has its type changed between versions?
            for ((key, value1) in settings) {
                val value: Any = value1!!
                // basically switching on the value type so we can call the correct setter method :/
                if (value is Boolean) {
                    putBoolean(key, value)
                } else if (value is String) {
                    putString(key, value)
                } else if (value is Float) {
                    putFloat(key, value)
                } else if (value is MutableList<*>) {
                    // this one's a little different, list -> string set
                    val values: MutableSet<String?> = HashSet()
                    for (item in value) {
                        values.add(item.toString())
                    }
                    putStringSet(key, values)
                } else {
                    // catch everything else - seems bad, look for Ints/Longs only
                    // TODO: the following prefs currently export and import as doubles, and get cast to either int or long - why??
                    /*
				default_post_fixed_font_size_dip is type Double -> parses as int (are these two legacy settings?)
				default_post_font_size_dip is type Double -> int
				curr_pref_version is type Double -> int
				last_version_seen is type Double -> int
				alert_id_shown is type Double => int
				probation_time is type Double -> long
		 		*/

                    if (longKeys.contains(key)) {
                        putLong(key, (value as Double).toLong())
                    } else {
                        putInt(key, (value as Double).toInt())
                    }
                    // TODO: 15/12/2017 once the doubles are sorted out, probably better to explicitly catch those types and have a safe failure default
                }
            }
        }
        updateValues()
        return true
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        unRegisterListener()
    }

    fun markUser(username: String?) {
        val newMarkedUsers: MutableSet<String?> = HashSet(markedUsers)
        newMarkedUsers.add(username)
        setPreference(StringSetPreference.MARKED_USERS, newMarkedUsers)
        markedUsers = newMarkedUsers
    }

    fun unmarkUser(username: String?) {
        val newMarkedUsers: MutableSet<String?> = HashSet(markedUsers)
        newMarkedUsers.remove(username)
        setPreference(StringSetPreference.MARKED_USERS, newMarkedUsers)
        markedUsers = newMarkedUsers
    }

    companion object {
        private const val TAG = "AwfulPreferences"

        @SuppressLint("StaticFieldLeak")
        var awfulPrefsInstance: AwfulPreferences? = null
            private set

        private const val PREFERENCES_VERSION = 1

        @JvmStatic
        fun getInstance(context: Context): AwfulPreferences {
            awfulPrefsInstance = awfulPrefsInstance ?: AwfulPreferences(context)
            return awfulPrefsInstance!!
        }

        @JvmStatic
        fun getInstance(): AwfulPreferences {
            return awfulPrefsInstance!!
        }

        @JvmStatic
        fun getInstance(
            context: Context,
            updateCallback: AwfulPreferenceUpdate?
        ): AwfulPreferences {
            val instance: AwfulPreferences = getInstance(context)
            instance.registerCallback(updateCallback)
            return instance
        }
    }
}
