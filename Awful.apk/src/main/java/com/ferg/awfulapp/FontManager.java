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
     * Note: Will be null if it hasn't been built using the other getInstance first
     *
     * @return The instance of FontManager or null.
     */
    public static FontManager getInstance() {
        return instance;
    }

    /**
     * Get the singleton instance of FontManager.
     *
     * @param preferredFont The filename of the selected font
     * @param assets        An AssetManager for accessing the font files
     * @return The instance of FontManager
     */
    public static FontManager getInstance(@NonNull String preferredFont, @NonNull AssetManager assets) {
        if (instance == null)
            instance = new FontManager(preferredFont, assets);

        return instance;
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
    public String[] getFontList() {
        Timber.i("Font list: %s", fonts.keySet());
        return fonts.keySet().toArray(new String[0]);
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
        if (textStyle < 0 || textStyle > 3) {
            textStyle = textView.getTypeface() == null ? Typeface.NORMAL :
                    textView.getTypeface().getStyle();
        }

        if (currentFont != null)
            textView.setTypeface(currentFont, textStyle);
        else
            Timber.w("Couldn't set typeface as currentFont is null");
    }

    /**
     * Create clean font names from the given file names.
     *
     * @param fontList An array of font file names
     * @return An array of font names
     */
    public static String[] extractFontNames(@NonNull String[] fontList) {
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