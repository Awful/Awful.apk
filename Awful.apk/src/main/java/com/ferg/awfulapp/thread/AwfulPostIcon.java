package com.ferg.awfulapp.thread;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.network.NetworkUtils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

/**
 * Created by Christoph on 16.11.2016.
 */

public class AwfulPostIcon {
    static final ColorFilter BACKGROUND_FILTER;

    static {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.7f);
        BACKGROUND_FILTER = new ColorMatrixColorFilter(matrix);
    }

    @DrawableRes
    public static final int BLANK_ICON_DRAWABLE_ID = R.drawable.empty_thread_tag;
    public static final String BLANK_ICON_ID = "0";
    public static final AwfulPostIcon BLANK_ICON = new AwfulPostIcon();
    private static final int TAG_SIZE = 90;

    public final String iconId;
    public final String iconUrl;
    public final int drawableId;
    public Drawable drawable = null;

    private AwfulPostIcon(@NonNull String iconId, @NonNull String iconUrl, @NonNull Context context) {
        this.iconId = iconId;
        this.iconUrl = iconUrl;
        drawableId = getIconResId(iconUrl, context);
        if(drawableId == BLANK_ICON_DRAWABLE_ID){
            new Handler(Looper.getMainLooper()).post(() -> NetworkUtils.getImageLoader().get(iconUrl, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                drawable = AwfulPostIcon.getClassicIconDrawable(response.getBitmap(), context);
            }

            @Override
            public void onErrorResponse(VolleyError error) {}
        }));

        }
    }


    /**
     * A default empty icon, to represent 'no icon' options
     */
    private AwfulPostIcon() {
        iconId = BLANK_ICON_ID;
        iconUrl = "";
        drawableId = BLANK_ICON_DRAWABLE_ID;
        drawable = null;
    }


    @DrawableRes
    private static int getIconResId(@NonNull String iconUrl, @NonNull Context context) {
        String localFileName = "@drawable/"+iconUrl.substring(iconUrl.lastIndexOf('/') + 1, iconUrl.lastIndexOf('.')).replace('-','_').toLowerCase();
        int imageID = context.getResources().getIdentifier(localFileName, null, context.getPackageName());
        return imageID == 0 ? BLANK_ICON_DRAWABLE_ID : imageID;
    }

    public static Drawable getClassicIconDrawable(@NonNull Bitmap bitmap, @NonNull Context context) {
        if(bitmap == null) {
            return context.getDrawable(R.drawable.empty_thread_tag);
        }
        // make a zoomed version of the tag bitmap that fills the view, and set it as the background
        Bitmap backgroundBitmap = ThumbnailUtils.extractThumbnail(bitmap, TAG_SIZE, TAG_SIZE);
        BitmapDrawable backgroundDrawable = new BitmapDrawable(context.getResources(), backgroundBitmap);
        backgroundDrawable.setColorFilter(BACKGROUND_FILTER);
        backgroundDrawable.setAlpha(128);

        BitmapDrawable foregroundDrawable = new BitmapDrawable(context.getResources(), bitmap);
        int newHeight = Math.round(((float) TAG_SIZE / (float)foregroundDrawable.getIntrinsicWidth()) * (float)foregroundDrawable.getIntrinsicHeight());
        int horizontalInset = (TAG_SIZE - newHeight) / 2;

        LayerDrawable mashDrawable = new LayerDrawable(new Drawable[] {backgroundDrawable, foregroundDrawable});
        mashDrawable.setLayerInset(0,0,0,0, 0);
        mashDrawable.setLayerInset(1, 0 , horizontalInset, 0, horizontalInset);

        final Bitmap finalBitmap = Bitmap.createBitmap(TAG_SIZE, TAG_SIZE, Bitmap.Config.ARGB_8888);
        mashDrawable.setBounds(0, 0, TAG_SIZE, TAG_SIZE);
        mashDrawable.draw(new Canvas(finalBitmap));
        return new BitmapDrawable(context.getResources(), finalBitmap);
    }

    public static ArrayList<AwfulPostIcon> parsePostIcons (Elements icons, @NonNull Context context){
        ArrayList<AwfulPostIcon> result = new ArrayList<>();

        for (Element icon: icons) {
            String iconUrl = icon.child(1).attr("src");
            String iconId = icon.child(0).val();
            AwfulPostIcon postIcon = new AwfulPostIcon(iconId, iconUrl, context);
            result.add(postIcon);
        }

        return result;
    }
}
