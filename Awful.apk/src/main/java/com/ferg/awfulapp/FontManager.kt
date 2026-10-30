package com.ferg.awfulapp

import android.content.res.AssetManager
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.TypefaceSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.AwfulPreferences.AwfulPreferenceUpdate
import com.google.android.material.textfield.TextInputLayout
import org.apache.commons.lang3.text.WordUtils
import timber.log.Timber.Forest.i
import timber.log.Timber.Forest.w
import java.io.IOException
import java.util.regex.Pattern

/**
 * Handles accessing font files from the assets
 */
class FontManager private constructor(preferredFont: String?, assets: AssetManager) :
    AwfulPreferenceUpdate {
    companion object {
        /**
         * Get the singleton instance of FontManager.
         *
         *
         * Note: Will be null if it hasn't been built using [.createInstance]
         *
         * @return The instance of FontManager or null.
         */
        @JvmStatic
        fun getInstance() : FontManager {
            return fontManagerInstance
        }
        lateinit var fontManagerInstance: FontManager
            private set
        private const val FONT_PATH = "fonts"

        /**
         * Create the singleton instance of FontManager.
         *
         * @param preferences The AwfulPreferences
         * @param assets      An AssetManager for accessing the font files
         */
        fun createInstance(preferences: AwfulPreferences, assets: AssetManager) {
            fontManagerInstance = FontManager(preferences.preferredFont, assets)
            preferences.registerCallback(fontManagerInstance)
        }

        /**
         * Check if the passed text style is valid.
         *
         * @param textStyle A text style
         * @return True iff textStyle is valid
         */
        private fun isValidTextStyle(textStyle: Int): Boolean {
            return textStyle == Typeface.NORMAL || textStyle == Typeface.BOLD || textStyle == Typeface.ITALIC || textStyle == Typeface.BOLD_ITALIC
        }

        /**
         * Create clean font names from the given file names.
         *
         * @param fontList An array of font file names
         * @return An array of font names
         */
        private fun extractFontNames(fontList: Array<String?>): Array<String?> {
            val fontNames = arrayOfNulls<String>(fontList.size)

            val pattern = Pattern.compile("$FONT_PATH/(.*).ttf.mp3", Pattern.CASE_INSENSITIVE)

            for (i in fontList.indices) {
                val fontName: String?
                val matcher = pattern.matcher(fontList[i] ?: "")

                fontName = if (matcher.find()) {
                    matcher.group(1)?.replace("_".toRegex(), " ")
                } else {
                    //if the regex fails, try our best to clean up the filename.
                    fontList[i]?.let {
                        it.replace(Regex(".ttf.mp3"), "")
                        it.replace(Regex("fonts/"), "")
                        it.replace(Regex("_"), " ")
                    }
                }

                fontNames[i] = WordUtils.capitalize(fontName)
            }

            return fontNames
        }
    }

    private var currentFont: Typeface? = null
    private val fonts: MutableMap<String?, Typeface?> = hashMapOf()

    init {
        /**
         * Initialising FontManager
         *
         * @param preferredFont The filename of the selected font
         * @param assets        An AssetManager for accessing the font files
         */
        buildFontList(preferredFont, assets)
    }

    val fontFilenames: Array<String?>
        /**
         * Get the list of font filenames.
         * 
         * @return The list of font filenames as a String array
         */
        get() {
            i("Font list: %s", fonts.keys)
            return fonts.keys.toTypedArray<String?>()
        }

    val fontNames: Array<String?>
        /**
         * Get the list of clean font names
         * 
         * @return The list of font filenames as a String array
         */
        get() = extractFontNames(this.fontFilenames)

    /**
     * Called to update the current font when the AwfulPreferences have changed.
     * 
     * @param preferences The new AwfulPreferences
     * @param key         Not used
     */
    override fun onPreferenceChange(preferences: AwfulPreferences, key: String?) {
        setCurrentFont(preferences.preferredFont)
    }

    /**
     * Set the current font from the fonts map.
     * 
     * @param fontName Filename of the current font
     */
    fun setCurrentFont(fontName: String?) {
        currentFont = fonts[fontName]

        if (currentFont != null) i("Font Selected: %s", fontName)
        else w("Couldn't select font: %s", fontName)
    }

    /**
     * Set typeface of TextViews and all child TextViews to the current font.
     * 
     * @param view  View to be processed
     * @param flags [Typeface.NORMAL], [Typeface.BOLD],
     * [Typeface.ITALIC], or [Typeface.BOLD_ITALIC],
     */
    fun setTypefaceToCurrentFont(view: View?, flags: Int) {
        when (view) {
            is TextView -> setTextViewTypefaceToCurrentFont(view, flags)

            is TextInputLayout -> setTextViewTypefaceToCurrentFont(view)


            is ViewGroup -> {
                for (i in 0..<view.childCount) setTypefaceToCurrentFont(
                    view.getChildAt(i),
                    flags
                )
            }
        }
    }

    /**
     * Recreate the font Map from the asset files.
     * 
     * @param preferredFont The filename of the currently selected font
     * @param assets        An AssetManager for accessing the font files
     */
    fun buildFontList(preferredFont: String?, assets: AssetManager) {
        fonts.clear()
        fonts["default"] = Typeface.defaultFromStyle(Typeface.NORMAL)

        var files: Array<String?>? = null

        try {
            files = assets.list(FONT_PATH)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }

        if (files == null) {
            w("Couldn't load font assets from %s", FONT_PATH)
            return
        }

        for (file in files) {
            val fileName = String.format("%s/%s", FONT_PATH, file)
            fonts[fileName] = Typeface.createFromAsset(assets, fileName)
            i("Processed Font: %s", fileName)
        }

        setCurrentFont(preferredFont)
    }

    /**
     * Set a TextView's typeface to the current font.
     * 
     * @param textView  TextView to set
     * @param textStyle [Typeface.NORMAL], [Typeface.BOLD],
     * [Typeface.ITALIC], or [Typeface.BOLD_ITALIC],
     */
    private fun setTextViewTypefaceToCurrentFont(textView: TextView, textStyle: Int) {
        var textStyle = textStyle
        if (!isValidTextStyle(textStyle)) {
            textStyle = if (textView.typeface != null) textView.typeface
                .style else Typeface.NORMAL
        }

        if (currentFont != null) textView.setTypeface(currentFont, textStyle)
        else w("Couldn't set typeface as currentFont is null")
    }

    /**
     * Set a TextView's typeface to the current font.
     * 
     * @param textLayout  TextView to set
     */
    private fun setTextViewTypefaceToCurrentFont(textLayout: TextInputLayout) {
        if (currentFont != null) textLayout.typeface = currentFont
        else w("Couldn't set typeface as currentFont is null")
    }

    inner class AwfulTypefaceSpan : TypefaceSpan("An awful font") {
        override fun updateDrawState(drawState: TextPaint) {
            drawState.typeface = currentFont
        }

        override fun updateMeasureState(paint: TextPaint) {
            paint.typeface = currentFont
        }
    }

    fun setMenuItemFont(item: MenuItem?) {
        item?.let {
            val title = SpannableStringBuilder(it.title)
            val face: TypefaceSpan = AwfulTypefaceSpan()
            title.setSpan(face, 0, title.length, 0)
            it.title = title
        }
    }
}