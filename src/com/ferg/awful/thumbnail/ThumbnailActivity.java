/* Copyright (c) 2008-09 -- CommonsWare, LLC

	 Licensed under the Apache License, Version 2.0 (the "License");
	 you may not use this file except in compliance with the License.
	 You may obtain a copy of the License at

		 http://www.apache.org/licenses/LICENSE-2.0

	 Unless required by applicable law or agreed to in writing, software
	 distributed under the License is distributed on an "AS IS" BASIS,
	 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 See the License for the specific language governing permissions and
	 limitations under the License.
*/
	 
package com.ferg.awful.thumbnail;

import android.app.ListActivity;
import android.widget.ListAdapter;
import com.commonsware.cwac.cache.SimpleWebImageCache;

abstract public class ThumbnailActivity extends ListActivity {
	abstract protected int[] getImageIdArray();
	
	private ThumbnailBus bus=new ThumbnailBus();
	private SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> cache=
							new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(null, null, 101, bus);
							
	@Override
	public void setListAdapter(ListAdapter adapter) {
		super.setListAdapter(new ThumbnailAdapter(this,
																							adapter,
																							cache,
																							getImageIdArray()));
	}
}
