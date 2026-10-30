/********************************************************************************
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
 */
package com.ferg.awfulapp

import android.os.Bundle
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ferg.awfulapp.databinding.PostReplyActivityBinding

class PostReplyActivity : AwfulSheetActivity() {
    var mToolbar: Toolbar? = null
    var replyFragment: PostReplyFragment? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = PostReplyActivityBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        mToolbar = binding.postToolbar.awfulToolbar

        setSupportActionBar(mToolbar)
        setUpActionBar()


        val sheetMenu = PopupMenu(this, window.decorView as ViewGroup).menu
        val inflater = MenuInflater(this)
        inflater.inflate(R.menu.insert_into_message, sheetMenu)
        inflater.inflate(R.menu.format_message, sheetMenu)

        val fm = supportFragmentManager
        replyFragment = fm.findFragmentById(R.id.reply_fragment) as PostReplyFragment?

        setupBottomSheet()

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.getRoot()
        ) { _: View?, insets: WindowInsetsCompat ->
            val innerPadding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            ViewCompat.setPaddingRelative(
                binding.getRoot(),
                innerPadding.left,
                0,
                innerPadding.right,
                if (imeVisible) imeHeight else innerPadding.bottom
            )
            insets
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onLeaveActivity()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onLeaveActivity()
    }

    private fun onLeaveActivity() {
        replyFragment?.onNavigateBack()
    }
}
