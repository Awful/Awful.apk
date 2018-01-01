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

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.android.volley.VolleyError
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.SearchForumsRequest
import com.ferg.awfulapp.thread.AwfulSearchForum
import java.util.*

class SearchForumsFragment() : AwfulDialogFragment() {

    lateinit private var parent: SearchFragment
    private var forums: ArrayList<AwfulSearchForum> = ArrayList()

    @BindView(R.id.select_forums)           lateinit var mSearchForums: RecyclerView
    @BindView(R.id.search_forums_progress)  lateinit var mProgress: ProgressBar

    @SuppressLint("ValidFragment")
    constructor(parent: SearchFragment) : this() {
        this.parent = parent
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        dialog.setTitle(title)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val result = inflateView(R.layout.search_forums_dialog, container, inflater)
        ButterKnife.bind(this, view!!)
        mSearchForums.adapter = object : RecyclerView.Adapter<SearchHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHolder {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.search_forum_item, parent, false)
                return SearchHolder(view)
            }

            override fun onBindViewHolder(holder: SearchHolder, position: Int) {
                val searchForum = forums[position]
                holder.forumName.text = searchForum.forumName
                holder.forumCheckbox.isChecked = searchForum.isChecked
                holder.forumCheckbox.tag = searchForum
                val self = this
                holder.forumCheckbox.setOnClickListener { v ->
                    val cb = v as CheckBox
                    val forum = cb.tag as AwfulSearchForum

                    forum.isChecked = cb.isChecked
                    searchForum.isChecked = cb.isChecked
                    if (cb.isChecked) {
                        parent.searchForums.add(forum.forumId)
                    } else {
                        parent.searchForums.remove(forum.forumId)
                    }
                    if (searchForum.depth < 3) {
                        for (childSearchForum in forums) {
                            if (childSearchForum.depth > searchForum.depth || searchForum.forumId == -1) {
                                if (childSearchForum.parents.contains("parent" + searchForum.forumId) || searchForum.forumId == -1) {
                                    childSearchForum.isChecked = searchForum.isChecked
                                    if (searchForum.isChecked) {
                                        parent.searchForums.add(childSearchForum.forumId)
                                    } else {
                                        parent.searchForums.remove(childSearchForum.forumId)
                                    }
                                }
                            }
                        }
                        self.notifyDataSetChanged()
                    }
                }
            }

            override fun getItemCount() = forums.size
        }
        mSearchForums.layoutManager = LinearLayoutManager(context)

        getForums()

        return result
    }

    override fun getTitle(): String {
        return getString(R.string.search_forums_select_forums)
    }

    override fun volumeScroll(event: KeyEvent): Boolean {
        return false
    }

    fun getForums() {
        NetworkUtils.queueRequest(SearchForumsRequest(this.context).build(null, object : AwfulRequest.AwfulResultCallback<ArrayList<AwfulSearchForum>> {

            override fun success(result: ArrayList<AwfulSearchForum>) {
                forums = result
                for (searchForum in forums) {
                    searchForum.isChecked = parent.searchForums.contains(searchForum.forumId)
                }
                mProgress.visibility = View.GONE
                mSearchForums.visibility = View.VISIBLE
                mSearchForums.adapter?.notifyDataSetChanged()
            }

            override fun failure(error: VolleyError) {
                Snackbar.make(if (view != null) view!! else parent.view!!, R.string.search_forums_failure_message, Snackbar.LENGTH_LONG)
                        .setAction("Retry") { getForums() }.show()
            }
        }))
    }

    private inner class SearchHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        internal val forumName: TextView = itemView.findViewById(R.id.search_forum_name)
        internal val forumCheckbox: CheckBox = itemView.findViewById(R.id.search_forum_checkbox)
    }
}
