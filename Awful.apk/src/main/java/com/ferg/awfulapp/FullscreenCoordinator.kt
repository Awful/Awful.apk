package com.ferg.awfulapp

import android.view.View
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.util.AwfulUtils

class FullscreenCoordinator(private val activity: ForumsIndexActivity, private var isImmersive: Boolean) {

    private var decorView: View? = null

    init {
        setupImmersion()
    }


    private fun setupImmersion() {
        decorView = activity.window.decorView
        if(isImmersive) hideSystemUi()
        else showSystemUi()
    }


    private fun showSystemUi() {
        if (AwfulUtils.isKitKat()) {
            decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    private fun hideSystemUi() {
        if (AwfulUtils.isKitKat()) {
            decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }


    fun onPreferenceChange(prefs: AwfulPreferences) {
        isImmersive = prefs.immersionMode
        if (decorView == null) {
            setupImmersion()
        }
    }

    fun onFocusChange(hasFocus: Boolean) {
        if(hasFocus) setupImmersion()
    }
}