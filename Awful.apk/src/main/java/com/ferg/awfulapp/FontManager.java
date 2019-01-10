package com.ferg.awfulapp;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ferg.awfulapp.preferences.AwfulPreferences;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class FontManager {
    private static final String FONT_PATH = "fonts";
    private Typeface currentFont;
    private final Map<String, Typeface> fonts = new HashMap<>();

    public FontManager(AwfulPreferences preferences, AssetManager assets) {
        buildFontList(preferences, assets);
    }

    public String[] getFontList() {
        Timber.i("Font list: %s", fonts.keySet());
        return fonts.keySet().toArray(new String[0]);
    }

    public void setCurrentFont(String fontName) {
        currentFont = fonts.get(fontName);

        if (currentFont != null)
            Timber.i("Font Selected: %s", fontName);
        else
            Timber.e("Couldn't select font: %s", fontName);
    }

    public void setTypefaceToCurrentFont(View view, int flags) {
        if (view instanceof TextView)
            setTextViewTypefaceToCurrentFont((TextView) view, flags);
        else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for (int i = 0; i < viewGroup.getChildCount(); i++)
                setTypefaceToCurrentFont(viewGroup.getChildAt(i), flags);
        }
    }

    private void buildFontList(AwfulPreferences preferences, AssetManager assets) {
        fonts.clear();
        fonts.put("default", Typeface.defaultFromStyle(Typeface.NORMAL));

        String[] files = null;

        try {
            files = assets.list(FONT_PATH);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        if (files == null) {
            Timber.e("Couldn't load fonts from files?");
            return;
        }

        for (String file : files) {
            String fileName = String.format("%s/%s", FONT_PATH, file);
            fonts.put(fileName, Typeface.createFromAsset(assets, fileName));
            Timber.i("Processed Font: %s", fileName);
        }

        setCurrentFont(preferences.preferredFont);
    }

    private void setTextViewTypefaceToCurrentFont(TextView textView, int textStyle) {
        if (textStyle < 0 || textStyle > 3) {
            textStyle = textView.getTypeface() == null ? Typeface.NORMAL :
                    textView.getTypeface().getStyle();
        }

        if (currentFont != null)
            textView.setTypeface(currentFont, textStyle);
        else
            Timber.e("currentFont is null");
    }
}