/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
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
 *******************************************************************************/

package com.ferg.awfulapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.thread.AwfulAction;
import com.ferg.awfulapp.thread.AwfulMessage;

import java.util.ArrayList;

public class PostActionsFragment extends AwfulDialogFragment {
	private final static String TAG = "PostActionsFragment";

	private String title;

	private ThreadDisplayFragment parent;
	private RecyclerView actionsView;
	private ArrayList<AwfulAction> actions;
	private String postId;
	private String username;
	private String userId;
	private String lastReadUrl;
	private int threadId;
	private String url;


    @Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View result = inflateView(R.layout.select_action_dialog, container, inflater);
		TextView actionTitle = (TextView) result.findViewById(R.id.actionTitle);
		actionTitle.setMovementMethod(new ScrollingMovementMethod());
		actionTitle.setText(title);
		actionsView = (RecyclerView) result.findViewById(R.id.post_actions);
		actionsView.setAdapter(new RecyclerView.Adapter<ActionHolder>() {
			@Override
			public ActionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
				View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.action_item, parent, false);
				return new ActionHolder(view);
			}

			@Override
			public void onBindViewHolder(ActionHolder holder, final int position) {
				final AwfulAction action = actions.get(position);
				holder.actionText.setText(action.getActionTitle());
				holder.actionText.setTextColor(ColorProvider.PRIMARY_TEXT.getColor());
				holder.actionTag.setImageResource(action.getActionIcon());
				holder.actionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						callAction(action.getActionType());
					}
				});
			}

			@Override
			public int getItemCount() {
				if (actions != null) {
					return actions.size();
				}
				return 0;
			}
		});
		actionsView.setLayoutManager(new LinearLayoutManager(getContext()));
		getDialog().setCanceledOnTouchOutside(true);

		return result;
	}

	private void callAction(AwfulAction.ActionType actionType) {
		switch(actionType){
			case SEND_PM:
				startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_USERNAME, username));
				break;
			case QUOTE:
				parent.displayPostReplyDialog(threadId, Integer.parseInt(postId), AwfulMessage.TYPE_QUOTE);
				break;
			case EDIT:
				parent.displayPostReplyDialog(threadId, Integer.parseInt(postId), AwfulMessage.TYPE_EDIT);
				break;
			case MARK_LAST_SEEN:
				parent.markLastRead(Integer.parseInt(lastReadUrl));
				break;
			case COPY_URL:
				parent.copyThreadURL(postId);
				break;
			case USER_POSTS:
				parent.toggleUserPosts(postId, userId, username);
				break;
			case MARK_USER:
				parent.toggleMarkUser(username);
				break;
			case IGNORE_USER:
				parent.ignoreUser(userId);
				break;
			case REPORT_POST:
				parent.reportUser(postId);
				break;
			case DOWNLOAD_IMAGE:
				parent.enqueueDownload(Uri.parse(url));
				break;
			case SHOW_INLINE:
				parent.showImageInline(url);
				break;
			case COPY_LINK_URL:
				parent.copyToClipboard(url);
				displayAlert(R.string.copy_url_success, 0, R.drawable.ic_insert_link);
				break;
			case OPEN_URL:
				parent.startUrlIntent(url);
				break;
			case SHARE_URL:
				startActivity(parent.createShareIntent(url));
				break;
			case DISPLAY_IMAGE:
				parent.displayImage(url);
				break;
		}
		this.dismiss();
	}


	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public boolean volumeScroll(KeyEvent event) {
		return false;
	}

	public void setParent(ThreadDisplayFragment frag){
		parent = frag;
	}

	public void setActions(ArrayList<AwfulAction> actions){
		this.actions = actions;
	}


	private class ActionHolder extends RecyclerView.ViewHolder {
		final ImageView actionTag;
		final TextView actionText;
		final View actionView;
		public ActionHolder(View view) {
			super(view);
			actionView = view;
			actionText = (TextView) itemView.findViewById(R.id.actionTitle);
			actionTag = (ImageView) itemView.findViewById(R.id.actionTag);
		}
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setSize(String size) {
		View view = getView();
		if (view != null) {
			TextView fileSize = (TextView) view.findViewById(R.id.fileSize);
			if (fileSize != null) {
				fileSize.setText(size);
			}
		}
	}

	public void setPostId(String postId) {
		this.postId = postId;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}

	public void setLastReadUrl(String lastReadUrl) {
		this.lastReadUrl = lastReadUrl;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
