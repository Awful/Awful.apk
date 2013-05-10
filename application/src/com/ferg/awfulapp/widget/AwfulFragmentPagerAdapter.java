package com.ferg.awfulapp.widget;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Iterator;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.ferg.awfulapp.constants.Constants;

/**
 * Implementation of {@link android.support.v4.view.PagerAdapter} that
 * represents each page as a {@link Fragment} that is persistently
 * kept in the fragment manager as long as the user can return to the page.
 *
 * <p>This version of the pager is best for use when there are a handful of
 * typically more static fragments to be paged through, such as a set of tabs.
 * The fragment of each page the user visits will be kept in memory, though its
 * view hierarchy may be destroyed when not visible.  This can result in using
 * a significant amount of memory since fragment instances can hold on to an
 * arbitrary amount of state.  For larger sets of pages, consider
 * {@link FragmentStatePagerAdapter}.
 *
 * <p>When using FragmentPagerAdapter the host ViewPager must have a
 * valid ID set.</p>
 *
 * <p>Subclasses only need to implement {@link #getItem(int)}
 * and {@link #getCount()} to have a working adapter.
 *
 * <p>Here is an example implementation of a pager containing fragments of
 * lists:
 *
 * {@sample development/samples/Support4Demos/src/com/example/android/supportv4/app/FragmentPagerSupport.java
 *      complete}
 *
 * <p>The <code>R.layout.fragment_pager</code> resource of the top-level fragment is:
 *
 * {@sample development/samples/Support4Demos/res/layout/fragment_pager.xml
 *      complete}
 *
 * <p>The <code>R.layout.fragment_pager_list</code> resource containing each
 * individual fragment's layout is:
 *
 * {@sample development/samples/Support4Demos/res/layout/fragment_pager_list.xml
 *      complete}
 */
public abstract class AwfulFragmentPagerAdapter extends AwfulPagerAdapter implements Iterable<AwfulFragmentPagerAdapter.AwfulPagerFragment> {

	private static final String TAG = "FragmentPagerAdapter";
    private static final boolean DEBUG = Constants.DEBUG;
    
    private boolean splitMode = false;

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;
    private Fragment mCurrentPrimaryItem = null;
    
    private ArrayList<AwfulPagerFragment> fragList;
    private ArrayList<AwfulPagerFragment> splitFragList;

    public AwfulFragmentPagerAdapter(FragmentManager fm, boolean widescreen) {
        mFragmentManager = fm;
		fragList = new ArrayList<AwfulPagerFragment>();
		splitFragList = new ArrayList<AwfulPagerFragment>();
		splitMode = !widescreen;
    }
    

	public void addFragment(AwfulPagerFragment frag){
		if(!fragList.contains(frag)){
			fragList.add(frag);
			if(frag instanceof AwfulDualPaneView){
				splitFragList.add(((AwfulDualPaneView)frag).getFirst());
				splitFragList.add(((AwfulDualPaneView)frag).getSecond());
			}else{
				splitFragList.add(frag);
			}
			notifyDataSetChanged();
		}
	}
	
	public void deleteFragment(AwfulPagerFragment frag){
		//TODO rewrite this before use to handle dualpaneview
		assert(false);
		fragList.remove(frag);
		splitFragList.remove(frag);
		notifyDataSetChanged();
	}

	public AwfulPagerFragment deletePage(int x) {
		//TODO rewrite this before use
		assert(false);
		AwfulPagerFragment tmp = fragList.remove(x);
		if(tmp instanceof AwfulDualPaneView){
			splitFragList.remove(((AwfulDualPaneView)tmp).getFirst());
			splitFragList.remove(((AwfulDualPaneView)tmp).getSecond());
		}else{
			splitFragList.remove(tmp);
		}
		notifyDataSetChanged();
		return tmp;
	}
	
    @Override
	public Iterator<AwfulPagerFragment> iterator() {
    	if(splitMode){
    		return splitFragList.iterator();
    	}else{
    		return fragList.iterator();
    	}
	}

