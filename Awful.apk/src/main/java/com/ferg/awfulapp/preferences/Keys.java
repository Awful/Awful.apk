package com.ferg.awfulapp.preferences;

import android.support.annotation.IntDef;

import com.ferg.awfulapp.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by baka kaba on 24/03/2016.
 *
 * <p>This class holds constants for preference keys that the app can get and set,
 * grouped by type, to enforce consistency and type checking.</p>
 *
 * <p>The actual key values are stored as string resources, so that the settings XML files can
 * reference them too. Any code that needs to set a preference should call the relevant
 * setPreference() method (e.g. {@link AwfulPreferences#setPreference(int, boolean)},
 * passing a key constant from here.</p>
 */
public abstract class Keys {


    // strings
    @IntDef({
            USERNAME,
            USER_TITLE,
            THEME,
            LAYOUT,
            IMGUR_THUMBNAILS,
            PREFERRED_FONT,
            IGNORE_FORMKEY,
            ORIENTATION,
            PAGE_LAYOUT,
            TRANSFORMER
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StringPreference{}


    // string sets
    @IntDef({
            MARKED_USERS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StringSetPreference{}


    // ints
    @IntDef({
            POST_FONT_SIZE_DIP,
            POST_FIXED_FONT_SIZE_DIP,
            POST_PER_PAGE,
            CURR_PREF_VERSION,
            ALERT_ID_SHOWN,
            USER_ID,
            LAST_VERSION_SEEN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface IntPreference{}


    // floats
    @IntDef({
            P2R_DISTANCE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FloatPreference{}

    // longs
    @IntDef({
            PROBATION_TIME
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LongPreference{}

    // bools
    @IntDef({
            HAS_PLATINUM,
            HAS_ARCHIVES,
            HAS_NO_ADS,
            IMAGES_ENABLED,
            NO_3G_IMAGES,
            AVATARS_ENABLED,
            HIDE_OLD_IMAGES,
            SHOW_SMILIES,
            ALTERNATE_BACKGROUND,
            HIGHLIGHT_USER_QUOTE,
            HIGHLIGHT_USERNAME,
            HIGHLIGHT_SELF,
            HIGHLIGHT_OP,
            INLINE_YOUTUBE,
            INLINE_TWEETS,
            INLINE_VINES,
            INLINE_WEBM,
            AUTOSTART_WEBM,
            ENABLE_HARDWARE_ACCELERATION,
            WRAP_THREAD_TITLES,
            SHOW_ALL_SPOILERS,
            THREAD_INFO_RATING,
            THREAD_INFO_TAG,
            NEW_THREADS_FIRST_UCP,
            NEW_THREADS_FIRST_FORUM,
            UPPER_NEXT_ARROW,
            SEND_USERNAME_IN_REPORT,
            DISABLE_GIFS,
            HIDE_OLD_POSTS,
            ALWAYS_OPEN_URLS,
            LOCK_SCROLLING,
            DISABLE_TIMGS,
            DISABLE_PULL_NEXT,
            VOLUME_SCROLL,
            FORCE_FORUM_THEMES,
            NO_FAB,
            SHOW_IGNORE_WARNING,
            COLORED_BOOKMARKS,
            HIDE_SIGNATURES,
            AMBER_DEFAULT_POS,
            HIDE_IGNORED_POSTS,
            IMMERSION_MODE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BooleanPreference{}


    public static final int USERNAME = R.string.pref_key_username;
    public static final int USER_TITLE = R.string.pref_key_user_title;
    public static final int THEME = R.string.pref_key_theme;
    public static final int LAYOUT = R.string.pref_key_layout;
    public static final int IMGUR_THUMBNAILS = R.string.pref_key_imgur_thumbnails;
    public static final int PREFERRED_FONT = R.string.pref_key_preferred_font;
    public static final int IGNORE_FORMKEY = R.string.pref_key_ignore_formkey;
    public static final int ORIENTATION = R.string.pref_key_orientation;
    public static final int PAGE_LAYOUT = R.string.pref_key_page_layout;
    public static final int TRANSFORMER = R.string.pref_key_transformer;

    public static final int POST_FONT_SIZE_DIP = R.string.pref_key_post_font_size_dip;
    public static final int POST_FIXED_FONT_SIZE_DIP = R.string.pref_key_post_fixed_font_size_dip;
    public static final int POST_PER_PAGE = R.string.pref_key_post_per_page;
    public static final int CURR_PREF_VERSION = R.string.pref_key_curr_pref_version;
    public static final int ALERT_ID_SHOWN = R.string.pref_key_alert_id_shown;
    public static final int USER_ID = R.string.pref_key_user_id;
    public static final int LAST_VERSION_SEEN = R.string.pref_key_last_version_seen;

    public static final int PROBATION_TIME = R.string.pref_key_probation_time;

    public static final int P2R_DISTANCE = R.string.pref_key_pull_to_refresh_distance;

    public static final int HAS_PLATINUM = R.string.pref_key_has_platinum;
    public static final int HAS_ARCHIVES = R.string.pref_key_has_archives;
    public static final int HAS_NO_ADS = R.string.pref_key_has_no_ads;
    public static final int IMAGES_ENABLED = R.string.pref_key_images_enabled;
    public static final int NO_3G_IMAGES = R.string.pref_key_no_3g_images;
    public static final int AVATARS_ENABLED = R.string.pref_key_avatars_enabled;
    public static final int HIDE_OLD_IMAGES = R.string.pref_key_hide_old_images;
    public static final int SHOW_SMILIES = R.string.pref_key_show_smilies;
    public static final int ALTERNATE_BACKGROUND = R.string.pref_key_alternate_background;
    public static final int HIGHLIGHT_USER_QUOTE = R.string.pref_key_highlight_user_quote;
    public static final int HIGHLIGHT_USERNAME = R.string.pref_key_highlight_username;
    public static final int HIGHLIGHT_SELF = R.string.pref_key_highlight_self;
    public static final int HIGHLIGHT_OP = R.string.pref_key_highlight_op;
    public static final int INLINE_YOUTUBE = R.string.pref_key_inline_youtube;
    public static final int INLINE_TWEETS = R.string.pref_key_inline_tweets;
    public static final int INLINE_VINES = R.string.pref_key_inline_vines;
    public static final int INLINE_WEBM = R.string.pref_key_inline_webm;
    public static final int AUTOSTART_WEBM = R.string.pref_key_autostart_webm;
    public static final int ENABLE_HARDWARE_ACCELERATION = R.string.pref_key_enable_hardware_acceleration;
    public static final int WRAP_THREAD_TITLES = R.string.pref_key_wrap_thread_titles;
    public static final int SHOW_ALL_SPOILERS = R.string.pref_key_show_all_spoilers;
    public static final int THREAD_INFO_RATING = R.string.pref_key_thread_info_rating;
    public static final int THREAD_INFO_TAG = R.string.pref_key_thread_info_tag;
    public static final int NEW_THREADS_FIRST_UCP = R.string.pref_key_new_threads_first_ucp;
    public static final int NEW_THREADS_FIRST_FORUM = R.string.pref_key_new_threads_first_forum;
    public static final int UPPER_NEXT_ARROW = R.string.pref_key_upper_next_arrow;
    public static final int SEND_USERNAME_IN_REPORT = R.string.pref_key_send_username_in_report;
    public static final int DISABLE_GIFS = R.string.pref_key_disable_gifs;
    public static final int HIDE_OLD_POSTS = R.string.pref_key_hide_old_posts;
    public static final int ALWAYS_OPEN_URLS = R.string.pref_key_always_open_urls;
    public static final int LOCK_SCROLLING = R.string.pref_key_lock_scrolling;
    public static final int DISABLE_TIMGS = R.string.pref_key_disable_timgs;
    public static final int DISABLE_PULL_NEXT = R.string.pref_key_disable_pull_next;
    public static final int VOLUME_SCROLL = R.string.pref_key_volume_scroll;
    public static final int FORCE_FORUM_THEMES = R.string.pref_key_force_forum_themes;
    public static final int NO_FAB = R.string.pref_key_no_fab;
    public static final int SHOW_IGNORE_WARNING = R.string.pref_key_show_ignore_warning;
    public static final int COLORED_BOOKMARKS = R.string.pref_key_colored_bookmarks;
    public static final int HIDE_SIGNATURES = R.string.pref_key_hide_signatures;
    public static final int AMBER_DEFAULT_POS = R.string.pref_key_amber_default_pos;
    public static final int HIDE_IGNORED_POSTS = R.string.pref_key_hide_ignored_posts;
    public static final int IMMERSION_MODE = R.string.pref_key_immersion_mode;

    public static final int MARKED_USERS = R.string.pref_key_marked_users;


}
