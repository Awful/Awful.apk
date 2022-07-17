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

package com.ferg.awfulapp.search

import android.app.ProgressDialog
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Html
import android.view.*
import android.widget.EditText
import android.widget.TextView
import com.android.volley.VolleyError
import com.ferg.awfulapp.AwfulFragment
import com.ferg.awfulapp.NavigationEvent
import com.ferg.awfulapp.NavigationEvent.Companion.parse
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.SearchRequest
import com.ferg.awfulapp.task.SearchResultPageRequest
import com.ferg.awfulapp.thread.AwfulSearch
import com.ferg.awfulapp.thread.AwfulSearchResult
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.widget.SwipyRefreshLayout
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection
import org.apache.commons.lang3.ArrayUtils
import timber.log.Timber
import java.util.*

class SearchFragment : AwfulFragment(), com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout.OnRefreshListener {

    private val mSearchQuery by lazy { view!!.findViewById(R.id.search_query) as EditText }

    private var mQueryId: Int = 0
    private var mMaxPageQueried: Int = 0
    private var mQueryPages: Int = 0

    var searchForums = HashSet<Int>()

    private var mDialog: ProgressDialog? = null
    private val mSearchResultList: RecyclerView by lazy {
        (view!!.findViewById(R.id.search_results) as RecyclerView)
                .apply {
                    adapter = SearchResultAdapter()
                    layoutManager =
                        LinearLayoutManager(context)
                }
    }
    private var mSearchResults: MutableList<AwfulSearch> = mutableListOf()

    private val mSRL: SwipyRefreshLayout by lazy {
        (view!!.findViewById(R.id.search_srl) as SwipyRefreshLayout)
                .apply {
                    setOnRefreshListener(this@SearchFragment)
                    setColorSchemeResources(*ColorProvider.getSRLProgressColors(null))
                    setProgressBackgroundColor(ColorProvider.getSRLBackgroundColor(null))
                    isEnabled = false
                }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.v("onCreate")
        setHasOptionsMenu(true)
        retainInstance = false
    }

