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
package com.ferg.awfulapp.widget

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.AwfulPreferences.AwfulPreferenceUpdate
import com.ferg.awfulapp.provider.ColorProvider

class AwfulProgressBar : View, AwfulPreferenceUpdate {
    private var mProgress = 100
    private lateinit var mProgressColor: Paint
    private lateinit var mClearColor: Paint


    constructor(context: Context?) : super(context) {
        setPaint(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        setPaint(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        if (this.isInEditMode) {
            return
        } // suppress errors during GUI dev

        setPaint(context)
    }

    private fun setPaint(context: Context?) {
        mProgressColor = Paint()
        mProgressColor.color = ColorProvider.PROGRESS_BAR.color
        mClearColor = Paint()
        mClearColor.color = Color.TRANSPARENT
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPaint(mClearColor)
        if (height > 3) {
            canvas.drawRect(
                0f,
                0f,
                ((mProgress / 100.0) * width).toInt().toFloat(),
                (height - 2).toFloat(),
                mProgressColor
            )
        } else {
            canvas.drawRect(
                0f,
                0f,
                ((mProgress / 100.0) * width).toInt().toFloat(),
                height.toFloat(),
                mProgressColor
            )
        }
    }

    fun setProgress(progress: Int, activity: Activity?) {
        if (activity != null) {
            mProgress = progress
            activity.runOnUiThread {
                if (progress in 1..<100) {
                    visibility = VISIBLE
                    invalidate()
                } else {
                    visibility = GONE
                }
            }
        }
    }


    override fun onPreferenceChange(preferences: AwfulPreferences, key: String?) {
        this.setPaint(context)
    }
}
