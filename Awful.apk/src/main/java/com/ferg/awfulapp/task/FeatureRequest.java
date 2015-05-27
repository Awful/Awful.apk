package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by matt on 8/8/13.
 */
public class FeatureRequest extends AwfulRequest<Void> {
    public FeatureRequest(Context context) {
        super(context, Constants.FUNCTION_MEMBER);
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        return urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, "accountfeatures").build().toString();
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        Element features = doc.getElementsByClass("features").first();
        boolean premium = false;
        boolean archives = false;
        boolean noads = false;
        if (features != null) {

            Elements feature_dts = features.getElementsByTag("dt");
            if (feature_dts.size() == 3) {
                premium = feature_dts.get(0).hasClass("enabled");
                archives = feature_dts.get(1).hasClass("enabled");
                noads = feature_dts.get(2).hasClass("enabled");
                try {
                    getPreferences().setBooleanPreference("has_platinum", premium);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    getPreferences().setBooleanPreference("has_archives", archives);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    getPreferences().setBooleanPreference("has_no_ads", noads);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            throw new AwfulError("Feature page did not load");
        }
        Log.i("FeatureRequest", "Updated account features P:" + premium + " A:"
                + archives + " NA:" + noads);
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
