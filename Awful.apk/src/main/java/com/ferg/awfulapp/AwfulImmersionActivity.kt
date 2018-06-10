package com.ferg.awfulapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.ferg.awfulapp.preferences.AwfulPreferences


open class AwfulImmersionActivity : AwfulActivity() {

    private lateinit var decorView: View
    private lateinit var immersionGestureDetector: GestureDetector
    private var ignoreFling: Boolean = false

    private val hideHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_HIDING) {
                hideSystemUi()
            } else if (msg.what == MESSAGE_VISIBLE_CHANGE_IN_PROGRESS) {
                ignoreFling = false
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupImmersion()
    }

    private fun setupImmersion() {
        if (mPrefs.immersionMode) {
            decorView = window.decorView

            decorView.setOnSystemUiVisibilityChangeListener { flags ->
                val visible = flags and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
                if (visible) {
                    // Add some delay so the act of swiping to bring system UI into view doesn't turn it back off
                    hideHandler.removeMessages(MESSAGE_VISIBLE_CHANGE_IN_PROGRESS)
                    hideHandler.sendEmptyMessageDelayed(MESSAGE_VISIBLE_CHANGE_IN_PROGRESS, 800)
                    ignoreFling = true
                }
            }

            immersionGestureDetector = GestureDetector(this,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                            if (ignoreFling) return true

                            val visible = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
                            if (visible) {
                                hideSystemUi()
                            }

                            return true
                        }
                    })
            showSystemUi()
        }
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        if (prefs.immersionMode) {
            setupImmersion()
        }
    }

    @SuppressLint("NewApi")
    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (mPrefs.immersionMode) {
            super.dispatchTouchEvent(e)
            return immersionGestureDetector.onTouchEvent(e)
        }

        return super.dispatchTouchEvent(e)
    }

    /**
     * Hide the system UI.
     *
     * @param delayMillis - delay in milliseconds before hiding.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeMessages(MESSAGE_HIDING)
        hideHandler.sendEmptyMessageDelayed(MESSAGE_HIDING, delayMillis.toLong())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (mPrefs.immersionMode) {
            // When the window loses focus (e.g. the action overflow is shown),
            // cancel any pending hide action. When the window gains focus,
            // hide the system UI.
            if (hasFocus) {
                delayedHide(DEFAULT_HIDE_DELAY)
            } else {
                hideHandler.removeMessages(0)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun showSystemUi() {
        if (mPrefs.immersionMode) {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    @SuppressLint("NewApi")
    private fun hideSystemUi() {
        if (mPrefs.immersionMode) {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    companion object {
        private const val DEFAULT_HIDE_DELAY = 300
        private const val MESSAGE_HIDING = 0
        private const val MESSAGE_VISIBLE_CHANGE_IN_PROGRESS = 1
    }
}