/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
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
 */

package com.ferg.awfulapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View

import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences

class PrivateMessageActivity : AwfulActivity(), MessageFragment.PrivateMessageCallbacks {

    private var paneTwo: View? = null
    private var pmIntentID: String? = null
    lateinit private var mToolbar: Toolbar

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.private_message_activity)
        mPrefs = AwfulPreferences.getInstance(this, this)

        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        setActionBar()

        intent.data?.let { data ->
            pmIntentID = data.getQueryParameter(Constants.PARAM_PRIVATE_MESSAGE_ID)
        }

        paneTwo = findViewById(R.id.fragment_pane_two)
        setContentPane()
    }

    private fun setContentPane() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_pane) == null) {
            val fragment = PrivateMessageListFragment()

            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_pane, fragment)
            var pmid = 0
            if (pmIntentID != null && pmIntentID!!.matches("\\d+".toRegex())) {
                pmid = Integer.parseInt(pmIntentID)
            }
            if (paneTwo == null) {
                if (pmid > 0) {//this should only trigger if we intercept 'private.php?pmid=XXXXX' and we are not on a tablet.
                    startActivity(Intent().setClass(this, MessageDisplayActivity::class.java).putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, pmid))
                }
            }

            transaction.commit()
        }
    }

    fun showMessage(name: String?, id: Int) {
        if (paneTwo != null) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_pane_two, MessageFragment(name, id), MESSAGE_FRAGMENT_TAG)
            transaction.commit()
        } else {
            if (name != null) {
                startActivity(Intent().setClass(this, MessageDisplayActivity::class.java).putExtra(Constants.PARAM_USERNAME, name))
            } else {
                startActivity(Intent().setClass(this, MessageDisplayActivity::class.java).putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, id))
            }
        }
    }

    override fun onMessageClosed() {
        // remove the message fragment, if it exists
        val messageFragment = supportFragmentManager.findFragmentByTag(MESSAGE_FRAGMENT_TAG)
        supportFragmentManager.beginTransaction().remove(messageFragment).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                returnHome()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun returnHome() {
        finish()
        val i = Intent(this, ForumsIndexActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
    }

    companion object {
        private val MESSAGE_FRAGMENT_TAG = "message fragment"
    }
}
