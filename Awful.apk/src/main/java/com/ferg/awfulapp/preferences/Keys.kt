package com.ferg.awfulapp.preferences

import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 24/03/2016.
 *
 * This class holds constants for preference keys that the app can get and set,
 * grouped by type, to enforce consistency and type checking.
 *
 * The actual key values are stored as string resources, so that the settings XML files can
 * reference them too. Any code that needs to set a preference should call the relevant
 * setPreference() method, passing a key constant from here.
 */

// Strings
enum class StringPreference(val key: Int) {
    USERNAME(R.string.pref_key_username),
    USER_AVATAR_URL(R.string.pref_key_user_title),
    THEME(R.string.pref_key_theme),
    LAUNCHER_ICON(R.string.pref_key_launcher_icon),
    LAYOUT(R.string.pref_key_layout),
    IMGUR_THUMBNAILS(R.string.pref_key_imgur_thumbnails),
    PREFERRED_FONT(R.string.pref_key_preferred_font),
    IGNORE_FORMKEY(R.string.pref_key_ignore_formkey),
    ORIENTATION(R.string.pref_key_orientation),
    PAGE_LAYOUT(R.string.pref_key_page_layout),
    TRANSFORMER(R.string.pref_key_transformer),
    FAVOURITE_FORUMS(R.string.pref_key_favourite_forums),
    RECENT_EMOTES(R.string.pref_key_recent_emotes),
    IMGUR_ACCOUNT(R.string.pref_key_imgur_account),
    IMGUR_ACCOUNT_TOKEN(R.string.pref_key_imgur_account_token),
    IMGUR_REFRESH_TOKEN(R.string.pref_key_imgur_refresh_token),
}

// String Sets
enum class StringSetPreference(val key: Int) {
    MARKED_USERS(R.string.pref_key_marked_users),
    BLOCKED_AVATAR_URLS(R.string.pref_key_blocked_avatar_urls),
    HIDDEN_THREAD_IDS(R.string.pref_key_hidden_thread_ids),
}

// Ints
enum class IntPreference(val key: Int) {
    POST_FONT_SIZE_SP(R.string.pref_key_post_font_size_sp),
    POST_FIXED_FONT_SIZE_SP(R.string.pref_key_post_fixed_font_size_sp),
    POST_PER_PAGE(R.string.pref_key_post_per_page),
    CURR_PREF_VERSION(R.string.pref_key_curr_pref_version),
    ALERT_ID_SHOWN(R.string.pref_key_alert_id_shown),
    USER_ID(R.string.pref_key_user_id),
    LAST_VERSION_SEEN(R.string.pref_key_last_version_seen),
}

// Floats
enum class FloatPreference(val key: Int) {
    P2R_DISTANCE(R.string.pref_key_pull_to_refresh_distance),
}

// Longs
enum class LongPreference(val key: Int) {
    PROBATION_TIME(R.string.pref_key_probation_time),
    IMGUR_TOKEN_EXPIRES(R.string.pref_key_imgur_token_expires),
}

// Bools
enum class BooleanPreference(val key: Int) {
    HAS_PLATINUM(R.string.pref_key_has_platinum),
    HAS_ARCHIVES(R.string.pref_key_has_archives),
    HAS_NO_ADS(R.string.pref_key_has_no_ads),
    IMAGES_ENABLED(R.string.pref_key_images_enabled),
    NO_3G_IMAGES(R.string.pref_key_no_3g_images),
    AVATARS_ENABLED(R.string.pref_key_avatars_enabled),
    HIDE_OLD_IMAGES(R.string.pref_key_hide_old_images),
    SHOW_SMILIES(R.string.pref_key_show_smilies),
    ALTERNATE_BACKGROUND(R.string.pref_key_alternate_background),
    HIGHLIGHT_USER_QUOTE(R.string.pref_key_highlight_user_quote),
    HIGHLIGHT_USERNAME(R.string.pref_key_highlight_username),
    HIGHLIGHT_SELF(R.string.pref_key_highlight_self),
    HIGHLIGHT_OP(R.string.pref_key_highlight_op),
    INLINE_YOUTUBE(R.string.pref_key_inline_youtube),
    INLINE_TWEETS(R.string.pref_key_inline_tweets),
    INLINE_BLUESKY(R.string.pref_key_inline_bluesky),
    INLINE_INSTAGRAM(R.string.pref_key_inline_instagram),
    INLINE_SOUNDCLOUD(R.string.pref_key_inline_soundcloud),
    INLINE_TWITCH(R.string.pref_key_inline_twitch),
    INLINE_TIKTOKS(R.string.pref_key_inline_tiktoks),
    INLINE_VINES(R.string.pref_key_inline_vines),
    INLINE_WEBM(R.string.pref_key_inline_webm),
    AUTOSTART_WEBM(R.string.pref_key_autostart_webm),
    SHOW_ALL_SPOILERS(R.string.pref_key_show_all_spoilers),
    THREAD_INFO_RATING(R.string.pref_key_thread_info_rating),
    THREAD_INFO_TAG(R.string.pref_key_thread_info_tag),
    NEW_THREADS_FIRST_UCP(R.string.pref_key_new_threads_first_ucp),
    NEW_THREADS_FIRST_FORUM(R.string.pref_key_new_threads_first_forum),
    UPPER_NEXT_ARROW(R.string.pref_key_upper_next_arrow),
    SEND_USERNAME_IN_REPORT(R.string.pref_key_send_username_in_report),
    DISABLE_GIFS(R.string.pref_key_disable_gifs),
    HIDE_OLD_POSTS(R.string.pref_key_hide_old_posts),
    ALWAYS_OPEN_URLS(R.string.pref_key_always_open_urls),
    LOCK_SCROLLING(R.string.pref_key_lock_scrolling),
    DISABLE_TIMGS(R.string.pref_key_disable_timgs),
    DISABLE_PULL_NEXT(R.string.pref_key_disable_pull_next),
    VOLUME_SCROLL(R.string.pref_key_volume_scroll),
    FORCE_FORUM_THEMES(R.string.pref_key_force_forum_themes),
    NO_FAB(R.string.pref_key_no_fab),
    SHOW_IGNORE_WARNING(R.string.pref_key_show_ignore_warning),
    COLORED_BOOKMARKS(R.string.pref_key_colored_bookmarks),
    HIDE_SIGNATURES(R.string.pref_key_hide_signatures),
    AMBER_DEFAULT_POS(R.string.pref_key_amber_default_pos),
    HIDE_IGNORED_POSTS(R.string.pref_key_hide_ignored_posts),
    IMMERSION_MODE(R.string.pref_key_immersion_mode),
    FORUM_INDEX_SHOW_SECTIONS(R.string.pref_key_forum_index_show_section_headers),
    FORUM_INDEX_SHOW_SUBTITLES(R.string.pref_key_forum_index_show_subtitles),
    FORUM_INDEX_HIDE_SUBFORUMS(R.string.pref_key_forum_index_hide_subforums),
    POST_WARNING_ACCEPTED(R.string.pref_key_post_warning_accepted),
    PROBATION_IGNORE(R.string.pref_key_probation_ignore),
    SHOW_HIDDEN_THREADS(R.string.pref_key_show_hidden_threads),
    HIGHLIGHT_YOUR_THREADS(R.string.pref_key_highlight_your_threads)
}