package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import static com.ferg.awfulapp.thread.AwfulPost.tryConvertToHttps;

/**
 * Created by baka kaba on 24/01/2017.
 * <p>
 * Request to load announcements and parse them as AwfulPosts.
 *
 * This performs no database caching!
 *
 * Announcements are like a weird variation on posts in a thread, as such they only have a few
 * of the usual page elements, and only a few properties set in AwfulPost.
 */

public class AnnouncementsRequest extends AwfulRequest<List<AwfulPost>> {

    public AnnouncementsRequest(Context context) {
        super(context, null);
    }

    @NonNull
    private static List<AwfulPost> parseAnnouncement(@NonNull Document aThread) {
        List<AwfulPost> results = new ArrayList<>();
        AwfulPreferences prefs = AwfulPreferences.getInstance();

        // grab all the main announcement sections - these contain *most* of the data we need :/
        Elements mainAnnouncements = aThread.select("#main_full  tr[valign='top']");
        Log.d(TAG, "parseAnnouncement: found" + mainAnnouncements.size() + " announcements");
        for (Element announcementSection : mainAnnouncements) {
            AwfulPost announcement = new AwfulPost();

            Element author = announcementSection.select(".author").first();
            if (author != null) {
                announcement.setUsername(author.text());
            }

            Element regDate = announcementSection.select(".registered").first();
            if (regDate != null) {
                announcement.setRegDate(regDate.text());
            }

            Element avatar = announcementSection.select(".title img").first();
            if (avatar != null) {
                tryConvertToHttps(avatar);
                announcement.setAvatar(avatar.attr("src"));
            }

            // not sure if this ever appears for announcements but whatever, may as well
            Element editedBy = announcementSection.select(".editedby").first();
            if (editedBy != null) {
                announcement.setEdited("<i>" + editedBy.text() + "</i>");
            }

            // announcements have their post date in a whole other section directly after the announcement section
            Element postDateSection = announcementSection.nextElementSibling();
            if (postDateSection != null) {
                Element postDate = postDateSection.select(".postdate").first();
                if (postDate != null) {
                    announcement.setDate(postDate.text());
                }
            }


            Element postBody = announcementSection.select(".postbody").first();
            if (postBody != null) {
                // process videos, images and links and store the resulting post HTML
                AwfulPost.convertVideos(postBody, prefs.inlineYoutube);
                for (Element image : postBody.getElementsByTag("img")) {
                    AwfulPost.processPostImage(image, false, prefs);
                }
                for (Element link : postBody.getElementsByTag("a")) {
                    tryConvertToHttps(link);
                }
                announcement.setContent(postBody.html());
            }
            // I guess this is important...?
            announcement.setEditable(false);
            results.add(announcement);
            Log.i(TAG, Integer.toString(mainAnnouncements.size()) + " posts found, " + results.size() + " posts parsed.");
        }
        return results;
    }


    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        return "https://forums.somethingawful.com/announcement.php?forumid=1";
    }

    @Override
    protected List<AwfulPost> handleResponse(Document doc) throws AwfulError {
        return parseAnnouncement(doc);
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        Log.d(TAG, "handleError: " + error.getMessage() + "\n" + error.getSubMessage());
        return error.isCritical();
    }

}
