package com.ferg.awfulapp.thread

import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import android.widget.Toast
import androidx.collection.ArrayMap
import androidx.core.content.ContextCompat
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.AwfulTheme.Companion.forForum
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.MustacheException
import com.samskivert.mustache.Template
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader

/**
 * Created by baka kaba on 04/06/2017.
 * 
 * 
 * Contains methods used to produce the HTML required to display site content like posts and
 * threads.
 * 
 * Typically you'll want to call [.getContainerHtml]
 * to generate the basic template HTML to use as webview content. This template will contain all
 * the scripts and styling used to produce the content we scrape from the site, it will handle
 * embedding etc.
 * 
 * Once you have this template, you can add content to the container class (usually by calling
 * [com.ferg.awfulapp.webview.AwfulWebView.setBodyHtml]) - most content involves some
 * CSS markup though, so if it's not parsed directly from the site, you will probably want to add
 * some markup yourself, e.g. styling it as a post (see the mustache files for the format those take)
 * so it looks correct.
 * 
 * The [.getThreadHtml] method specifically allows you to
 * create a view of a thread by passing it a set of posts and some details about what part of the
 * thread it represents. This handles things like styling read/unread posts and hiding previously
 * read ones on the page.
 * 
 * Extracted from [AwfulThread]
 */
object AwfulHtmlPage {
    // TODO: 16/08/2017 generate this automatically from the folder contents
    /**
     * All the scripts from the javascript folder used in generating HTML
     */
    val JS_FILES: Array<String?> = arrayOf(
        "polyfills.js",
        "twitterwidget.js",
        "longtap.js",
        "jsonp.js",
        "embedding.js",
        "thread.js"
    )

    /**
     * All user roles we have icons for
     */
    val ROLES: Array<String?> = arrayOf(
        "admin",
        "supermod",
        "mod",
        "coder",
        "ik"
    )

    /**
     * parses the user role and returns it if it is a known role. Null otherwise
     * @param role the user role
     * @return the role as a string or null
     */
    private fun parseRole(role: String): String? {
        if (role.isNotEmpty() && listOf(*ROLES).contains(role)) {
            return role
        }

        return null
    }

    /**
     * Get the main HTML for the containing page.
     * 
     * 
     * This contains no post data, but sets up the basic template with the required JS scripts,
     * CSS etc., and a container class element to insert post data into. If you want to display
     * site content in a WebView, use this and put the content in the container.
     * 
     * @param aPrefs  used to customise the template to the user's preferences
     * @param forumId the ID of the forum this thread belongs to, used for theming
     * @param padForFab whether this HTML is for a layout where you might need to make space for
     * the FAB (e.g. viewing a thread in the viewpager) or not (e.g. previewing a
     * post in a popup dialog). The method will check if the FAB is actually enabled,
     * you just need to define whether that requires padding.
     */
    @JvmStatic
    fun getContainerHtml(aPrefs: AwfulPreferences, forumId: Int?, padForFab: Boolean): String {
        val buffer = StringBuilder("<!DOCTYPE html>\n<html>\n<head>\n")
        buffer.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0 maximum-scale=1.0 minimum-scale=1.0, user-scalable=no\" />\n")
        buffer.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n")
        buffer.append("<meta name='format-detection' content='telephone=no' />\n")
        buffer.append("<meta name='format-detection' content='address=no' />\n")


        // build the theme css tag, using the appropriate css path
        // the dark-theme attribute can be used to e.g. embed a dark or light widget
        val theme = forForum(forumId)
        buffer.append("<link rel='stylesheet' href='file:///android_asset/css/general.css' />")
        buffer.append(
            String.format(
                "<link id='theme-css' rel='stylesheet' data-dark-theme='%b' href='%s'>\n",
                theme.isDark,
                theme.cssPath
            )
        )
        buffer.append("<link rel='stylesheet' type='text/css' href='https://i.somethingawful.com/css/platicons.css' />")


        if (aPrefs.preferredFont?.contains("default") == false) {
            buffer.append("<style id='font-face' type='text/css'>@font-face { font-family: userselected; src: url('file:///android_asset/")
                .append(aPrefs.preferredFont).append("'); }</style>\n")
        }
        for (scriptName in JS_FILES) {
            buffer.append("<script src='file:///android_asset/javascript/")
                .append(scriptName)
                .append("' type='text/javascript'></script>\n")
        }

