/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferg.awfulapp.thread

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils.get
import com.ferg.awfulapp.preferences.AwfulPreferences
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.i
import java.sql.Timestamp
import java.util.Collections
import java.util.concurrent.Callable
import java.util.regex.Matcher
import java.util.regex.Pattern

class AwfulPost {
    var threadId: Int = -1
    var id: String? = ""
    var date: String? = ""
    var regDate: String? = ""
    var userId: String? = ""
    var username: String? = ""
    var avatar: String? = ""
    var avatarSecond: String? = ""
    var avatarText: String? = ""
    var content: String? = ""
    var edited: String? = ""

    var isIgnored: Boolean = false
    var isPreviouslyRead: Boolean = false
    var lastReadUrl: String? = ""
    var isEditable: Boolean = false
    var hasModControls: Boolean = false
    var isOp: Boolean = false
    var isPlat: Boolean = false
    var role: String? = ""
    var icon: String? = ""


    companion object {
        private const val TAG = "AwfulPost"

        const val PATH: String = "/post"
        @JvmField
        val CONTENT_URI: Uri = "content://${Constants.AUTHORITY}$PATH".toUri()

        private val youtubeHDId_regex: Pattern = Pattern.compile("/embed/([\\w_-]+)&?")
        private val tiktokId_regex: Pattern = Pattern.compile("([\\d]+)$")
        private val imgurId_regex: Pattern = Pattern.compile("^(.*\\.imgur\\.com/)([\\w]+)(\\..*)$")
        private val vimeoId_regex: Pattern = Pattern.compile("clip_id=(\\d+)&?")
        private val badPost_regex: Pattern =
            Pattern.compile("^\\(USER WAS (?:BANNED|AUTOBANNED|PERMABANNED|PUT ON PROBATION) FOR THIS POST\\)$")

        private val HTTPS_SUPPORTED_DOMAINS: MutableList<String?> =
            Collections.unmodifiableList(
                mutableListOf<String?>("imgur.com", "somethingawful.com", "giphy.com")
            )

        const val ID: String = "_id"
        const val POST_INDEX: String = "post_index"
        const val THREAD_ID: String = "thread_id"
        const val DATE: String = "date"
        const val REGDATE: String = "regdate"
        const val USER_ID: String = "user_id"
        const val USERNAME: String = "username"

        // 2022/09/21 - TODO: the disadvantage of storing this with a post is that it's not automatically refreshed if a user is unblocked
        const val IS_IGNORED: String = "is_ignored"
        const val PREVIOUSLY_READ: String = "previously_read"
        const val EDITABLE: String = "editable"
        const val HAS_MOD_CONTROLS: String = "has_mod_controls"
        const val IS_OP: String = "is_op"
        const val IS_PLAT: String = "is_plat"
        const val ROLE: String = "role"
        const val ICON: String = "icon"
        const val AVATAR: String = "avatar"

        // people may be using gangtags, etc. as avatars with a 1x1 primary av
        const val AVATAR_SECOND: String = "avatar_second"
        const val AVATAR_TEXT: String = "avatar_text"
        const val CONTENT: String = "content"
        const val EDITED: String = "edited"

        const val FORM_KEY: String = "form_key"
        const val FORM_COOKIE: String = "form_cookie"
        const val FORM_BOOKMARK: String = "bookmark"
        const val FORM_SIGNATURE: String = "signature"
        const val FORM_DISABLE_SMILIES: String = "disablesmilies"

        /** For comparing against replies to see if the user actually typed anything.  */
        const val REPLY_ORIGINAL_CONTENT: String = "original_reply"
        const val EDIT_POST_ID: String = "edit_id"


        fun fromCursor(aCursor: Cursor): MutableList<AwfulPost> {
            val result = ArrayList<AwfulPost>()

            if (aCursor.moveToFirst()) {
                val idIndex = aCursor.getColumnIndex(ID)
                val threadIdIndex = aCursor.getColumnIndex(THREAD_ID)
                val postIndexIndex = aCursor.getColumnIndex(POST_INDEX) //ooh, meta
                val dateIndex = aCursor.getColumnIndex(DATE)
                val regdateIndex = aCursor.getColumnIndex(REGDATE)
                val userIdIndex = aCursor.getColumnIndex(USER_ID)
                val usernameIndex = aCursor.getColumnIndex(USERNAME)
                val isIgnoredIndex = aCursor.getColumnIndex(IS_IGNORED)
                val previouslyReadIndex = aCursor.getColumnIndex(PREVIOUSLY_READ)
                val editableIndex = aCursor.getColumnIndex(EDITABLE)
                val hasModControlsIndex = aCursor.getColumnIndex(HAS_MOD_CONTROLS)
                val isOpIndex = aCursor.getColumnIndex(IS_OP)
                val isPlatIndex = aCursor.getColumnIndex(IS_PLAT)
                val roleIndex = aCursor.getColumnIndex(ROLE)
                val iconIndex = aCursor.getColumnIndex(ICON)
                val avatarIndex = aCursor.getColumnIndex(AVATAR)
                val avatarSecondIndex = aCursor.getColumnIndex(AVATAR_SECOND)
                val avatarTextIndex = aCursor.getColumnIndex(AVATAR_TEXT)
                val contentIndex = aCursor.getColumnIndex(CONTENT)
                val editedIndex = aCursor.getColumnIndex(EDITED)

                var current: AwfulPost

                do {
                    current = AwfulPost()
                    current.id = aCursor.getString(idIndex)
                    current.threadId = aCursor.getInt(threadIdIndex)
                    current.date = aCursor.getString(dateIndex)
                    current.regDate = aCursor.getString(regdateIndex)
                    current.userId = aCursor.getString(userIdIndex)
                    current.username = aCursor.getString(usernameIndex)
                    current.isIgnored = aCursor.getInt(isIgnoredIndex) == 1
                    current.isPreviouslyRead = aCursor.getInt(previouslyReadIndex) > 0
                    current.lastReadUrl = aCursor.getInt(postIndexIndex).toString()
                    current.isEditable = aCursor.getInt(editableIndex) == 1
                    current.hasModControls = aCursor.getInt(hasModControlsIndex) == 1
                    current.isOp = aCursor.getInt(isOpIndex) == 1
                    current.isPlat = aCursor.getInt(isPlatIndex) > 0
                    current.role = aCursor.getString(roleIndex)
                    current.icon = aCursor.getString(iconIndex)
                    current.avatar = aCursor.getString(avatarIndex)
                    current.avatarSecond = aCursor.getString(avatarSecondIndex)
                    current.avatarText = aCursor.getString(avatarTextIndex)
                    current.content = aCursor.getString(contentIndex)
                    current.edited = aCursor.getString(editedIndex)

                    result.add(current)
                } while (aCursor.moveToNext())
            } else {
                i("No posts to convert.")
            }
            return result
        }


        /**
         * Process any videos found within an Element's hierarchy.
         * 
         * This will look for appropriate video elements, and rewrite or replace them as necessary so
         * the app can display them according to the user's preferences.
         * This mutates the supplied Element's structure.
         * @param contentNode       the Element to search and edit
         * @param inlineYouTubes    whether YouTube videos should be displayed inline, or replaced with a link
         * @param inlineTiktoks     whether TikTok videos should be displayed inline, or replaced with a link
         */
        fun convertVideos(contentNode: Element, inlineYouTubes: Boolean, inlineTiktoks: Boolean) {
            val youtubeNodes = contentNode.getElementsByClass("youtube-player")

            for (youTube in youtubeNodes) {
                try {
                    val src = youTube.attr("src")
                    val youtubeMatcher: Matcher = youtubeHDId_regex.matcher(src)
                    if (youtubeMatcher.find()) {
                        val videoId = youtubeMatcher.group(1)
                        val link = "http://www.youtube.com/watch?v=$videoId"

                        val youtubeLink = Element(Tag.valueOf("a"), "")
                        youtubeLink.text(link)
                        youtubeLink.attr("href", link)
                        if (!inlineYouTubes || postElementIsNMWSOrSpoilered(youTube)) {
                            youTube.replaceWith(youtubeLink)
                        } else {
                            youTube.after(youtubeLink)
                            youtubeLink.before(Element(Tag.valueOf("br"), ""))
                            Element("div")
                            val youtubeContainer = Element(Tag.valueOf("div"), "")
                            youtubeContainer.addClass("videoWrapper")
                            youTube.before(youtubeContainer)
                            youtubeContainer.appendChild(youTube)
                            youTube.attr(
                                "sandbox",
                                youTube.attr("sandbox") + " allow-top-navigation"
                            )
                        }
                    }
                } catch (e: Exception) {
                    e(e, "Failed youtube conversion:")
                    continue  //if we fail to convert the video tag, we can still display the rest.
                }
            }

            /*
         * TikTok URL forms seem to be:
         * https://www.tiktok.com/embed/[video id = \d+]
         * https://www.tiktok.com/@[username]/video/[video id]
         * there are more but they don't relate to embedding and don't appear to have video IDs associated
        */
            val tiktokNodes = contentNode.getElementsByClass("tiktok-player")

            for (tiktok in tiktokNodes) {
                try {
                    val src = tiktok.attr("src")
                    val tiktokMatcher: Matcher = tiktokId_regex.matcher(src)
                    if (tiktokMatcher.find()) {
                        val videoId = tiktokMatcher.group(1)
                        // usernames aren't included in the embed link format, thankfully they don't matter
                        val linkURLPrefix = "https://www.tiktok.com/@/video/"
                        val link = linkURLPrefix + videoId

                        val tiktokLink = Element(Tag.valueOf("a"), "")
                        tiktokLink.text(link)
                        tiktokLink.attr("href", link)
                        if (!inlineTiktoks || postElementIsNMWSOrSpoilered(tiktok)) {
                            tiktok.replaceWith(tiktokLink)
                        } else {
                            tiktok.after(tiktokLink)
                            tiktokLink.before(Element(Tag.valueOf("br"), ""))
                        }
                    }
                } catch (e: Exception) {
                    e(e, "Failed TikTok conversion:")
                    continue
                }
            }

            val videoNodes = contentNode.getElementsByClass("bbcode_video")
            for (node in videoNodes) {
                try {
                    var src: String? = null
                    var height = 0
                    var width = 0
                    val `object` = node.getElementsByTag("object")
                    if (`object`.isNotEmpty()) {
                        height = `object`[0].attr("height").toInt()
                        width = `object`[0].attr("width").toInt()
                        val emb = `object`[0].getElementsByTag("embed")
                        if (emb.isNotEmpty()) {
                            src = emb[0].attr("src")
                        }
                    }
                    if (src != null && height != 0 && width != 0) {
                        var link: String?
                        val vimeo: Matcher = vimeoId_regex.matcher(src)
                        if (vimeo.find()) {
                            val videoId = vimeo.group(1)
                            val vimeoXML: Element?
                            try {
                                vimeoXML = get("http://vimeo.com/api/v2/video/$videoId.xml")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                continue
                            }
                            link = if (vimeoXML?.getElementsByTag("mobile_url")?.first() != null) {
                                vimeoXML.getElementsByTag("mobile_url").first()?.text()
                            } else {
                                vimeoXML?.getElementsByTag("url")?.first()?.text()
                            }
                        } else {
                            node.empty()
                            val ln = Element(Tag.valueOf("a"), "")
                            ln.attr("href", src)
                            ln.text(src)
                            node.replaceWith(ln)
                            continue
                        }
                        link?.let {
                            node.empty()
                            val ln = Element(Tag.valueOf("a"), "")
                            ln.attr("href", it)
                            ln.text(it)
                            node.replaceWith(ln)
                        }
                    }
                } catch (e: Exception) {
                    e(e, "Failed video conversion:")
                    continue  //if we fail to convert the video tag, we can still display the rest.
                }
            }
        }

        /**
         * Duplicates logic from embedding.js. Make changes in both locations!
         * @param postElement Must have a containing .postbody element
         * @return boolean
         */
        private fun postElementIsNMWSOrSpoilered(postElement: Element): Boolean {
            return postElement.closest(".postbody")?.selectFirst("img[title=':nws:'], img[title=':nms:']") != null
                    || postElement.parent()?.hasClass("bbc-spoiler") ?: false
        }

        /**
         * Parse a thread page to grab its post data.
         * 
         * @param content
         * @param aThread
         * @param aThreadId
         * @param unreadIndex
         * @param opId
         * @param prefs
         * @param startIndex
         * @return the number of posts found on the page
         */
        fun syncPosts(
            content: ContentResolver,
            aThread: Document,
            aThreadId: Int,
            unreadIndex: Int,
            opId: Int,
            prefs: AwfulPreferences,
            startIndex: Int
        ): Int {
            val result: MutableList<ContentValues> =
                parsePosts(aThread, aThreadId, unreadIndex, opId, prefs, startIndex)
            val resultCount = content.bulkInsert(CONTENT_URI, result.toTypedArray<ContentValues?>())
            i("Inserted $resultCount posts into DB, threadId:$aThreadId unreadIndex: $unreadIndex")
            return resultCount
        }


        fun parsePosts(
            aThread: Document,
            aThreadId: Int,
            unreadIndex: Int,
            opId: Int,
            prefs: AwfulPreferences,
            startIndex: Int
        ): MutableList<ContentValues> {
            var index = startIndex
            val updateTime = Timestamp(System.currentTimeMillis()).toString()

            val posts = aThread.getElementsByClass("post")
            val parseTasks: MutableList<Callable<ContentValues>> = ArrayList(posts.size)
            for (postData in posts) {
                parseTasks.add(
                    PostParseTask(
                        postData,
                        updateTime,
                        index,
                        unreadIndex,
                        aThreadId,
                        opId,
                        prefs
                    )
                )
                index++
            }

            val startTime = System.currentTimeMillis()
            // parse posts using multithreading if possible - some of the Jsoup calls (#html in particular) are very slow
            // (#html should be a lot faster when jsoup updates to handle Windows-1252 encoding user their fast path for Entities#canEncode)
            val result: MutableList<ContentValues> = parse(parseTasks).toMutableList()
            val averageParseTime = (System.currentTimeMillis() - startTime) / parseTasks.size.toFloat()
            i(
                "%d posts found, %d posts parsed\nAverage parse time: %.3fms",
                posts.size,
                result.size,
                averageParseTime
            )
            return result
        }


        /**
         * Process an img element from a post, to make it display correctly in the app.
         * 
         * 
         * This performs any necessary conversion, referring to user preferences to determine if e.g.
         * images should be loaded or converted to a link. This mutates the supplied Element.
         * 
         * @param img        an Element represented by an img tag
         * @param isOldImage whether this is an old image (from a previously seen post), may be hidden
         * @param prefs      preferences used to make decisions
         */
        fun processPostImage(img: Element, isOldImage: Boolean, prefs: AwfulPreferences) {
            //don't alter video mock buttons
            if (img.hasClass("videoPlayButton")) {
                return
            }
            tryConvertToHttps(img)
            val isTimg = img.hasClass("timg")
            var originalUrl = img.attr("src")

            // Fix postimg.org images
            if (originalUrl.contains("postimg.org")) {
                originalUrl = originalUrl.replace(".org/", ".cc/")
            }
            // check whether images can be converted to / wrapped in a link
            val alreadyLinked = img.parent()?.tagName().equals("a", ignoreCase = true)
            val linkOk = !img.hasClass("nolink")

            // image is a smiley - if required, replace it with its :code: (held in the 'title' attr)
            if (img.hasAttr("title")) {
                if (!prefs.showSmilies) {
                    val name = img.attr("title")
                    img.replaceWith(Element(Tag.valueOf("span"), "").text(name))
                }
                return
            }

            // image shouldn't be displayed - convert to link / plaintext url
            // if image is wrapped in an <a>, make a link to image and the <a>
            if (isOldImage && prefs.hideOldImages || !prefs.canLoadImages()) {
                if (!linkOk) {
                    img.replaceWith(
                        Element(Tag.valueOf("span"), "")
                            .text(originalUrl)
                            .attr("class", "link-no-ok")
                    )
                } else if (alreadyLinked) {
                    val newParent = Element(Tag.valueOf("span"), "").attr("class", "converted-to-link")
                    val parent = img.parent()!!

                    parent
                        .appendText(parent.attr("href"))
                        .attr("class", "a-link")

                    parent.parent()?.insertChildren(
                        parent.elementSiblingIndex(),
                        newParent
                    ) // set the image as the first child of the div
                    newParent.appendChild(parent)
                    newParent.appendChild(img)
                    img.replaceWith(
                        Element(Tag.valueOf("a"), "")
                            .attr("href", originalUrl)
                            .text(originalUrl)
                            .attr("class", "img-link")
                    )
                } else {
                    // switch out for a link with the url
                    img.replaceWith(
                        Element(Tag.valueOf("a"), "").attr("href", originalUrl).text(originalUrl)
                    )
                }
                return
            }

            // normal image - if we can't link it (e.g. to turn into an expandable thumbnail) there's nothing else to do
            if (!linkOk || alreadyLinked) {
                return
            }

            // handle linking, thumbnailing, gif conversion etc

            // default to the 'thumbnail' url just being the full image
            var thumbUrl = originalUrl

            // thumbnail any imgur images according to user prefs, if set
            if (prefs.imgurThumbnails != "d" && thumbUrl.contains("i.imgur.com")) {
                thumbUrl = imgurAsThumbnail(thumbUrl, prefs.imgurThumbnails ?: "d")
            }

            // handle GIFs - different cases for different sites
            if (prefs.disableGifs && thumbUrl.contains(".gif", true)) {
                if (thumbUrl.contains("imgur.com", true)) {
                    thumbUrl = imgurAsThumbnail(thumbUrl, "h")
                } else if (thumbUrl.contains( "i.kinja-img.com", true)) {
                    thumbUrl = thumbUrl.replace(".gif", ".jpg")
                } else if (thumbUrl.contains( "giphy.com", true)) {
                    thumbUrl = thumbUrl.replace("://i.giphy.com", "://media.giphy.com/media")
                    thumbUrl = if (thumbUrl.endsWith("giphy.gif")) {
                        thumbUrl.replace("giphy.gif", "200_s.gif")
                    } else {
                        thumbUrl.replace(".gif", "/200_s.gif")
                    }
                } else if (thumbUrl.contains( "giant.gfycat.com", true)) {
                    thumbUrl = thumbUrl.replace("giant.gfycat.com", "thumbs.gfycat.com")
                    thumbUrl = thumbUrl.replace(".gif", "-poster.jpg")
                } else {
                    thumbUrl = "file:///android_asset/images/gif.png"
                    img.attr("width", "200px")
                }

                // link and rewrite image, setting the link as click-to-play
                thumbnailAndLink(img, thumbUrl).addClass("playGif")
                return
            }

            // non-gif images - wrap them in a link, unless handling as a TIMG (to avoid breaking its click behaviour)
            if (!isTimg || prefs.disableTimgs) {
                // if the image hasn't been processed then thumbUrl will be the original image URL, i.e. a full-size image
                thumbnailAndLink(img, thumbUrl)
            }
        }


        /**
         * Rewrite a Imgur image url as a thumbnailed version, if possible (e.g. not already a thumbnail).
         * 
         * @param imgurUrl      the image url to rewrite
         * @param thumbnailCode the type of thumbnail, usually a single character code
         * @return the rewritten url, or the original if it couldn't be rewritten
         */
        private fun imgurAsThumbnail(imgurUrl: String, thumbnailCode: String): String {
            var imgurUrl = imgurUrl
            val match: Matcher = imgurId_regex.matcher(imgurUrl)
            if (match.find()) {
                val imgurBase = match.group(1)!!
                val imgurImageId = match.group(2)!!
                val imgurImageEnd = match.group(3)

                //check if already thumbnails
                if (imgurImageId.length != 6 && imgurImageId.length != 8) {
                    imgurUrl = imgurBase + imgurImageId + thumbnailCode + imgurImageEnd
                }
            }
            return imgurUrl
        }


        /**
         * Rewrite an image element as a thumbnail, wrapping it in a link to the original image URL.
         * 
         * 
         * The image will be replaced in the DOM by the anchor element, with the image as its child, and
         * the anchor returned. Classes on the image element are cleared.
         * 
         * @param img          the image element
         * @param thumbnailUrl the URL for the wrapped image
         * @return the link element, containing the modified image element
         */
        private fun thumbnailAndLink(img: Element, thumbnailUrl: String): Element {
            val link = Element(Tag.valueOf("a"), "").attr("href", img.attr("src"))
            // rewrite the image (new src, no classes) and wrap it with the link in the DOM
            img.attr("src", thumbnailUrl)
            img.classNames(mutableSetOf())
            img.replaceWith(link)
            link.appendChild(img)
            return link
        }


        /**
         * Converts URLs to https versions, where appropriate.
         * 
         * 
         * This mutates the element directly.
         */
        fun tryConvertToHttps(element: Element) {
            var url: String?

            // get the element's url attribute, give up if it doesn't have one
            val attr = if (element.hasAttr("href")) {
                "href"
            } else if (element.hasAttr("src")) {
                "src"
            } else {
                return
            }

            // if the element's url is for a https-able domain, rewrite it
            for (domain in HTTPS_SUPPORTED_DOMAINS) {
                url = element.attr(attr)
                if (url.contains(domain.toString(), true)) {
                    element.attr(attr, url.replace("http://", "https://"))
                    return
                }
            }
        }


        /**
         * Converts bad post markers to links, when found.
         * 
         * 
         * This mutates the element directly.
         */
        fun setBanlistLinks(element: Element, userId: Int, postid: String) {
            // get the element's url attribute, give up if it doesn't have one
            val match: Matcher = badPost_regex.matcher(element.text())
            if (match.find()) {
                val url = Constants.FUNCTION_BANLIST + "?userid=" + userId + "#from" + postid
                val text = element.text()
                val link = Element(Tag.valueOf("a"), "")
                link.attr("href", url)
                link.text(text)
                element.empty().appendChild(link)
            }
        }
    }
}