    /**
     * Return the Fragment associated with a specified position.
     */
	public AwfulPagerFragment getItem(int position) {
		AwfulPagerFragment frag;
		if(splitMode){
			frag = splitFragList.get(position);
		}else{
			frag = fragList.get(position);
		}
		if(DEBUG) Log.w(TAG,"getItem "+position+" - "+frag.toString());
		return frag;
	}
	
    public interface AwfulPagerFragment{
    	/**
    	 * This event is called when the user presses the back button. Return true to consume this back-button event and prevent the activity from finishing.
    	 * 
    	 * @return true if you are consuming this back-button event.
    	 */
    	public boolean onBackPressed();
    	public void onPageVisible();
    	public void onPageHidden();
    	public String getTitle();
    	/**
    	 * Check to see if this point is horizontally scrollable, and by X distance.
    	 * @param dx Distance on X axis to scroll.
    	 * @param y Position on Y axis where the event was triggered.
    	 * @return True to allow horizontal scrolling, false to allow viewpager to take over.
    	 */
    	public boolean canScrollX(int dx, int y);
    	public int getProgressPercent();
    	public void fragmentMessage(String type, String contents);
    	public boolean canSplitscreen();
		public String getInternalId();
		public boolean volumeScroll(KeyEvent event);
    }

    @Override
    public void startUpdate(ViewGroup container) {
    	if(DEBUG) Log.w(TAG,"startUpdate: "+container);
    }
    
    protected abstract Fragment resolveConflict(int position, Fragment oldFrag, Fragment newFrag);

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        // Do we already have this fragment?
        AwfulPagerFragment listItem = getItem(position);
        Fragment listFragment = getFrag(listItem, true),listFragmentB = getFrag(listItem, false);
        String name = makeFragmentName(listFragment);
        String nameB = makeFragmentName(listFragmentB);
        Fragment existingFragment = null;
        Fragment existingFragmentB = null;
        
        if(listItem instanceof AwfulDualPaneView){
        	existingFragment = mFragmentManager.findFragmentByTag(name);
        	existingFragmentB = mFragmentManager.findFragmentByTag(nameB);
        	listFragment = (Fragment) ((AwfulDualPaneView)listItem).getFirst();
        	listFragmentB = (Fragment) ((AwfulDualPaneView)listItem).getSecond();
        	container.addView((View) listItem);
        }else if(listItem instanceof Fragment){
        	listFragment = (Fragment) listItem;
        	existingFragment = mFragmentManager.findFragmentByTag(name);
        }
        if (existingFragment == listFragment) {
            if (DEBUG) Log.w(TAG, "Attaching item #" + position + ": f=" + existingFragment);
            mCurTransaction.attach(existingFragment);
        } else {
        	if(existingFragment != null){
            	mCurTransaction.remove(existingFragment);
        	}
            if (DEBUG) Log.w(TAG, "Adding item #" + position + ": f=" + listFragment);
            if(listItem instanceof AwfulDualPaneView){
	            mCurTransaction.add(listFragment, makeFragmentName(listFragment));
            }else{
	            mCurTransaction.add(container.getId(), listFragment,
	                    makeFragmentName(listFragment));
            }
        }
        if (listFragmentB != null){
        	if(existingFragmentB == listFragmentB) {
	            if (DEBUG) Log.w(TAG, "Attaching item #" + position + ": f=" + existingFragmentB);
	            mCurTransaction.attach(existingFragmentB);
	        } else {
	            if (DEBUG) Log.w(TAG, "Adding item #" + position + ": f=" + listFragmentB);
	            mCurTransaction.add(listFragmentB,
	                    makeFragmentName(listFragmentB));
	        }
        	listFragmentB.setMenuVisibility(false);
        	listFragmentB.setUserVisibleHint(false);
        }
        if (listFragment != mCurrentPrimaryItem) {
        	listFragment.setMenuVisibility(false);
        	listFragment.setUserVisibleHint(false);
        }

