package com.ferg.awfulapp.widget

import android.content.Context
import androidx.appcompat.view.CollapsibleActionView
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.ferg.awfulapp.R
import com.ferg.awfulapp.util.PassiveTextWatcher
import com.ferg.awfulapp.util.bind
import kotlin.properties.Delegates


/**
 * Created by baka kaba on 24/02/2020.
 *
 * A simple widget for searching a provided [webView].
 *
 * Replaces the in-built find toolbar in the Webview class which is fully broken at this point.
 * Set [webView] and text entered into the search bar will be forwarded to its search system. This
 * implements [CollapsibleActionView] so it can be used as an expanding menu item that appears in
 * the toolbar, and cleans up after itself once closed.
 *
 * Implemented as a LinearLayout because ConstraintLayout wasn't playing nice with the textbox filling
 * the available area.
 */

class WebViewSearchBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), WebView.FindListener, CollapsibleActionView {

    private val nextButton: ImageButton by bind(R.id.web_view_search_next_result)
    private val prevButton: ImageButton by bind(R.id.web_view_search_prev_result)
    private val searchBox: EditText by bind(R.id.web_view_search_text)
    private val resultCount: TextView by bind(R.id.webview_search_result_count)

    /** The webview this widget will attempt to search in */
    var webView by Delegates.observable<WebView?>(null) { _, _, new -> new?.setFindListener(this) }


    init {
        LayoutInflater.from(context).inflate(R.layout.webview_search_bar, this, true)

        searchBox.addTextChangedListener(object : PassiveTextWatcher() {
            override fun afterTextChanged(s: Editable?) = onSearchQueryUpdated()
        })
        nextButton.setOnClickListener { webView?.findNext(true) }
        prevButton.setOnClickListener { webView?.findNext(false) }
    }


    private fun onSearchQueryUpdated() {
        // don't pass null to #findAllAsync no matter what it claims, those NPEs are why I had to write this
        webView?.findAllAsync(searchBox.text?.toString() ?: "")
    }


    override fun onFindResultReceived(activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean) {
        resultCount.text = when {
            searchBox.text.isEmpty() -> ""
            numberOfMatches == 0 -> "0/0"
            else -> "${activeMatchOrdinal + 1}/$numberOfMatches"
        }
    }


    override fun onActionViewExpanded() {
        searchBox.requestFocus()
        // show the keyboard - need to delay this for... reasons? But it makes it pop up reliably
        searchBox.postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }


    override fun onActionViewCollapsed() {
        // clean up because we're only getting hidden
        searchBox.text.clear()
        searchBox.clearFocus()
        webView?.clearMatches()
        // hide the keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }
}