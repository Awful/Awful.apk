package com.ferg.awfulapp

/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
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


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ferg.awfulapp.webview.AwfulWebView


/**
 * Loads and displays an image in a zoomable webview.
 */
class ZoomViewFragment : AwfulFragment() {

    private var imageUrl = "No image url"

    companion object {
        const val EXTRA_IMAGE_URL = "image url"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflateView(R.layout.zoom_view_fragment, container, inflater)
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        val mImageView = requireActivity().findViewById<AwfulWebView>(R.id.zoom_view)
        val imageUrl = requireActivity().intent.getStringExtra(EXTRA_IMAGE_URL)

        val imageHtml = StringBuilder()
        imageHtml.append("<html><head><meta name=\"viewport\" content=\"width=device-width, maximum-scale=10\"></head>")
        imageHtml.append("<body style=\"margin: 0px;display:flex;\"><img style=\"max-width: 100%;margin: auto;\" src=\"")
        imageHtml.append(imageUrl)
        imageHtml.append("\"></body></html>")

        mImageView.settings.setSupportZoom(true)
        mImageView.settings.builtInZoomControls = true
        mImageView.settings.displayZoomControls = false
        mImageView.settings.loadWithOverviewMode = true
        mImageView.settings.useWideViewPort = true
        mImageView.setInitialScale(1)
        mImageView.loadDataWithBaseURL(null, imageHtml.toString(), "text/html", "UTF-8", null)

        setActionBarTitle(imageUrl!!)
    }

    override fun getTitle(): String = imageUrl
}