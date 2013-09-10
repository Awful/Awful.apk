/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ferg.awful.widget;

import pl.polidea.treeview.TreeViewList;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;

import com.handmark.pulltorefresh.library.OverscrollHelper;
import com.handmark.pulltorefresh.library.PullToRefreshAdapterViewBase;
import com.handmark.pulltorefresh.library.internal.EmptyViewMethodAccessor;

public class PullToRefreshTreeView extends PullToRefreshAdapterViewBase<TreeViewList> {

	public PullToRefreshTreeView(Context context) {
		super(context);
	}

	public PullToRefreshTreeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PullToRefreshTreeView(Context context, Mode mode) {
		super(context, mode);
	}

    @Override
    public Orientation getPullToRefreshScrollDirection() {
        return Orientation.VERTICAL;
    }

    @Override
	public ContextMenuInfo getContextMenuInfo() {
		return ((InternalTreeViewList) getRefreshableView()).getContextMenuInfo();
	}

	@Override
	protected final TreeViewList createRefreshableView(Context context, AttributeSet attrs) {
		final TreeViewList lv;
		if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
			lv = new InternalTreeViewListSDK9(context, attrs);
		} else {
			lv = new InternalTreeViewList(context, attrs);
		}

		// Set it to this so it can be used in ListActivity/ListFragment
		lv.setId(android.R.id.list);
		return lv;
	}

	class InternalTreeViewList extends TreeViewList implements EmptyViewMethodAccessor {

		public InternalTreeViewList(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public ContextMenuInfo getContextMenuInfo() {
			return super.getContextMenuInfo();
		}

		@Override
		public void setEmptyView(View emptyView) {
			PullToRefreshTreeView.this.setEmptyView(emptyView);
		}

		@Override
		public void setEmptyViewInternal(View emptyView) {
			super.setEmptyView(emptyView);
		}
	}

	@TargetApi(9)
	final class InternalTreeViewListSDK9 extends InternalTreeViewList {

		public InternalTreeViewListSDK9(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
				int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {

            final boolean returnValue = super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
                    scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);

            // Does all of the hard work...
            OverscrollHelper.overScrollBy(PullToRefreshTreeView.this, deltaX, scrollX, deltaY, scrollY,
                    isTouchEvent);

            return returnValue;
		}
	}
}
