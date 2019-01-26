package com.ferg.awfulapp;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ferg.awfulapp.preferences.AwfulPreferences;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Handles accessing font files from the assets
 */
public class FontManager implements AwfulPreferences.AwfulPreferenceUpdate {
    private static final String FONT_PATH = "fonts";
    private Typeface currentFont;
    private final Map<String, Typeface> fonts = new HashMap<>();

    /**
     * Constructor for FontManager
     *
     * @param preferredFont The filename of the selected font
     * @param assets        An AssetManager for accessing the font files
     */
    public FontManager(String preferredFont, AssetManager assets) {
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
}