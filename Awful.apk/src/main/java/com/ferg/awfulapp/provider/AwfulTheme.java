package com.ferg.awfulapp.provider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;
import android.widget.Toast;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by baka kaba on 03/01/2017.
 * <p>
 * Provides access to themed resources and values.
 * <p>
 * You'll generally use the {@link #forForum(Integer)} method to obtain an appropriate theme.
 * Each enum represents one of the app's default themes, as well as two representing light and dark
 * custom themes. Properties like {@link #displayName} give you access to that theme's specific details,
 * and the {@link #getCssPath()} method resolves a path to a CSS file, falling back to a default if
 * a user theme's CSS can't be accessed.
 */

public enum AwfulTheme {

    // Themes need a display name, source CSS (for the thread webview) and a resource theme
    DEFAULT("Default", "default.css", R.style.Theme_AwfulTheme),
    DARK("Dark", "dark.css", R.style.Theme_AwfulTheme_Dark),
    OLED("OLED", "oled.css", R.style.Theme_AwfulTheme_OLED),
    GREENPOS("YOSPOS", "yospos.css", R.style.Theme_AwfulTheme_YOSPOS),
    AMBERPOS("AMBERPOS", "amberpos.css", R.style.Theme_AwfulTheme_AMBERPOS),
    FYAD("FYAD", "fyad.css", R.style.Theme_AwfulTheme_FYAD),
    BYOB("BYOB", "byob.css", R.style.Theme_AwfulTheme_BYOB),
    CLASSIC("Classic", "classic.css", R.style.Theme_AwfulTheme),

    // TODO: I don't like having these hardcoded copies of the app themes, it's kinda stupid
    /*
        you could just have the base app themes and use the css filename in prefs.theme to check if
        it's an app theme (matches the theme's css filename exactly) or a custom theme based on
        an app theme (ends with the theme's css filename). The problem is, say the user's custom css
        is based on the GREENPOS theme - if they visit that forum, the app will get the appropriate
        theme, which depends on whether 'force forum-specific themes' is active. In both cases they'll
        get the GREENPOS theme enum back - there's no way to know whether that should provide the theme's
        CSS or the user's, without passing that information in, which kinda defeats the point of having
        a black box 'get theme for forum X -> get CSS from theme' system where those details are internal
     */

    // These represent the basic variations for user themes, and are handled specially in the code.
    // The CSS is the fallback file to use if there's a problem, the resource file is the actual app theme
    // (users can't customise those... yet...)
    CUSTOM_DEFAULT("Custom default", "default.css", R.style.Theme_AwfulTheme),
    CUSTOM_DARK("Custom dark", "dark.css", R.style.Theme_AwfulTheme_Dark),
    CUSTOM_OLED("Custom OLED", "oled.css", R.style.Theme_AwfulTheme_OLED),
    CUSTOM_GREENPOS("Custom YOSPOS", "yospos.css", R.style.Theme_AwfulTheme_YOSPOS),
    CUSTOM_AMBERPOS("Custom AMBERPOS", "amberpos.css", R.style.Theme_AwfulTheme_AMBERPOS),
    CUSTOM_FYAD("Custom FYAD", "fyad.css", R.style.Theme_AwfulTheme_FYAD),
    CUSTOM_BYOB("Custom BYOB", "byob.css", R.style.Theme_AwfulTheme_BYOB);

    private static final String APP_CSS_PATH = "file:///android_asset/css/";
    private static final String CUSTOM_THEME_PATH = Environment.getExternalStorageDirectory() + "/awful/";

    /**
     * Values representing custom, non-app themes
     */
    private static final List<AwfulTheme> CUSTOM_THEMES;
    /**
     * Values representing default app themes
     */
    public static final List<AwfulTheme> APP_THEMES;

    static {
        // divide all the enum values into the custom and standard app themes
        CUSTOM_THEMES = Collections.unmodifiableList(Arrays.asList(CUSTOM_DEFAULT, CUSTOM_DARK, CUSTOM_OLED, CUSTOM_GREENPOS, CUSTOM_AMBERPOS, CUSTOM_FYAD, CUSTOM_BYOB));
        List<AwfulTheme> allThemes = new ArrayList<>(Arrays.asList(AwfulTheme.values()));
        allThemes.removeAll(CUSTOM_THEMES);
        APP_THEMES = Collections.unmodifiableList(allThemes);
    }

    /**
     * The ID of the style resource this theme uses
     */
    @StyleRes
    public final int themeResId;
    /**
     * The display name for this theme
     */
    @NonNull
    public final String displayName;
    /**
     * The name of this theme's CSS file, used as a unique identifier
     */
    @NonNull
    public final String cssFilename;
    /**
     * Cached after the first check, to keep things speedy
     */
    @Nullable
    private Boolean isDark = null;

    /**
     * Represents an app theme.
     *
     * @param displayName The name to display for this theme
     * @param cssFilename The filename for the theme's CSS file - used to identify the theme, must be unique
     * @param themeId     The ID of the style resource this theme uses
     */
    AwfulTheme(@NonNull String displayName, @NonNull String cssFilename, @StyleRes int themeId) {
        this.displayName = displayName;
        this.cssFilename = cssFilename;
        this.themeResId = themeId;
    }


    /**
     * The path to the location where custom themes should be stored.
     */
    @NonNull
    public static String getCustomThemePath() {
        return CUSTOM_THEME_PATH;
    }


    /**
     * Get a forum's specific theme, if it has one.
     *
     * @param forumId the ID of the forum to check
     * @param prefs   used to resolve user options, e.g. YOSPOS colours
     * @return a specific theme to use, otherwise null
     */
    @Nullable
    private static AwfulTheme themeForForumId(int forumId, @NonNull AwfulPreferences prefs) {
        switch (forumId) {
            case (Constants.FORUM_ID_FYAD):
            case (Constants.FORUM_ID_FYAD_SUB):
                return FYAD;
            case (Constants.FORUM_ID_BYOB):
            case (Constants.FORUM_ID_COOL_CREW):
                return BYOB;
            case (Constants.FORUM_ID_YOSPOS):
                return (prefs.amberDefaultPos ? AMBERPOS : GREENPOS);
            default:
                return null;
        }
    }

    /**
     * Get the theme to display, resolving according to the given forum and user preferences.
     * <p>
     * Passing null to this method will return the user's currently selected theme. If you pass in
     * a forum ID, and the user has 'display forum themes' enabled, that forum's theme will be returned
     * instead, if it has one.
     */
    @NonNull
    public static AwfulTheme forForum(@Nullable Integer forumId) {
        // if we're using per-forum themes, try to get and return one, otherwise use the current theme in prefs
        AwfulTheme forumTheme = null;
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        if (forumId != null && prefs.forceForumThemes) {
            forumTheme = themeForForumId(forumId, prefs);
        }
        return (forumTheme != null) ? forumTheme : themeForCssFilename(prefs.theme);
    }

    /**
     * Get the AwfulTheme associated with a css filename
     * <p>
     * This is the main way of obtaining a theme, since preferences use css filenames to identify
     * the user's desired theme. This method parses the filename, and returns the app theme
     * it corresponds to. If it's unrecognised, it's treated as a custom theme.
     */
    @NonNull
    private static AwfulTheme themeForCssFilename(@Nullable String themeName) {
        if (StringUtils.isEmpty(themeName)) {
            return DEFAULT;
        }

        for (AwfulTheme appTheme : APP_THEMES) {
            if (appTheme.cssFilename.equalsIgnoreCase(themeName)) {
                return appTheme;
            }
        }
        // not an app theme, treat it as a user theme
        for (AwfulTheme customTheme : CUSTOM_THEMES) {
            if (StringUtils.endsWithIgnoreCase(themeName, customTheme.cssFilename)) {
                return customTheme;
            }
        }
        // couldn't match the custom css filename with any of the app themes, so use the default
        return CUSTOM_DEFAULT;
    }


    /**
     * True if this is a dark app or custom theme
     */
    public boolean isDark() {
        if (isDark == null) {
            // initialise the dark theme flag - we're storing this per enum value, so we can avoid heavy processing
            TypedValue isLight = new TypedValue();
            // this should pull the correct attribute from the thene - it's specified in the base platform themes
            getTheme(AwfulPreferences.getInstance()).resolveAttribute(R.attr.isLightTheme, isLight, true);
            int FALSE = 0;
            isDark = (isLight.data == FALSE);
        }
        return isDark;
    }


    /**
     * Returns the path to a CSS file for this theme.
     * <p>
     * If this is a user theme, and the specified CSS file can't be read, this will fall back
     * to a default CSS file.
     */
    @NonNull
    public String getCssPath() {
        // non-custom themes just need the local css file
        if (!CUSTOM_THEMES.contains(this)) {
            return APP_CSS_PATH + cssFilename;
        }

        // must be a user theme, try to read it from storage
        String errorMessage;
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        Context context = prefs.getContext();
        @SuppressLint("InlinedApi")
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            File cssFile = new File(CUSTOM_THEME_PATH, prefs.theme);
            if (cssFile.isFile() && cssFile.canRead()) {
                return "file:///" + cssFile.getPath();
            }
            errorMessage = "Theme CSS file error!";
        } else {
            errorMessage = context.getString(R.string.no_file_permission_theme);
        }

        // couldn't get the user css - fall back to the base theme
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        return APP_CSS_PATH + cssFilename;

    }

    @Override
    public String toString() {
        return displayName;
    }


    /**
     * Create an Android Theme from this theme's attributes.
     */
    @NonNull
    public Resources.Theme getTheme(@NonNull AwfulPreferences prefs) {
        Resources.Theme theme = prefs.getResources().newTheme();
        theme.applyStyle(themeResId, true);
        return theme;
    }
}