    override fun onCreateView(aInflater: LayoutInflater, aContainer: ViewGroup?, aSavedState: Bundle?): View? {
        super.onCreateView(aInflater, aContainer, aSavedState)
        Timber.v("onCreateView")
        return inflateView(R.layout.search, aContainer, aInflater)
    }


    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        if (aSavedState == null) {
            // only parse the intent on the first run, i.e. no saved state
            activity?.intent?.parse()?.let(::handleNavigation)
        }
    }


    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        invalidateOptionsMenu()
    }

    override fun getTitle(): String = getString(R.string.search)


    override fun handleNavigation(event: NavigationEvent): Boolean {
        if (event is NavigationEvent.SearchForums) {
            event.filters.forEach(::addFilter)
            return true
        }
        return false
    }


    private fun search() {
        mDialog = ProgressDialog.show(activity, getString(R.string.search_forums_active_dialog_title), getString(R.string.search_forums_active_dialog_message), true, false)
        val searchForumsPrimitive = ArrayUtils.toPrimitive(searchForums.toTypedArray())
        NetworkUtils.queueRequest(SearchRequest(this.context!!, mSearchQuery.text.toString().toLowerCase(), searchForumsPrimitive)
                .build(null, object : AwfulRequest.AwfulResultCallback<AwfulSearchResult> {
                    override fun success(result: AwfulSearchResult) {
                        removeLoadingDialog()
                        with(result) {
                            if (resultsFound) {
                                mSearchResults = resultList
                                mQueryPages = pages
                                mQueryId = queryId
                                mSearchResultList.adapter?.notifyDataSetChanged()

                                mMaxPageQueried = 1
                                mSRL.isEnabled = (queryId != 0 && mMaxPageQueried < pages)
                            }
                            Timber.e("resultsFound: %s\nmQueryPages: %s\nmQueryId: %s", result.resultsFound, mQueryPages, mQueryId)
                        }
                    }

                    override fun failure(error: VolleyError?) {
                        removeLoadingDialog()
                        Snackbar.make(view!!, R.string.search_forums_failure_message, Snackbar.LENGTH_LONG).setAction("Retry") { search() }.show()
                    }

                    private fun removeLoadingDialog() {
                        // since this is called from network callbacks, need to check the dialog is attached to the window - hopefully this works!
                        mDialog?.run {
                            if (ownerActivity?.isDestroyed != false) dismiss()
                            mDialog = null
                        }
                    }
                }))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater!!.inflate(R.menu.search, menu)
        val filterMenu = menu?.findItem(R.id.search_terms)!!.subMenu
        SearchFilter.FilterType.values().forEach { filterMenu.add(it.label) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.v("onOptionsItemSelected")
        if (item == null) return super.onOptionsItemSelected(item)
        // check if it's one of our filters
        SearchFilter.FilterType.values().firstOrNull { it.label == item.title }?.run {
            showDialog(this@SearchFragment)
            return true
        }

        when (item.itemId) {
            R.id.search_submit -> search()
            R.id.select_forums ->
                SearchForumsFragment(this)
                        .apply { setStyle(DialogFragment.STYLE_NO_TITLE, 0) }
                        .show(fragmentManager!!, "searchforums")
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }


    /**
     * Add a filter to the current set.
     */
    fun addFilter(filter: SearchFilter) {
        // TODO: implement a better UI for this than adding to the text field - e.g. list items that can be swiped away to delete the filter
        with(mSearchQuery) {
            val noCursor = selectionStart == -1 || selectionEnd == -1
            // no cursor? Put it at the end
            val start = if (noCursor) length() else selectionStart
            val end = if (noCursor) start else selectionEnd
            // add the filter string, with convenient spaces around it (kind of a hack but it works)
            val filterString = " $filter "
            text.replace(start, end, filterString)
            // deselect and position the cursor after what we just added (newer APIs do this automatically)
            setSelection(start + filterString.length)
        }
    }


    override fun onRefresh(direction: SwipyRefreshLayoutDirection) {
        Timber.i("onRefresh: %s", mMaxPageQueried)
        val preItemCount = mSearchResultList.adapter?.itemCount ?: 0
        NetworkUtils.queueRequest(SearchResultPageRequest(this.context!!, mQueryId, mMaxPageQueried + 1).build(null, object : AwfulRequest.AwfulResultCallback<ArrayList<AwfulSearch>> {

            // TODO: combine this with #search since they share functionality - maybe a SearchQuery object for the current query that holds this state we're changing
            override fun success(result: ArrayList<AwfulSearch>) {
                mSearchResults.addAll(result)
                mMaxPageQueried++
                if (mMaxPageQueried >= mQueryPages) {
                    mSRL.isEnabled = false
                }
                mSearchResultList.adapter?.notifyDataSetChanged()
                mSRL.isRefreshing = false
                mSearchResultList.smoothScrollToPosition(preItemCount + 1)
            }

            override fun failure(error: VolleyError?) {
                mSRL.isRefreshing = false
            }
        }))
    }

    private inner class SearchResultHolder internal constructor(internal val self: View) : RecyclerView.ViewHolder(self) {
        internal val threadName: TextView = itemView.findViewById(R.id.search_result_threadname)
        internal val hitInfo: TextView = itemView.findViewById(R.id.search_result_hit_info)
        internal val blurb: TextView = itemView.findViewById(R.id.search_result_blurb)
        internal val timestamp: TextView = itemView.findViewById(R.id.search_result_timestamp)

    }


    private inner class SearchResultAdapter : RecyclerView.Adapter<SearchResultHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.search_result_item, parent, false)
            return SearchResultHolder(view)
        }

        override fun onBindViewHolder(holder: SearchResultHolder, position: Int) {
            val result = mSearchResults[position]
            with(holder) {
                threadName.text = result.threadTitle
                hitInfo.text = Html.fromHtml("<b>${result.username}</b> in <b>${result.forumTitle}</b>")
                blurb.text = Html.fromHtml(result.blurb)
                timestamp.text = result.postDate

                val threadLink = result.threadLink
                val forumId = result.forumId
                self.setOnClickListener {
                    AwfulURL.parse(Constants.BASE_URL + threadLink).let(NavigationEvent::Url).let(::navigate)
                }
            }
        }

        override fun getItemCount() = mSearchResults.size
    }
}