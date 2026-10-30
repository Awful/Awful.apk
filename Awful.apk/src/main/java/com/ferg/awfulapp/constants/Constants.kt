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
package com.ferg.awfulapp.constants

import com.ferg.awfulapp.BuildConfig

object Constants {
    @JvmField
    val DEBUG: Boolean = BuildConfig.DEBUG

    const val SITE_HTML_ENCODING: String = "CP1252"

    const val BASE_URL: String = "https://forums.somethingawful.com"

    const val FUNCTION_LOGIN: String = "$BASE_URL/account.php"
    const val FUNCTION_INDEX: String = "$BASE_URL/index.php"
    const val FUNCTION_BOOKMARK: String = "$BASE_URL/bookmarkthreads.php"
    const val FUNCTION_ANNOUNCEMENTS: String = "$BASE_URL/announcement.php"
    const val FUNCTION_USERCP: String = "$BASE_URL/usercp.php"
    const val FUNCTION_FORUM: String = "$BASE_URL/forumdisplay.php"
    const val FUNCTION_THREAD: String = "$BASE_URL/showthread.php"
    const val FUNCTION_POST_THREAD: String = "$BASE_URL/newthread.php"
    const val FUNCTION_POST_REPLY: String = "$BASE_URL/newreply.php"
    const val FUNCTION_EDIT_POST: String = "$BASE_URL/editpost.php"
    const val FUNCTION_MEMBER: String = "$BASE_URL/member.php"
    const val FUNCTION_MEMBER2: String = "$BASE_URL/member2.php"
    const val FUNCTION_SEARCH: String = "$BASE_URL/query.php"
    const val FUNCTION_PRIVATE_MESSAGE: String = "$BASE_URL/private.php"
    const val FUNCTION_BANLIST: String = "$BASE_URL/banlist.php"
    const val FUNCTION_RATE_THREAD: String = "$BASE_URL/threadrate.php"
    const val FUNCTION_MISC: String = "$BASE_URL/misc.php"
    const val FUNCTION_REPORT: String = "$BASE_URL/modalert.php"
    const val FUNCTION_POSTINGS: String = "$BASE_URL/postings.php"
    const val FUNCTION_MODQUEUE: String = "$BASE_URL/modqueue.php"
    const val FUNCTION_NEW_THREAD: String = "$BASE_URL/newthread.php"

    const val PATH_FORUM: String = "forumdisplay.php"
    const val PATH_THREAD: String = "showthread.php"
    const val PATH_BOOKMARKS: String = "bookmarkthreads.php"
    const val PATH_USERCP: String = "usercp.php"
    const val PATH_BANLIST: String = "banlist.php"
    const val PATH_MODQUEUE: String = "modqueue.php"

    const val ACTION_PROFILE: String = "getinfo"
    const val ACTION_SEARCH_POST_HISTORY: String = "do_search_posthistory"
    const val ACTION_NEW_MESSAGE: String = "newmessage"
    const val ACTION_NEW_THREAD: String = "newthread"
    const val ACTION_SHOWPOST: String = "showpost"
    const val ACTION_ADDLIST: String = "addlist"
    const val ACTION_QUERY: String = "query"
    const val ACTION_RESULTS: String = "results"
    const val ACTION_TOGGLE_THREAD_LOCKED: String = "openclosethread"
    const val ACTION_REQUEST_PROBATION: String = "request_probation"
    const val ACTION_REQUEST_BAN: String = "request_ban"

    const val PARAM_USER_ID: String = "userid"
    const val PARAM_USERNAME: String = "username"
    const val PARAM_PASSWORD: String = "password"
    const val PARAM_ACTION: String = "action"
    const val PARAM_THREAD_ID: String = "threadid"
    const val PARAM_PAGE: String = "pagenumber"
    const val PARAM_FORUM_ID: String = "forumid"
    const val PARAM_GOTO: String = "goto"
    const val PARAM_PER_PAGE: String = "perpage"
    const val PARAM_INDEX: String = "index"
    const val PARAM_BOOKMARK: String = "bookmark"
    const val PARAM_PRIVATE_MESSAGE_ID: String = "privatemessageid"
    const val PARAM_VOTE: String = "vote"
    const val PARAM_POST_ID: String = "postid"
    const val PARAM_TARGET_POST_ID: String = "tpostid"
    const val PARAM_USERLIST: String = "userlist"
    const val PARAM_FORMKEY: String = "formkey"
    const val PARAM_FORM_COOKIE: String = "form_cookie"
    const val PARAM_ATTACHMENT: String = "attachment"
    const val PARAM_FOLDERID: String = "folderid"
    const val PARAM_SHOWALL: String = "showall"
    const val PARAM_QUERY: String = "q"
    const val PARAM_QID: String = "qid"
    const val PARAM_FORUMS: String = "forums[%d]"
    const val PARAM_SUBMIT: String = "submit"
    const val PARAM_PREVIEW: String = "preview"
    const val PARAM_PARSEURL: String = "parseurl"
    const val PARAM_ATTACHMENT_ACTION: String = "attachmentaction"

    const val USERLIST_IGNORE: String = "ignore"
    const val USERLIST_BUDDY: String = "buddy"

    const val VALUE_POST: String = "post"
    const val VALUE_NEWPOST: String = "newpost"
    const val VALUE_LASTPOST: String = "lastpost"

    const val FRAGMENT_PTI: String = "pti"

