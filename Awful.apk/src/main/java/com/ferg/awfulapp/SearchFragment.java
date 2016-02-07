/**
 * *****************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * <p/>
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
 * <p/>
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

package com.ferg.awfulapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.RedirectTask;
import com.ferg.awfulapp.task.SearchRequest;
import com.ferg.awfulapp.task.SearchResultRequest;
import com.ferg.awfulapp.thread.AwfulSearch;
import com.ferg.awfulapp.thread.AwfulSearchResult;
import com.ferg.awfulapp.thread.AwfulURL;
import com.ferg.awfulapp.util.AwfulError;
import com.ferg.awfulapp.widget.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.HashSet;

public class SearchFragment extends AwfulFragment implements SwipyRefreshLayout.OnRefreshListener{
    private static final String TAG = "SearchFragment";

    private EditText mSearchQuery;

    private int mQueryId;
    private int mMaxPageQueried;
    private int mQueryPages;

    public HashSet<Integer> searchForums = new HashSet<>();

    private ProgressDialog mDialog;
    private RecyclerView mSearchResultList;
    private ArrayList<AwfulSearch> mSearchResults;
    private SwipyRefreshLayout mSRL;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.e(TAG, "onCreate");
        setHasOptionsMenu(true);
        setRetainInstance(false);
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);
        if (DEBUG) Log.e(TAG, "onCreateView");

        View result = inflateView(R.layout.search, aContainer, aInflater);
        mSearchQuery = (EditText) result.findViewById(R.id.search_query);

        mSRL = (SwipyRefreshLayout) result.findViewById(R.id.search_srl);
        mSRL.setOnRefreshListener(this);
        mSRL.setColorSchemeResources(
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light,
                android.R.color.holo_blue_bright);
        mSRL.setEnabled(false);

        mSearchResultList = (RecyclerView) result.findViewById(R.id.search_results);
        mSearchResultList.setAdapter(new RecyclerView.Adapter<SearchResultHolder>() {
            @Override
            public SearchResultHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.search_result_item, parent, false);
                return new SearchResultHolder(view);
            }

            @Override
            public void onBindViewHolder(SearchResultHolder holder, final int position) {
                AwfulSearch search = mSearchResults.get(position);
                holder.threadName.setText(search.getThreadTitle());
                holder.hitInfo.setText(Html.fromHtml("<b>" + search.getUsername()+"</b> in <b>" + search.getForumTitle()+"</b>"));
                holder.blurb.setText(Html.fromHtml(search.getBlurb()));
                holder.threadName.setText(search.getThreadTitle());


                final String threadlink = search.getThreadLink();
                final int forumId = search.getForumId();
                final ProgressDialog redirectDialog = new ProgressDialog(getContext());
                final RedirectTask redirect = new RedirectTask(Constants.BASE_URL + threadlink) {
                    @Override
                    protected void onPostExecute(String url) {
                        if (!isCancelled()) {
                            if (url != null) {
                                AwfulURL result = AwfulURL.parse(url);
                                Activity activity = getActivity();
                                Intent openThread = new Intent().setClass(activity, ForumsIndexActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        .putExtra(Constants.THREAD_ID, (int) result.getId())
                                        .putExtra(Constants.THREAD_PAGE, (int) result.getPage())
                                        .putExtra(Constants.FORUM_ID, forumId)
                                        .putExtra(Constants.FORUM_PAGE, 1)
                                        .putExtra(Constants.THREAD_FRAGMENT, result.getFragment().substring(4));
                                redirectDialog.dismiss();
                                activity.finish();
                                startActivity(openThread);
                            } else {
                                displayAlert(new AwfulError());
                            }
                        }
                    }
                };
                holder.self.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        if (getActivity() != null) {
                            if(redirect.getStatus() == AsyncTask.Status.PENDING){
                                redirect.execute();
                                redirectDialog.setMessage("Just a second");
                                redirectDialog.setTitle("Loading");
                                redirectDialog.setIndeterminate(true);
                                redirectDialog.setCancelable(false);
                                redirectDialog.show();
                            }
                        }


                    }
                });
            }

            @Override
            public int getItemCount() {
                if (mSearchResults != null) {
                    return mSearchResults.size();
                }
                return 0;
            }
        });
        mSearchResultList.setLayoutManager(new LinearLayoutManager(getContext()));
        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
        if (DEBUG) Log.e(TAG, "onActivityCreated");

    }


    @Override
    public void onPreferenceChange(AwfulPreferences prefs, String key) {
        super.onPreferenceChange(prefs, key);
        //refresh the menu to show/hide attach option (plat only)
        invalidateOptionsMenu();
    }

    @Override
    public String getTitle() {
        return getString(R.string.search);
    }



    private void search() {
        mDialog = ProgressDialog.show(getActivity(), "Loading", "Searching...", true, false);
        Integer[] searchforums = new Integer[]{};
        int[] searchforumsprimitive = ArrayUtils.toPrimitive(searchForums.toArray(searchforums));
        NetworkUtils.queueRequest(new SearchRequest(this.getContext(), mSearchQuery.getText().toString().toLowerCase(), searchforumsprimitive).build(null, new AwfulRequest.AwfulResultCallback<AwfulSearchResult>() {
            @Override
            public void success(AwfulSearchResult result) {
                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                }
                if(result.getQueryId() != 0){
                    mSearchResults = result.getResultList();
                    mQueryPages = result.getPages();
                    mQueryId = result.getQueryId();
                    mSearchResultList.getAdapter().notifyDataSetChanged();

                    mMaxPageQueried = 1;
                    if(mMaxPageQueried < result.getPages()){
                        mSRL.setEnabled(true);
                    }
                }
                Log.e(TAG,"mQueryPages: "+mQueryPages+ " mQueryId: "+mQueryId);
            }

            @Override
            public void failure(VolleyError error) {
                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                }
                Snackbar.make(getView(), "Searching failed.", Snackbar.LENGTH_LONG)
                        .setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                search();
                            }

                        }).show();
            }
        }));
    }


    @Override
    public void onPageVisible() {

    }

    @Override
    public void onPageHidden() {
    }

    @Override
    public String getInternalId() {
        return null;
    }


    @Override
    public boolean volumeScroll(KeyEvent event) {
        //I don't think that's necessary
        return false;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.e(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {

            case R.id.search_submit:
                search();
                break;
            case R.id.search_forums:
                new SearchForumsFragment(this).show(getFragmentManager(), "searchforums");
                break;
            case R.id.search_threadid:
                insertSearchTerm(SEARCHTERM.THREADID);
                break;
            case R.id.search_intitle:
                insertSearchTerm(SEARCHTERM.INTITLE);
                break;
            case R.id.search_userid:
                insertSearchTerm(SEARCHTERM.USERID);
                break;
            case R.id.search_quoting:
                insertSearchTerm(SEARCHTERM.QUOTING);
                break;
            case R.id.search_username:
                insertSearchTerm(SEARCHTERM.USERNAME);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void insertSearchTerm(SEARCHTERM term) {
        int selectionStart = mSearchQuery.getSelectionStart();
        switch (term) {
            case INTITLE:
                mSearchQuery.getEditableText().insert(selectionStart, " intitle:\"\" ");
                mSearchQuery.setSelection(selectionStart + " intitle:\"".length());
                break;
            case THREADID:
                mSearchQuery.getEditableText().insert(selectionStart, " threadid: ");
                mSearchQuery.setSelection(selectionStart + " threadid:".length());
                break;
            case USERID:
                mSearchQuery.getEditableText().insert(selectionStart, " userid: ");
                mSearchQuery.setSelection(selectionStart + " userid:".length());
                break;
            case USERNAME:
                mSearchQuery.getEditableText().insert(selectionStart, " username:\"\" ");
                mSearchQuery.setSelection(selectionStart + " username:\"".length());
                break;
            case QUOTING:
                mSearchQuery.getEditableText().insert(selectionStart, " quoting:\"\" ");
                mSearchQuery.setSelection(selectionStart + " quoting:\"".length());
                break;
        }
    }

    @Override
    public void onRefresh(SwipyRefreshLayoutDirection direction) {
        Log.e(TAG,"onRefresh: "+ mMaxPageQueried+ " ");
        final int preItemCount = mSearchResultList.getAdapter().getItemCount();
        NetworkUtils.queueRequest(new SearchResultRequest(this.getContext(), mQueryId, (mMaxPageQueried+1)).build(null, new AwfulRequest.AwfulResultCallback<ArrayList<AwfulSearch>>() {

            @Override
            public void success(ArrayList<AwfulSearch> result) {
                mSearchResults.addAll(result);
                mMaxPageQueried++;
                if(mMaxPageQueried >= mQueryPages){
                    mSRL.setEnabled(false);
                }
                mSearchResultList.getAdapter().notifyDataSetChanged();
                mSRL.setRefreshing(false);
                mSearchResultList.smoothScrollToPosition(preItemCount+1);
            }

            @Override
            public void failure(VolleyError error) {
                mSRL.setRefreshing(false);
            }
        }));
    }

    private class SearchResultHolder extends RecyclerView.ViewHolder {
        final TextView threadName;
        final TextView hitInfo;
        final TextView blurb;
        final View self;
        public SearchResultHolder(View view) {
            super(view);
            self = view;
            threadName = (TextView) itemView.findViewById(R.id.search_result_threadname);
            hitInfo = (TextView) itemView.findViewById(R.id.search_result_hit_info);
            blurb = (TextView) itemView.findViewById(R.id.search_result_blurb);
        }
    }

    private enum SEARCHTERM {INTITLE,THREADID,USERID,USERNAME,QUOTING}
}
