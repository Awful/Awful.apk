/**
 * *****************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * 
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *****************************************************************************
 */
package com.ferg.awfulapp

import android.app.ProgressDialog
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import com.android.volley.VolleyError
import com.ferg.awfulapp.Authentication.isUserLoggedIn
import com.ferg.awfulapp.CaptchaActivity.Companion.handleCaptchaChallenge
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.CookieController
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.StringPreference
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.LoginRequest
import org.apache.http.HttpStatus
import timber.log.Timber.Forest.i

class AwfulLoginActivity : AwfulActivity() {
    private var mLogin: Button? = null
    private var mUsername: EditText? = null
    private var mPassword: EditText? = null
    private var mAccept: CheckBox? = null
    private var mTerms: TextView? = null

    private var mDialog: ProgressDialog? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.login)

        CookieController.logCookies()

        mLogin = findViewById<View?>(R.id.login) as Button
        mUsername = findViewById<View?>(R.id.username) as EditText
        mPassword = findViewById<View?>(R.id.password) as EditText
        mAccept = findViewById<View?>(R.id.login_accept) as CheckBox
        mTerms = findViewById<View?>(R.id.login_terms) as TextView
        mPassword?.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (mAccept?.isChecked != true) {
                    return false
                }
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginClick()
                }
                if (event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    loginClick()
                }
                return false
            }
        })

        mLogin?.setOnClickListener(onLoginClick)

        mUsername?.requestFocus()

        val image = findViewById<View?>(R.id.dealwithit) as ImageView
        image.setOnClickListener { (image.drawable as AnimationDrawable).start() }

        mTerms?.movementMethod = LinkMovementMethod.getInstance()

        mAccept?.setOnClickListener {
            mLogin?.let { login ->
                mAccept?.let { accept -> login.isEnabled = accept.isChecked }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        i("onResume")
        if (isUserLoggedIn()) {
            i("Already logged in! Closing AwfulLoginActivity!")
            this.finish()
        }
    }

    public override fun onPause() {
        super.onPause()
        mDialog?.dismiss()
    }

    public override fun onStop() {
        super.onStop()
        mDialog?.dismiss()

    }

    public override fun onDestroy() {
        super.onDestroy()
        mDialog?.dismiss()
    }

    private fun loginClick() {
        val username = NetworkUtils.encodeHtml(mUsername?.text.toString())
        val password = NetworkUtils.encodeHtml(mPassword?.text.toString())

        mDialog = ProgressDialog.show(this@AwfulLoginActivity, "Logging In", "Hold on...", true)
        setPreferredFont(mDialog?.findViewById(android.R.id.title))
        val self = this
        NetworkUtils.queueRequest(
            LoginRequest(this, username, password).build(
                null,
                object : AwfulResultCallback<Boolean> {
                    override fun success(result: Boolean) {
                        onLoginSuccess()
                    }
                    override fun failure(error: VolleyError?) {
                        handleCaptchaChallenge(self, error!!)

                        // Volley sometimes generates NetworkErrors with no response set, or wraps them
                        var response = error.networkResponse
                        if (response == null) {
                            val cause = error.cause
                            if (cause != null && cause is VolleyError) {
                                response = cause.networkResponse
                            }
                        }
                        if (response != null && response.statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                            val result =
                                CookieController.getCookieString(Constants.COOKIE_PREF_PASSWORD).isNotEmpty()
                            if (result) {
                                // TODO: this should probably be handled by firing a ProfileRequest and getting the username from there, maybe through SyncManager
                                val prefs = AwfulPreferences.getInstance(applicationContext)
                                prefs.setPreference(StringPreference.USERNAME, username)
                                onLoginSuccess()
                            } else {
                                onLoginFailed()
                            }
                        } else {
                            onLoginFailed()
                        }
                    }

                    fun onLoginSuccess() {
                        mDialog?.dismiss()
                        Toast.makeText(
                            this@AwfulLoginActivity,
                            R.string.login_succeeded,
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(RESULT_OK)
                        self.finish()
                    }

                    fun onLoginFailed() {
                        mDialog?.dismiss()
                        Toast.makeText(
                            this@AwfulLoginActivity,
                            R.string.login_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(RESULT_CANCELED)
                    }
                })
        )
    }

    private val onLoginClick: View.OnClickListener = View.OnClickListener { loginClick() }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