    // Intent parameters
    const val FORUM: String = "forum"
    const val FORUM_ID: String = "forum_id"
    const val THREAD: String = "thread"
    const val THREAD_ID: String = "thread_id"
    const val POST_ID: String = "post_id"
    const val QUOTE: String = "quote"
    const val PAGE: String = "page"
    const val EDITING: String = "editing"
    const val MODAL: String = "modal"
    const val SHORTCUT: String = "shortcut"
    const val PRIVATE_MESSAGE: String = "private"
    const val THREAD_FRAGMENT: String = "fragment"

    const val FORM_KEY: String = "form_key"
    const val FORMKEY: String = "formkey"

    const val PREFERENCES: String = "prefs"

    const val COOKIE_DOMAIN: String = "forums.somethingawful.com"
    const val COOKIE_PATH: String = "/"
    const val COOKIE_NAME_USERID: String = "bbuserid"
    const val COOKIE_NAME_PASSWORD: String = "bbpassword"
    const val COOKIE_NAME_SESSIONID: String = "sessionid"
    const val COOKIE_NAME_SESSIONHASH: String = "sessionhash"

    const val COOKIE_PREFERENCE: String = "awful_cookie_pref"
    const val COOKIE_PREF_USERID: String = "bbuserid"
    const val COOKIE_PREF_PASSWORD: String = "bbpassword"
    const val COOKIE_PREF_SESSIONID: String = "sessionid"
    const val COOKIE_PREF_SESSIONHASH: String = "sessionhash"
    const val COOKIE_PREF_EXPIRY_DATE: String = "expiration"
    const val COOKIE_PREF_VERSION: String = "version"

    // Cloudflare cookie which is set after completing captcha challenges.
    const val COOKIE_NAME_CAPTCHA: String = "cf_clearance"
    const val COOKIE_DOMAIN_CAPTCHA: String = "somethingawful.com"

    // Content provider
    const val AUTHORITY: String = BuildConfig.APPLICATION_ID + ".provider"

    //default per-page, user configurable
    const val ITEMS_PER_PAGE: Int = 40

    //we can have up to 80 threads per forum page (SAMart)
    const val THREADS_PER_PAGE: Int = 80

    // private message folder IDs
    const val PRIVATE_MESSAGE_DEFAULT_FOLDER: Int = 0
    const val PRIVATE_MESSAGE_SENT_FOLDER: Int = -1

    // attachments
    const val ATTACHMENT_MAX_BYTES: Int = 1024 * 1024 * 2
    const val ATTACHMENT_MAX_WIDTH: Int = 4096
    const val ATTACHMENT_MAX_HEIGHT: Int = 4096

    //asynctasks are managed by ID number, but PM page has no id
    const val PRIVATE_MESSAGE_THREAD: Int = 998 //can't use negative numbers anymore.
    const val USERCP_ID: Int = 999 //can't use negative numbers anymore.
    const val FORUM_INDEX_ID: Int = 0

    /** To prevent loader ID collisions.  */
    const val REPLY_LOADER_ID: Int = 884
    const val FORUM_LOADER_ID: Int = 885
    const val SUBFORUM_LOADER_ID: Int = 886
    const val EMOTE_LOADER_ID: Int = 887
    const val MISC_LOADER_ID: Int = 888
    const val THREAD_LOADER_ID: Int = 889
    const val FORUM_THREADS_LOADER_ID: Int = 890
    const val THREAD_INFO_LOADER_ID: Int = 891
    const val POST_LOADER_ID: Int = 892
    const val FORUM_INDEX_LOADER_ID: Int = 893
    const val THREAD_DRAFT_LOADER_ID: Int = 894

    const val ACTION_DOSEND: String = "dosend"
    const val DESTINATION_TOUSER: String = "touser"
    const val PARAM_TITLE: String = "title"
    const val PARAM_MESSAGE: String = "message"
    const val PARAM_SUBJECT: String = "subject"

    const val EXTRA_BUNDLE: String = "extras"

    const val SUBMIT_REPLY: String = "Submit Reply"
    const val PREVIEW_REPLY: String = "Preview Reply"
    const val PREVIEW_POST: String = "Preview Post"

    const val YES: String = "yes" //heh
    const val DELETE: String = "delete" //heh


    //NOT FOR NETWORK USE
    const val FORUM_PAGE: String = "forum_page"

    //NOT FOR NETWORK USE
    const val THREAD_PAGE: String = "thread_page"

    const val LOGIN_ACTIVITY_REQUEST: Int = 99

    const val DEFAULT_FONT_SIZE_SP: Int = 16
    const val DEFAULT_FIXED_FONT_SIZE_SP: Int = 13
    const val MINIMUM_FONT_SIZE_SP: Int = 5

    const val TABLET_MIN_SIZE: Double = 7.0 //everything above this is considered tablet layout

    const val REPLY_POST_ID: String = "reply_post_id"
    const val REPLY_THREAD_ID: String = "reply_thread_id"

    const val POST_FORUM_ID: String = "post_forum_id"

    const val AWFUL_THREAD_ID: Int = 3571717
    const val FORUM_ID_SHSC: Int = 22
    const val FORUM_ID_YOSPOS: Int = 219
    const val FORUM_ID_FYAD: Int = 26
    const val FORUM_ID_FYAD_SUB: Int = 154

    const val FORUM_ID_BYOB: Int = 268
    const val FORUM_ID_COOL_CREW: Int = 196

    const val FORUM_ID_GOLDMINE: Int = 21

    const val SETTINGS_PAGE: String = "settings_page"

    const val AWFUL_PERMISSION_READ_EXTERNAL_STORAGE: Int = 123
    const val AWFUL_PERMISSION_WRITE_EXTERNAL_STORAGE: Int = 124
    const val AWFUL_PERMISSION_READ_MEDIA_IMAGES: Int = 125

    enum class POST_ICON_REQUEST_TYPES {
        FORUM_POST, PM
    }
}
