package com.ferg.awfulapp.thread;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.ferg.awfulapp.R;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

/**
 * Created by Christoph on 16.11.2016.
 */

public class AwfulPostIcon {

    @DrawableRes
    public static final int BLANK_ICON_DRAWABLE_ID = R.drawable.empty_thread_tag;
    public static final String BLANK_ICON_ID = "0";
    public static final AwfulPostIcon BLANK_ICON = new AwfulPostIcon();

    public final String iconId;
    public final String iconUrl;
    public final int drawableId;

    private AwfulPostIcon(@NonNull String iconId, @NonNull String iconUrl, @NonNull Context context) {
        this.iconId = iconId;
        this.iconUrl = iconUrl;
        drawableId = getIconResId(iconUrl, context);
    }


    /**
     * A default empty icon, to represent 'no icon' options
     */
    private AwfulPostIcon() {
        iconId = BLANK_ICON_ID;
        iconUrl = "";
        drawableId = BLANK_ICON_DRAWABLE_ID;
    }


    @DrawableRes
    private static int getIconResId(@NonNull String iconUrl, @NonNull Context context) {
        String localFileName = "@drawable/"+iconUrl.substring(iconUrl.lastIndexOf('/') + 1, iconUrl.lastIndexOf('.')).replace('-','_').toLowerCase();
        int imageID = context.getResources().getIdentifier(localFileName, null, context.getPackageName());
        return imageID == 0 ? BLANK_ICON_DRAWABLE_ID : imageID;
    }

    public static ArrayList<AwfulPostIcon> parsePostIcons (Elements icons, @NonNull Context context){
        ArrayList<AwfulPostIcon> result = new ArrayList<>();

        for (Element icon: icons) {
            String iconUrl = icon.child(1).attr("src");
            String iconId = icon.child(0).val();
            AwfulPostIcon postIcon = new AwfulPostIcon(iconId,iconUrl, context);
            result.add(postIcon);
        }

        return result;
    }
}
