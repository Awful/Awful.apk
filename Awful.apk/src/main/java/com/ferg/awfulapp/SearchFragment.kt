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
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.EditText
import android.widget.TextView
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.RedirectTask
import com.ferg.awfulapp.task.SearchRequest
import com.ferg.awfulapp.task.SearchResultRequest
import com.ferg.awfulapp.thread.AwfulSearch
import com.ferg.awfulapp.thread.AwfulSearchResult
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.util.AwfulUtils
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection
import org.apache.commons.lang3.ArrayUtils
import timber.log.Timber
import java.util.*

class SearchFragment : AwfulFragment(), SwipyRefreshLayout.OnRefreshListener {

    private var mQueryId: Int = 0
    private var mMaxPageQueried: Int = 0
    private var mQueryPages: Int = 0

    var searchForums = HashSet<Int>()

    lateinit private var mDialog: ProgressDialog
    lateinit private var mSearchResultList: RecyclerView
    lateinit private var mSearchQuery: EditText
    lateinit private var refreshLayout: SwipyRefreshLayout

    private var mSearchResults: ArrayList<AwfulSearch> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.v("onCreate")
        setHasOptionsMenu(true)
        retainInstance = false
    }

    override fun onCreateView(aInflater: LayoutInflater, aContainer: ViewGroup?, aSavedState: Bundle?): View? {
        super.onCreateView(aInflater, aContainer, aSavedState)
        val result = inflateView(R.layout.search, aContainer, aInflater)

        mSearchQuery = result.findViewById(R.id.search_query)
        refreshLayout = result.findViewById(R.id.search_srl)
        refreshLayout.setOnRefreshListener(this)
        refreshLayout.setColorSchemeResources(*ColorProvider.getSRLProgressColors(null))
        refreshLayout.setProgressBackgroundColor(ColorProvider.getSRLBackgroundColor(null))
        refreshLayout.isEnabled = false

        mSearchResultList = result.findViewById(R.id.search_results)
        mSearchResultList.adapter = object : RecyclerView.Adapter<SearchResultHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultHolder {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.search_result_item, parent, false)
                return SearchResultHolder(view)
            }

            override fun onBindViewHolder(holder: SearchResultHolder, position: Int) {
                val search = mSearchResults[position]
                holder.threadName.text = search.threadTitle
                holder.hitInfo.text = AwfulUtils.fromHtml("<b>" + search.username + "</b> in <b>" + search.forumTitle + "</b>")
                holder.blurb.text = AwfulUtils.fromHtml(search.blurb)
                holder.threadName.text = search.threadTitle
                holder.timestamp.text = search.postDate


                val threadlink = search.threadLink
                val forumId = search.forumId
                val redirectDialog = ProgressDialog(context)
                // TODO: Get rid of disgusting async task
                val redirect = object : RedirectTask(Constants.BASE_URL + threadlink) {
                    override fun onPostExecute(url: String?) {
                        if (!isCancelled) {
                            if (url != null) {
                                val result = AwfulURL.parse(url)
                                val activity = activity
                                val openThread = Intent().setClass(activity!!, ForumsIndexActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        .putExtra(Constants.THREAD_ID, result.id.toInt())
                                        .putExtra(Constants.THREAD_PAGE, result.page.toInt())
                                        .putExtra(Constants.FORUM_ID, forumId)
                                        .putExtra(Constants.FORUM_PAGE, 1)
                                        .putExtra(Constants.THREAD_FRAGMENT, result.fragment.substring(4))
                                redirectDialog.dismiss()
                                activity.finish()
                                startActivity(openThread)
                            } else {
                                AlertBuilder().fromError(AwfulError()).show()
                            }
                        }
                    }
                }
                holder.self.setOnClickListener {
                    if (activity != null) {
                        if (redirect.status == AsyncTask.Status.PENDING) {
                            redirect.execute()
                            redirectDialog.setMessage("Just a second")
                            redirectDialog.setTitle("Loading")
                            redirectDialog.isIndeterminate = true
                            redirectDialog.setCancelable(false)
                            redirectDialog.show()
                        }
                    }
                }
            }

            override fun getItemCount() = mSearchResults.size
        }
        mSearchResultList.layoutManager = LinearLayoutManager(context)
        return result
    }


    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        //refresh the menu to show/hide attach option (plat only)
        invalidateOptionsMenu()
    }

    override fun getTitle() = getString(R.string.search)


    private fun search() {
        mDialog = ProgressDialog.show(activity, getString(R.string.search_forums_active_dialog_title), getString(R.string.search_forums_active_dialog_message), true, false)
        val searchForumsPrimitive = ArrayUtils.toPrimitive(this.searchForums.toTypedArray())
        val query = mSearchQuery.text.toString().toLowerCase()
        NetworkUtils.queueRequest(SearchRequest(this.context, query, searchForumsPrimitive)
                .build(null, object : AwfulRequest.AwfulResultCallback<AwfulSearchResult> {
            override fun success(result: AwfulSearchResult) {
                mDialog.dismiss()

                if (result.queryId != 0) {
                    mSearchResults = result.resultList
                    mQueryPages = result.pages
                    mQueryId = result.queryId
                    mSearchResultList.adapter.notifyDataSetChanged()

                    mMaxPageQueried = 1
                    if (mMaxPageQueried < result.pages) {
                        refreshLayout.isEnabled = true
                    }
                }
                Timber.i("mQueryPages: $mQueryPages mQueryId: $mQueryId")
            }

            override fun failure(error: VolleyError) {
                mDialog.dismiss()
                Snackbar.make(view!!, R.string.search_forums_failure_message, Snackbar.LENGTH_LONG)
                        .setAction("Retry") { search() }.show()
            }
        }))
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.search, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.v("onOptionsItemSelected")
        when (item.itemId) {
            R.id.search_submit -> search()
            R.id.select_forums -> {
                val frag = SearchForumsFragment(this)
                frag.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
                frag.show(fragmentManager!!, "searchforums")
            }
            R.id.search_threadid -> insertSearchTerm(SEARCHTERM.THREADID)
            R.id.search_intitle -> insertSearchTerm(SEARCHTERM.INTITLE)
            R.id.search_userid -> insertSearchTerm(SEARCHTERM.USERID)
            R.id.search_quoting -> insertSearchTerm(SEARCHTERM.QUOTING)
            R.id.search_username -> insertSearchTerm(SEARCHTERM.USERNAME)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun insertSearchTerm(term: SEARCHTERM) {
        val selectionStart = mSearchQuery.selectionStart
        when (term) {
            SearchFragment.SEARCHTERM.INTITLE -> {
                mSearchQuery.editableText.insert(selectionStart, " intitle:\"\" ")
                mSearchQuery.setSelection(selectionStart + " intitle:\"".length)
            }
            SearchFragment.SEARCHTERM.THREADID -> {
                mSearchQuery.editableText.insert(selectionStart, " threadid: ")
                mSearchQuery.setSelection(selectionStart + " threadid:".length)
            }
            SearchFragment.SEARCHTERM.USERID -> {
                mSearchQuery.editableText.insert(selectionStart, " userid: ")
                mSearchQuery.setSelection(selectionStart + " userid:".length)
            }
            SearchFragment.SEARCHTERM.USERNAME -> {
                mSearchQuery.editableText.insert(selectionStart, " username:\"\" ")
                mSearchQuery.setSelection(selectionStart + " username:\"".length)
            }
            SearchFragment.SEARCHTERM.QUOTING -> {
                mSearchQuery.editableText.insert(selectionStart, " quoting:\"\" ")
                mSearchQuery.setSelection(selectionStart + " quoting:\"".length)
            }
        }
    }

    override fun onRefresh(direction: SwipyRefreshLayoutDirection) {
       Timber.v("onRefresh: $mMaxPageQueried")
        val preItemCount = mSearchResultList.adapter.itemCount
        NetworkUtils.queueRequest(SearchResultRequest(this.context, mQueryId, mMaxPageQueried + 1)
                .build(null, object : AwfulRequest.AwfulResultCallback<ArrayList<AwfulSearch>> {

            override fun success(result: ArrayList<AwfulSearch>) {
                mSearchResults.addAll(result)
                mMaxPageQueried++
                if (mMaxPageQueried >= mQueryPages) {
                    refreshLayout.isEnabled = false
                }
                mSearchResultList.adapter.notifyDataSetChanged()
                refreshLayout.isRefreshing = false
                mSearchResultList.smoothScrollToPosition(preItemCount + 1)
            }

            override fun failure(error: VolleyError) {
                refreshLayout.isRefreshing = false
            }
        }))
    }

    private inner class SearchResultHolder internal constructor(internal val self: View) : RecyclerView.ViewHolder(self) {
        internal val threadName: TextView = itemView.findViewById(R.id.search_result_threadname)
        internal val hitInfo: TextView = itemView.findViewById(R.id.search_result_hit_info)
        internal val blurb: TextView = itemView.findViewById(R.id.search_result_blurb)
        internal val timestamp: TextView = itemView.findViewById(R.id.search_result_timestamp)
    }

    private enum class SEARCHTERM {
        INTITLE, THREADID, USERID, USERNAME, QUOTING
    }
}