        if (DEBUG) Log.w(TAG, "instantiated " + position + ": f=" + listItem+" - c: "+container);
        return listItem;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        if (DEBUG) Log.w(TAG, "Detaching item #" + position + ": f=" + object);
        if(object instanceof AwfulDualPaneView){
        	mCurTransaction.remove(getFrag(object, true));
        	mCurTransaction.remove(getFrag(object, false));
        	((AwfulDualPaneView)object).clearFragments();
        	container.removeView((View) object);
        }else{
        	mCurTransaction.remove((Fragment)object);
        }
    }
    
    private Fragment getFrag(Object frag, boolean first){
    	if(frag instanceof AwfulDualPaneView){
    		if(first){
    			return (Fragment) ((AwfulDualPaneView)frag).getFirst();
    		}else{
    			return (Fragment) ((AwfulDualPaneView)frag).getSecond();
    		}
    	}else if(frag instanceof Fragment && first){
    		return (Fragment) frag;
    	}else{
    		return null;
    	}
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
    	if(DEBUG) Log.e(TAG,"setPrimaryItem "+object.toString());
        Fragment fragment = getFrag(object, true);
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
    	if(DEBUG) Log.w(TAG,"finishUpdate: "+container);
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
        if(!splitMode){
            for(AwfulPagerFragment apf : fragList){
            	if(apf instanceof AwfulDualPaneView){
            		((AwfulDualPaneView)apf).refreshChildren();
            	}
            }
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
    	if(DEBUG) Log.w(TAG,"isViewFromObject: "+view+" obj: "+object);
    	if(!splitMode){
    		if(object instanceof AwfulDualPaneView){
    			AwfulDualPaneView adpw = (AwfulDualPaneView) object;
    			return object == view || ((Fragment)adpw.getFirst()).getView() == view || ((Fragment)adpw.getSecond()).getView() == view;
    		}else if(object instanceof Fragment){
    			return ((Fragment)object).getView() == view;
        	}
    	}else if(object instanceof Fragment){
			return ((Fragment)object).getView() == view;
    	}
		return false;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    }

    private static String makeFragmentName(Fragment fragment) {
    	String name = "null";
    	if(fragment instanceof AwfulPagerFragment){
    		return "android:switcher:" + ((AwfulPagerFragment)fragment).getInternalId();
    	}else{
            Log.e(TAG, "makeFragmentName - INVALID FRAGMENT");
        }
    	if(DEBUG) Log.e(TAG, "makeFragmentName "+name);
		return name;
    }
    
	@Override
	public CharSequence getPageTitle(int position) {
		return getItem(position).getTitle();
	}

	@Override
	public int getItemPosition(Object object) {
		int pos = AwfulFragmentPagerAdapter.POSITION_NONE;
		if(splitMode){
			pos = splitFragList.indexOf(object);
		}else{
			pos = fragList.indexOf(object);
		}
		if(pos < 0){
			return AwfulFragmentPagerAdapter.POSITION_NONE;
		}else{
			return pos;
		}
	}
	
	public int getRealItemPosition(Object object){
		int pos = AwfulFragmentPagerAdapter.POSITION_NONE;
		if(splitMode){
			pos = splitFragList.indexOf(object);
		}else{
			pos = fragList.indexOf(object);
			if(pos < 0){
				for(int ix=0;ix<fragList.size();ix++){
					if(fragList.get(ix) instanceof AwfulDualPaneView){
						AwfulDualPaneView adpw = (AwfulDualPaneView) fragList.get(ix);
						if(adpw.getFirst() == object || adpw.getSecond() == object){
							return ix;
						}
					}
				}
			}
		}
		if(pos < 0){
			return AwfulFragmentPagerAdapter.POSITION_NONE;
		}else{
			return pos;
		}
	}

	@Override
	public int getCount() {
		if(splitMode){
			return splitFragList.size();
		}else{
			return fragList.size();
		}
	}
	
	public void setWidescreen(boolean widescreen){
		Log.e(TAG, "setWidescreen: "+(widescreen?"true":"false")+" currently: "+(splitMode? "false" : "true"));
		splitMode = !widescreen;
		notifyDataSetChanged();
	}
}
