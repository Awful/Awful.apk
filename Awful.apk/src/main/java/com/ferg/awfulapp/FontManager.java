package com.ferg.awfulapp;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ferg.awfulapp.preferences.AwfulPreferences;

import org.apache.commons.lang3.text.WordUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Handles accessing font files from the assets
 */
public class FontManager implements AwfulPreferences.AwfulPreferenceUpdate {
    private static FontManager instance;
    private static final String FONT_PATH = "fonts";
    private Typeface currentFont;
    private final Map<String, Typeface> fonts = new HashMap<>();

    /**
     * Get the singleton instance of FontManager.
     * <p>
     * Note: Will be null if it hasn't been built using {@link #createInstance(AwfulPreferences, AssetManager)}
     *
     * @return The instance of FontManager or null.
     */
    public static FontManager getInstance() {
        return instance;
    }

    /**
     * Create the singleton instance of FontManager.
     *
     * @param preferences The AwfulPreferences
     * @param assets      An AssetManager for accessing the font files
     */
    public static void createInstance(@NonNull AwfulPreferences preferences, @NonNull AssetManager assets) {
        instance = new FontManager(preferences.preferredFont, assets);
        preferences.registerCallback(instance);
    }

    /**
     * Constructor for FontManager
     *
     * @param preferredFont The filename of the selected font
     * @param assets        An AssetManager for accessing the font files
     */
    private FontManager(String preferredFont, AssetManager assets) {
        fonts.clear();
        fonts.put("default", Typeface.defaultFromStyle(Typeface.NORMAL));

        String[] files = null;

        try {
            files = assets.list(FONT_PATH);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        if (files == null) {
            Timber.w("Couldn't load font assets from %s", FONT_PATH);
            return;
        }

        for (String file : files) {
            String fileName = String.format("%s/%s", FONT_PATH, file);
            fonts.put(fileName, Typeface.createFromAsset(assets, fileName));
            Timber.i("Processed Font: %s", fileName);
        }

        setCurrentFont(preferredFont);
    }

    /**
     * Get the list of font filenames.
     *
     * @return The list of font filenames as a String array
     */
    public String[] getFontFilenames() {
        Timber.i("Font list: %s", fonts.keySet());
        return fonts.keySet().toArray(new String[0]);
    }

    /**
     * Get the list of clean font names
     *
     * @return The list of font filenames as a String array
     */
    public String[] getFontNames() {
        return extractFontNames(getFontFilenames());
    }

    /**
     * Called to update the current font when the AwfulPreferences have changed.
     *
     * @param preferences The new AwfulPreferences
     * @param key         Not used
     */
    @Override
    public void onPreferenceChange(AwfulPreferences preferences, @Nullable String key) {
        setCurrentFont(preferences.preferredFont);
    }

    /**
     * Set the current font from the fonts map.
     *
     * @param fontName Filename of the current font
     */
    public void setCurrentFont(String fontName) {
        currentFont = fonts.get(fontName);

        if (currentFont != null)
            Timber.i("Font Selected: %s", fontName);
        else
            Timber.w("Couldn't select font: %s", fontName);
    }

    /**
     * Set typeface of TextViews and all child TextViews to the current font.
     *
     * @param view  View to be processed
     * @param flags {@link Typeface#NORMAL}, {@link Typeface#BOLD},
     *              {@link Typeface#ITALIC}, or {@link Typeface#BOLD_ITALIC},
     */
    public void setTypefaceToCurrentFont(View view, int flags) {
        if (view instanceof TextView)
            setTextViewTypefaceToCurrentFont((TextView) view, flags);
        else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for (int i = 0; i < viewGroup.getChildCount(); i++)
                setTypefaceToCurrentFont(viewGroup.getChildAt(i), flags);
        }
    }

    /**
     * Set a TextView's typeface to the current font.
     *
     * @param textView  TextView to set
     * @param textStyle {@link Typeface#NORMAL}, {@link Typeface#BOLD},
     *                  {@link Typeface#ITALIC}, or {@link Typeface#BOLD_ITALIC},
     */
    private void setTextViewTypefaceToCurrentFont(TextView textView, int textStyle) {
        if (!isValidTextStyle(textStyle)) {
            textStyle = textView.getTypeface() != null ?
                    textView.getTypeface().getStyle() : Typeface.NORMAL;
        }

        if (currentFont != null)
            textView.setTypeface(currentFont, textStyle);
        else
            Timber.w("Couldn't set typeface as currentFont is null");
    }

    /**
     * Check if the passed text style is valid.
     *
     * @param textStyle A text style
     * @return True iff textStyle is valid
     */
    private static boolean isValidTextStyle(int textStyle) {
        return textStyle == Typeface.NORMAL || textStyle == Typeface.BOLD ||
                textStyle == Typeface.ITALIC || textStyle == Typeface.BOLD_ITALIC;
    }

    /**
     * Create clean font names from the given file names.
     *
     * @param fontList An array of font file names
     * @return An array of font names
     */
    private static String[] extractFontNames(@NonNull String[] fontList) {
        String[] fontNames = new String[fontList.length];

        Pattern pattern = Pattern.compile(FONT_PATH + "/(.*).ttf.mp3", Pattern.CASE_INSENSITIVE);

        for (int i = 0; i < fontList.length; i++) {
            String fontName;
            Matcher matcher = pattern.matcher(fontList[i]);

            if (matcher.find()) {
                fontName = matcher.group(1).replaceAll("_", " ");
            } else {
                //if the regex fails, try our best to clean up the filename.
                fontName = fontList[i].replaceAll(".ttf.mp3", "")
                        .replaceAll("fonts/", "")
                        .replaceAll("_", " ");
            }

            fontNames[i] = WordUtils.capitalize(fontName);
        }

        return fontNames;
    }
}