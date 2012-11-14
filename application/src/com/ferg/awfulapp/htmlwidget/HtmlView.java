/*-
 * Copyright (C) 2010 Google Inc.
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

package com.ferg.awfulapp.htmlwidget;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

import org.jsoup.nodes.Element;

import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.CharacterStyle;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.ferg.awfulapp.R;

/**
 * A light-weight alternative to {@link WebView}.
 * <p>
 * Supports basic formatting, images, and embedded YouTube videos.
 */
public final class HtmlView extends TextView {
	private AQuery aq;
	private ImageView pseudoImgview = new ImageView(this.getContext());

    private static final String TAG = "HtmlView";

    private static final int EMBED_YOUTUBE = 1;

    private static final boolean USE_PLACEHOLDER_IMAGE = true;

    private static UriMatcher sUriMatcher;

    /**
     * Logs an error message about a resource with a URL.
     * <p>
     * The URL (which may contain personal information) is only logged if the
     * log level is set to {@link Log#DEBUG}.
     */
    private static void logResourceError(String message, String url) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                message += ": " + url;
            }
            Log.e(TAG, message);
        }
    }

    /**
     * Logs an error message about a resource with a URL.
     * <p>
     * The URL (which may contain personal information) is only logged if the
     * log level is set to {@link Log#DEBUG}.
     */
    private static void logResourceError(String message, String url, Throwable tr) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                message += ": " + url;
            }
            Log.e(TAG, message, tr);
        }
    }
    /**
     * Creates a {@link BitmapDrawable} with no scaling.
     */
    private Drawable createBitmapDrawable(Bitmap bitmap) {
        Resources res = getResources();
        BitmapDrawable drawable = new BitmapDrawable(res, bitmap);
        setBoundsToIntrinsicSize(drawable);
        return drawable;
    }

    /**
     * Creates an invisible {@link Drawable}.
     */
    protected Drawable getPlaceholderDrawable() {
        if(USE_PLACEHOLDER_IMAGE) {
            return mDrawableLoadingImage;
        } else {
            Drawable drawable = new ColorDrawable(0);
            drawable.setBounds(0, 0, 0, 0);
            return drawable;
        }
    }

    /**
     * Returns the video ID for a YouTube {@link Uri}.
     */
    private static String getYouTubeVideoId(Uri uri) {
        assert uri != null;
        assert sUriMatcher.match(uri) == EMBED_YOUTUBE;
        String id = uri.getPathSegments().get(1);
        int index = id.indexOf('&');
        if (index != -1) {
            id = id.substring(0, index);
        }
        return id;
    }

    private static String getYouTubeSnapshotUrl(String videoId) {
        if (videoId == null) {
            throw new NullPointerException();
        }
        // Returns a 480x360 snapshot of the video
        return "http://img.youtube.com/vi/" + videoId + "/0.jpg";
    }

    private static void setBoundsToIntrinsicSize(Drawable drawable) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, w, h);
    }

    private static LayerDrawable createLayerDrawable(Drawable... layers) {
        return new LayerDrawable(layers);
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI("www.youtube.com", "v/*", EMBED_YOUTUBE);
        sUriMatcher.addURI("www.youtube-nocookie.com", "v/*", EMBED_YOUTUBE);
    }

    /**
     * Keeps track of pending tasks.
     */
    private final ArrayList<ImageTask> mTasks = new ArrayList<ImageTask>();

    /**
     * The total number of tasks created for the last call to
     * {@link #setHtml(String)}.
     */
    private int mTotalTaskCount;

    /**
     * The number of tasks completed since the last call to
     * {@link #setHtml(String)}.
     */
    private int mCompleteTaskCount;

    private HtmlChromeClient mHtmlChromeClient = new HtmlChromeClient();

    /**
     * The x-position of the last touch event.
     */
    private int mLastX;

    /**
     * The y-position of the last touch event.
     */
    private int mLastY;
    
    private boolean imagesEnabled;

    private String mHtml;

    private Drawable mDrawableVideoBackground;

    private Drawable mDrawableVideoPlay;

    private Drawable mDrawableYouTubeLogo;

    private Drawable mDrawableMissingEmbed;

    private Drawable mDrawableMissingImage;
    
    private Drawable mDrawableLoadingImage;

    public HtmlView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadDrawables();
        aq = new AQuery(this);
    }

    public HtmlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadDrawables();
        aq = new AQuery(this);
    }

    public HtmlView(Context context) {
        super(context);
        loadDrawables();
        aq = new AQuery(this);
    }

    private void loadDrawables() {
        Context context = getContext();
        String packgeName = context.getPackageName();
        Resources resources = getResources();
        int backgroundResId = resources.getIdentifier("icon", "drawable", packgeName);//background
        int playResId = resources.getIdentifier("ic_menu_video", "drawable", packgeName);
        int logoResId = resources.getIdentifier("icon", "drawable", packgeName);
        int missingEmbedResId = resources.getIdentifier("icon", "drawable", packgeName);
        int missingImageResId = resources.getIdentifier("light_inline_link", "drawable", packgeName);
        
        if (backgroundResId == 0) {
            throw new RuntimeException("R.drawable.background is missing");
        }
        if (playResId == 0) {
            throw new RuntimeException("R.drawable.play_center is missing");
        }
        if (logoResId == 0) {
            throw new RuntimeException("R.drawable.logo is missing");
        }
        if (missingEmbedResId == 0) {
            throw new RuntimeException("R.drawable.missing_embed is missing");
        }
        if (missingImageResId == 0) {
            throw new RuntimeException("R.drawable.missing_image is missing");
        }
        
        mDrawableVideoBackground = resources.getDrawable(backgroundResId);
        mDrawableVideoPlay = resources.getDrawable(playResId);
        mDrawableYouTubeLogo = resources.getDrawable(logoResId);
        mDrawableMissingEmbed = resources.getDrawable(missingEmbedResId);
        mDrawableMissingImage = resources.getDrawable(missingImageResId);
        
        setBoundsToIntrinsicSize(mDrawableMissingEmbed);
        setBoundsToIntrinsicSize(mDrawableMissingImage);
        
        if(USE_PLACEHOLDER_IMAGE) {
            int loadingImageResId = resources.getIdentifier("light_inline_load", "drawable", packgeName);
            if (loadingImageResId == 0) {
                throw new RuntimeException("R.drawable.loading_image is missing");
            }
            mDrawableLoadingImage = resources.getDrawable(loadingImageResId);
            setBoundsToIntrinsicSize(mDrawableLoadingImage);
        }
    }

    /**
     * Creates a new {@link LayerDrawable} to represent an embedded video.
     * <p>
     * The layer {@link android.R.id#background} can be replaced with a
     * thumbnail of the video using
     * {@link LayerDrawable#setDrawableByLayerId(int, Drawable)}.
     */
    private LayerDrawable createVideoDrawable(Drawable logo) {
        // Note: It is important that the LayerDrawable is not inflated from a
        // resource because Drawable#mutate() does not make it safe to swap
        // layers.
        LayerDrawable drawable = (logo != null) ? createLayerDrawable(mDrawableVideoBackground,
                mDrawableVideoPlay, logo) : createLayerDrawable(mDrawableVideoBackground,
                mDrawableVideoPlay);

        int backgroundIndex = 0;
        drawable.setId(backgroundIndex, android.R.id.background);

        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, w, h);

        return drawable;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.mHtml = mHtml;
        return ss;
    }
    

	@Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        String html = ss.mHtml;
        if (html != null) {
            setHtml(html, true);
        }
    }

    public void setHtmlChromeClient(HtmlChromeClient client) {
        if (client == null) {
            throw new NullPointerException();
        }
        mHtmlChromeClient = client;
    }

    protected Bitmap getImage(String src) {
    	return aq.getCachedImage(src);
    }

    private void handleEmbed(Element node, Editable output) {
        String src = node.attr("src");
        String type = node.attr("type");
        boolean allowFullScreen = Boolean.parseBoolean(node.attr("allowfullscreen"));

        Uri uri = null;
        int match = UriMatcher.NO_MATCH;
        if (src != null) {
            uri = Uri.parse(src);
            match = sUriMatcher.match(uri);
        }

        Intent[] intents = null;
        String snapshotUrl = null;
        Drawable drawable;
        LayerDrawable frame = null;
        if (match == EMBED_YOUTUBE) {
            String videoId = getYouTubeVideoId(uri);
            drawable = frame = createVideoDrawable(mDrawableYouTubeLogo);
            snapshotUrl = getYouTubeSnapshotUrl(videoId);
            intents = new Intent[] {
                    // Try opening with YouTube application
                    new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId)),
                    // Fallback to opening website
                    new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + videoId))
            };
        } else {
            if ("application/x-shockwave-flash".equals(type) && allowFullScreen) {
                // The embed looks like a generic Flash video
                Drawable logo = null;
                drawable = createVideoDrawable(logo);
            } else {
                // The embed was not recognized
                drawable = mDrawableMissingEmbed;
            }
            if (src != null) {
                if (type != null) {
                    intents = new Intent[] {
                            // Try opening with URL and type (use application)
                            new Intent(Intent.ACTION_VIEW, Uri.parse(src)).setType(type),
                            // Fallback to opening with URL (use browser)
                            new Intent(Intent.ACTION_VIEW, Uri.parse(src))
                    };
                } else {
                    intents = new Intent[] {
                        // Try opening source URL directly
                        new Intent(Intent.ACTION_VIEW, Uri.parse(src))
                    };
                }
            }
        }
        int start = output.length();
        output.append("\uFFFC");
        int end = output.length();
        output.setSpan(new ImageSpan(drawable, src), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (intents != null) {
            output.setSpan(new IntentsSpan(intents), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (frame != null && snapshotUrl != null && PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("images_enabled", true)) {
            int layerId = android.R.id.background;
            Bitmap snapshotBitmap = getImage(snapshotUrl);
            if (snapshotBitmap != null) {
                Drawable snapshotDrawable = new BitmapDrawable(snapshotBitmap);
                snapshotDrawable.setBounds(frame.getBounds());
                frame.setDrawableByLayerId(layerId, snapshotDrawable);
            } else {
            	executeImageTask(new ImageTask(snapshotUrl,frame,layerId));
            }
        }
    }

    private void handleImg(Element node, Editable output) {
        String src = node.attr("src");
        String alt = node.attr("alt");
        String title = node.attr("title");
        
        int start = output.length();
        output.append("\uFFFC");
        int end = output.length();

        Bitmap bitmap = getImage(src);
        if (bitmap != null) {
            Drawable drawable = createBitmapDrawable(bitmap);
            HtmlImageSpan span = new HtmlImageSpan(drawable, src, title, alt);
            output.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            Drawable placeholder = getPlaceholderDrawable();
            HtmlImageSpan span = new HtmlImageSpan(placeholder, src, title, alt);
            output.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (src != null) {
                ImageTask task = new ImageTask(src, span);
                executeImageTask(task);
            }
        }
    }

    public void setHtml(String source, boolean loadImages) {
    	cancelTasks();
    	aq.recycle(pseudoImgview);
        imagesEnabled = loadImages;
        if (source == null) {
            setText(null);
            return;
        }
        if (source.equals(mHtml)) {
            return;
        }
        mHtml = source;

        // The Html.ImageGetter API is too limited because it does not provide
        // values for the 'alt' and 'title' attributes of image tags.
        Html.ImageGetter imageGetter = null;

        Html.TagHandler tagHandler = new Html.TagHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void handleStartTag(Element node, Editable output) {
                String tag = node.tagName();
                
                if (tag.equalsIgnoreCase("embed")) {
                    handleEmbed(node, output);
                } else if (tag.equalsIgnoreCase("img")) {
                    if(imagesEnabled){
                        handleImg(node, output);
                    }else{
                        if(node.attr("title").equalsIgnoreCase("")){
                            int start = output.length();
                            output.append(node.attr("src"));
                            output.setSpan(new URLSpan(node.attr("src")), start, output.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }else{
                            int start = output.length();
                            output.append(node.attr("title"));
                            output.setSpan(new URLSpan(node.attr("src")), start, output.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            }
            
            @Override
            public void handleEndTag(Element node, Editable output) {}
        };

        CharSequence text = Html.fromHtml(source, imageGetter, tagHandler, getContext());

        // Although the text is not editable by the user, it needs to be
        // BufferType.EDITABLE so that asynchronous tasks can replace spans with
        // content retrieved from the network.
        // Caller super.setText to avoid clearing the mHtml field.
        super.setText(text, BufferType.EDITABLE);

        // Show one progress unit for loading the HTML itself.
        mTotalTaskCount++;
        mCompleteTaskCount++;
        updateProgress();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        mHtml = null;
    }

    @Override
    public Editable getEditableText() {
        // Hide the fact that this TextView is editable from external classes
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Remember the location of the last touch event for getHitTestResult
        //
        // TODO: Support key-driven long-press events
        mLastX = (int) event.getX();
        mLastY = (int) event.getY();
        return super.onTouchEvent(event);
    }

    /**
     * Returns {@link CharacterStyle spans} of the given type at the location of
     * the last touch event.
     *
     * @param <T>
     * @param type the type of {@link CharacterStyle spans} to search for.
     *            Specify {@code Object.class} for the type if you want all the
     *            objects regardless of type.
     * @return the located {@link CharacterStyle spans}.
     */
    @SuppressWarnings("unchecked")
    public <T> T[] getHitTestResult(Class<T> type) {
        int x = mLastX;
        int y = mLastY;

        x -= getTotalPaddingLeft();
        y -= getTotalPaddingTop();

        x += getScrollX();
        y += getScrollY();

        Layout layout = getLayout();
        int line = layout.getLineForVertical(y);
        int offset = layout.getOffsetForHorizontal(line, x);

        // Call super.getEditableText() because this.getEditableText()
        // always returns null.
        Spannable buffer = super.getEditableText();
        if (buffer != null) {
            return buffer.getSpans(offset, offset, type);
        } else {
            return (T[]) Array.newInstance(type, 0);
        }
    }

    private void updateProgress() {
        int newProgress = 100;
        if (mTotalTaskCount != 0) {
            newProgress = 100 * mCompleteTaskCount / mTotalTaskCount;
        }

        newProgress = Math.max(0, newProgress);
        newProgress = Math.min(100, newProgress);

        mHtmlChromeClient.onProgressChanged(this, newProgress);
    }
    
    public void cancelTasks(){
    	for(ImageTask i : mTasks){
    		i.cancelTask();
    	}
    }
    
    private void executeImageTask(ImageTask it){
    	aq.image(it.getUrl(),true,true,0,R.drawable.light_inline_link,it);
    	mTasks.add(it);
    }

    private class ImageTask extends BitmapAjaxCallback {
    	

        /**
         * The URL to load.
         */
        private final String mUrl;

        /**
         * The temporary span to replace, or {@code null}.
         */
        private final HtmlImageSpan mSpan;

        /**
         * A multi-layer {@link Drawable} to contain the loaded image, or
         * {@code null}.
         */
        private final LayerDrawable mLayers;

        /**
         * The ID of the layer in {@link #mLayers} to replace.
         */
        private final int mLayerId;

        /**
         * {@code true} if the task has been canceled, {@code false} otherwise.
         */
        private boolean mCancelled = false;
        
        private InputStream inputStream = null;

        public ImageTask(String url, HtmlImageSpan placeholder) {
            mUrl = url;
            mSpan = placeholder;

            // Not used:
            mLayers = null;
            mLayerId = -1;
            aq = new AQuery(HtmlView.this.getContext());
        }

        public void cancelTask() {
			mCancelled = true;
			cancel();
		}

		public ImageTask(String url, LayerDrawable layers, int layerId) {
            mUrl = url;
            mLayers = layers;
            mLayerId = layerId;

            // Not used:
            mSpan = null;
            aq = new AQuery(HtmlView.this.getContext());
        }
        
        public void killTask(){
        	if(inputStream != null){
        		try {
					inputStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        }


        private Drawable getDrawable(Bitmap result) {
            if (result != null) {
                return createBitmapDrawable(result);
            } else {
                return mDrawableMissingImage;
            }
        }

        private void replaceSpan(Bitmap result) {
            if (mSpan != null) {
                // Call super.getEditableText() because
                // this.getEditableText() always returns null.
                Editable editableText = HtmlView.super.getEditableText();
                if (editableText != null) {
                    int start = editableText.getSpanStart(mSpan);
                    int end = editableText.getSpanEnd(mSpan);
                    if (start != -1 && end != -1) {
                        editableText.removeSpan(mSpan);
                        Drawable d = getDrawable(result);
                        String src = mSpan.getSource();
                        String alt = mSpan.getAlt();
                        String title = mSpan.getTitle();
                        HtmlImageSpan span = new HtmlImageSpan(d, src, title, alt);
                        editableText.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }

        private void replaceLayer(Bitmap result) {
            if (mLayers != null && result != null) {
                BitmapDrawable drawable = new BitmapDrawable(result);
                drawable.setBounds(mLayers.getBounds());
                mLayers.setDrawableByLayerId(mLayerId, drawable);
            }
        }

		@Override
		protected void callback(String url, ImageView iv,
				Bitmap bm, AjaxStatus status) {
			onPostImage(bm);
			mTasks.remove(this);
		}
		
        protected void onPostImage(Bitmap result) {
            if (!mCancelled) {
                replaceSpan(result);
                replaceLayer(result);

                mCompleteTaskCount += 1;
                updateProgress();
            }
        }
    }

    public static class SavedState extends BaseSavedState {

        private String mHtml;

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public SavedState(Parcel in) {
            super(in);
            boolean hasHtml = in.readInt() != 0;
            if (hasHtml) {
                mHtml = in.readString();
            }
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            boolean hasHtml = (mHtml != null);
            out.writeInt(hasHtml ? 1 : 0);
            if (hasHtml) {
                out.writeString(mHtml);
            }
        }
    }
}
