package com.ferg.awfulapp.thread;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.view.Menu;
import android.view.MenuItem;

import com.ferg.awfulapp.R;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetMenuDialog;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

/**
 * Created by Christoph on 16.11.2016.
 */

public class AwfulPostIcon {

    public String iconId;
    public String iconUrl;

    public AwfulPostIcon(String iconId, String iconUrl){
        this.iconId = iconId;
        this.iconUrl = iconUrl;
    }

    public static ArrayList<AwfulPostIcon> parsePostIcons (Elements posticons){
        ArrayList<AwfulPostIcon> result = new ArrayList<>();

        for (Element icon: posticons) {
            String iconUrl = icon.child(1).attr("src");
            String iconId = icon.child(0).val();
            AwfulPostIcon postIcons = new AwfulPostIcon(iconId,iconUrl);
            result.add(postIcons);
        }

        return result;
    }
}