        buffer.append("</head><body><div id='container' class='container' ")
            .append((if (padForFab && !aPrefs.noFAB) "style='padding-bottom:75px'" else ""))
            .append("></div><script type='text/javascript'>containerInit();</script></body></html>")
        return buffer.toString()
    }


    /**
     * Generates post content HTML for a list of posts.
     * 
     * 
     * This method produces the content that should be inserted into the container template that
     * [.getContainerHtml] produces.
     * 
     * @param aPosts   the list of posts to generate HTML for
     * @param aPrefs   used to customise the post content to the user's preferences
     * @param page     the number of the page this represents
     * @param lastPage the number of the last page in this thread
     * @return the generated content, ready for insertion into the template
     */
    fun getThreadHtml(
        aPosts: MutableList<AwfulPost>,
        aPrefs: AwfulPreferences,
        page: Int,
        lastPage: Int
    ): String {
        val buffer = StringBuilder(1024)
        buffer.append("<div id='thread' class='content'>\n")

        // if we're hiding read posts, work out how many are read and add the 'show old posts' link
        if (aPrefs.hideOldPosts && aPosts.isNotEmpty() && !aPosts[aPosts.size - 1].isPreviouslyRead) {
            var unreadCount = 0
            for (ap in aPosts) {
                if (!ap.isPreviouslyRead) {
                    unreadCount++
                }
            }
            if (unreadCount < aPosts.size && unreadCount > 0) {
                buffer.append("    <article class='toggleread post'>")
                buffer.append("      <a>\n")
                val prevPosts = aPosts.size - unreadCount
                buffer.append("        <h3>Show ")
                    .append(prevPosts).append(" Previous Post")
                    .append(if (prevPosts > 1) "s" else "").append("</h3>\n")
                buffer.append("      </a>\n")
                buffer.append("    </article>")
            }
        }

        // add the actual posts
        buffer.append(getPostsHtml(aPosts, aPrefs))

        if (page == lastPage) {
            buffer.append("<div class='unread' ></div>\n")
        }
        buffer.append("</div>\n")

        return buffer.toString()
    }


    /**
     * Generates HTML for a list of posts using the appropriate Mustache layout.
     * 
     * 
     * This method generates HTML for the actual posts, taking user preferences into account.
     * 
     * @return a HTML string representing all the posts
     */
    private fun getPostsHtml(aPosts: MutableList<AwfulPost>, aPrefs: AwfulPreferences): String {
        val buffer = StringBuilder()
        val postTemplate: Template

        try {
            postTemplate = getPostTemplate(aPrefs)
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }

        // should be fine to re-use this since we rewrite every mapping each time
        val postData: MutableMap<String?, String?> = ArrayMap()
        postData["notOnProbation"] = if (aPrefs.isOnProbation) null else "notOnProbation"

        // run each post's data through the template, and combine into a final HTML string
        for (post in aPosts) {
            val username = post.username
            val avatar = post.avatar
            val avatarSecond = post.avatarSecond

            postData["seen"] = if (post.isPreviouslyRead) "read" else "unread"
            postData["isOP"] = if (aPrefs.highlightOP && post.isOp) "op" else null
            postData["isIgnored"] = if (aPrefs.hideIgnoredPosts && post.isIgnored) "ignored" else null
            postData["isMarked"] = if (aPrefs.markedUsers?.contains(username) == true) "marked" else null
            postData["postID"] = post.id
            postData["isSelf"] = if (aPrefs.highlightSelf && username == aPrefs.username) "self" else null
            postData["avatarURL"] = if (aPrefs.canLoadAvatars() && !avatar.isNullOrEmpty()) avatar else null
            postData["avatarSecondURL"] = if (aPrefs.canLoadAvatars() && !avatarSecond.isNullOrEmpty()) avatarSecond else null
            postData["username"] = username
            postData["userID"] = post.userId
            postData["postDate"] = post.date?.ifEmpty { null }
            postData["regDate"] = post.regDate?.ifEmpty { null }
            postData["role"] = parseRole(post.role ?: "")
            postData["icon"] = post.icon
            postData["plat"] = if (post.isPlat) "plat" else null
            postData["avatarText"] = post.avatarText
            postData["lastReadUrl"] = post.lastReadUrl
            postData["editable"] = if (post.isEditable) "editable" else null
            postData["modControls"] = if (post.hasModControls) "modControls" else null
            postData["postcontent"] = post.content
            postData["hideAvatar"] = if (aPrefs.isBlockedAvatar(avatar)) "blockedAvatar" else null

            try {
                buffer.append(postTemplate.execute(postData))
            } catch (e: MustacheException) {
                e.printStackTrace()
            }
        }
        return buffer.toString()
    }

    /**
     * Get a Mustache template for posts, according to the user's preferences.
     * 
     * 
     * Falls back to the default template if a custom layout can't be accessed.
     * 
     * @param aPrefs used to check if a custom layout is selected
     * @throws IOException if the default template can't be read
     */
    @Throws(IOException::class)
    private fun getPostTemplate(aPrefs: AwfulPreferences): Template {
        val postTemplate: Template
        var templateReader: Reader? = null

        // user has a custom template selected (nobody uses this I bet)
        if ("default" != aPrefs.layout) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                aPrefs.context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                val template = File(
                    Environment.getExternalStorageDirectory().toString() + "/awful/" + aPrefs.layout
                )
                if (template.isFile && template.canRead()) {
                    templateReader = FileReader(template)
                }
            } else {
                Toast.makeText(
                    aPrefs.context,
                    "Can't access custom layout because Awful lacks storage permissions. Reverting to default layout.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // use the default if necessary
        if (templateReader == null) {
            templateReader = InputStreamReader(aPrefs.resources.assets.open("mustache/post.mustache"))
        }
        postTemplate = Mustache.compiler().compile(templateReader)
        return postTemplate
    }
}
