package com.ferg.awfulapp.thread

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.util.AwfulUtils
import kotlin.math.ceil

class AwfulURL {
    enum class TYPE {
        FORUM, THREAD, POST, EXTERNAL, NONE, INDEX, BANLIST, MODQUEUE_REQUEST
    }

    var id: Long = 0
        private set
    var page: Long = 1
        private set
    private var perPage = Constants.ITEMS_PER_PAGE
    private var externalURL: String? = null
    var type: TYPE = TYPE.NONE
        private set
    private var gotoParam: String? = null
    var fragment: String? = null
        private set

    var targetPostId = 0L
    var threadId = 0L
    var isBanRequest = false
    val isModQueueRequest: Boolean
        get() = this.type == TYPE.MODQUEUE_REQUEST

    val uRL: String
        /**
         * Returns the URL, assuming the default 40 items per page.
         * @return URL
         */
        get() = getURL(perPage)

    fun getURL(postPerPage: Int): String {
        var url: Uri.Builder? = null
        when (type) {
            TYPE.FORUM -> {
                url = if (id == Constants.USERCP_ID.toLong()) {
                    Constants.FUNCTION_USERCP.toUri().buildUpon()
                } else {
                    Constants.FUNCTION_FORUM.toUri().buildUpon()
                }
                url.appendQueryParameter(Constants.PARAM_FORUM_ID, id.toString())
                url.appendQueryParameter(Constants.PARAM_PAGE, page.toString())
            }

            TYPE.THREAD -> {
                url = Constants.FUNCTION_THREAD.toUri().buildUpon()
                url.appendQueryParameter(Constants.PARAM_THREAD_ID, id.toString())
                url.appendQueryParameter(Constants.PARAM_PER_PAGE, postPerPage.toString())
                if (gotoParam != null) {
                    url.appendQueryParameter(Constants.PARAM_GOTO, gotoParam) //goto=newpost, ect
                } else {
                    url.appendQueryParameter(
                        Constants.PARAM_PAGE, convertPerPage(
                            this.page, perPage.toLong(), postPerPage.toLong()
                        ).toString()
                    )
                }
            }

            TYPE.POST -> {
                url = Constants.FUNCTION_THREAD.toUri().buildUpon()
                url.appendQueryParameter(Constants.PARAM_GOTO, Constants.VALUE_POST)
                url.appendQueryParameter(Constants.PARAM_PER_PAGE, postPerPage.toString())
                url.appendQueryParameter(Constants.PARAM_POST_ID, id.toString())
            }

            TYPE.MODQUEUE_REQUEST -> {
                url = Constants.FUNCTION_MODQUEUE.toUri().buildUpon();
                url.appendQueryParameter(
                    Constants.PARAM_ACTION,
                    (if (isBanRequest) Constants.ACTION_REQUEST_BAN else Constants.ACTION_REQUEST_PROBATION))
                url.appendQueryParameter(Constants.PARAM_USER_ID, id.toString());
                url.appendQueryParameter(
                    Constants.PARAM_TARGET_POST_ID, targetPostId.toString()
                );
                url.appendQueryParameter(Constants.PARAM_THREAD_ID, threadId.toString());
            }

            TYPE.EXTERNAL -> return externalURL!!
            TYPE.INDEX -> return Constants.BASE_URL
            else -> {}
        }
        return (url?.toString() ?: "")
    }

    fun getPage(postPerPage: Int): Long {
        return convertPerPage(this.page, perPage.toLong(), postPerPage.toLong())
    }

    fun getPerPage(): Long {
        return perPage.toLong()
    }

    val isRedirect: Boolean
        get() = gotoParam != null

    override fun toString(): String {
        return this.uRL
    }

    fun setGoto(goTo: String?): AwfulURL {
        gotoParam = goTo
        return this
    }

    val isForumIndex: Boolean
        get() = type == TYPE.INDEX

    val isForum: Boolean
        get() = type == TYPE.FORUM

    val isThread: Boolean
        get() = type == TYPE.THREAD

    val isPost: Boolean
        get() = type == TYPE.POST

    val isExternal: Boolean
        get() = type == TYPE.EXTERNAL

    val isBanlist: Boolean
        get() = type == TYPE.BANLIST

    fun setPerPage(postPerPage: Int): AwfulURL {
        perPage = postPerPage
        return this
    }

    companion object {
        @JvmOverloads
        fun forum(id: Long, pageNum: Long = 1): AwfulURL {
            val aurl = AwfulURL()
            aurl.type = TYPE.FORUM
            aurl.id = id
            aurl.page = pageNum
            aurl.perPage = Constants.THREADS_PER_PAGE
            return aurl
        }

        fun threadUnread(id: Long): AwfulURL {
            return thread(id, 1, Constants.ITEMS_PER_PAGE, null).setGoto(Constants.VALUE_NEWPOST)
        }

        fun threadUnread(id: Long, perPage: Int): AwfulURL {
            return thread(id, 1, perPage, null).setGoto(Constants.VALUE_NEWPOST)
        }

        fun threadLastPage(id: Long): AwfulURL {
            return thread(id, 1, Constants.ITEMS_PER_PAGE, null).setGoto(Constants.VALUE_LASTPOST)
        }

        fun threadLastPage(id: Long, perPage: Int): AwfulURL {
            return thread(id, 1, perPage, null).setGoto(Constants.VALUE_LASTPOST)
        }

        @JvmOverloads
        fun thread(
            id: Long,
            pageNum: Long = 1,
            perPage: Int = Constants.ITEMS_PER_PAGE,
            goTo: String? = null
        ): AwfulURL {
            val aurl = AwfulURL()
            aurl.type = TYPE.THREAD
            aurl.id = id
            aurl.page = pageNum
            aurl.perPage = perPage
            aurl.gotoParam = goTo
            return aurl
        }

        @JvmOverloads
        fun post(id: Long, perPage: Int = Constants.ITEMS_PER_PAGE): AwfulURL {
            val aurl = AwfulURL()
            aurl.type = TYPE.POST
            aurl.id = id
            aurl.perPage = perPage
            aurl.gotoParam = Constants.VALUE_POST
            return aurl
        }

        fun parse(url: String): AwfulURL {
            val aurl = AwfulURL()
            val uri = url.toUri()
            if (uri.isRelative || (uri.host != null && uri.host!!
                    .contains("forums.somethingawful.com"))
            ) {
                if (uri.getQueryParameter(Constants.PARAM_PAGE) != null) {
                    aurl.page =
                        AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_PAGE)!!, 1)
                }
                if (uri.getQueryParameter(Constants.PARAM_PER_PAGE) != null) {
                    aurl.perPage = AwfulUtils.safeParseInt(
                        uri.getQueryParameter(Constants.PARAM_PER_PAGE)!!,
                        Constants.ITEMS_PER_PAGE
                    )
                }
                if (Constants.PATH_FORUM == uri.lastPathSegment) {
                    aurl.type = TYPE.FORUM
                    aurl.perPage = Constants.THREADS_PER_PAGE
                    if (uri.getQueryParameter(Constants.PARAM_FORUM_ID) != null) {
                        aurl.id = AwfulUtils.safeParseLong(
                            uri.getQueryParameter(Constants.PARAM_FORUM_ID)!!,
                            1
                        )
                    }
                } else if (Constants.PATH_BOOKMARKS == uri.lastPathSegment || Constants.PATH_USERCP == uri.lastPathSegment) {
                    aurl.type = TYPE.FORUM
                    aurl.perPage = Constants.THREADS_PER_PAGE
                    aurl.id = Constants.USERCP_ID.toLong()
                } else if (Constants.PATH_THREAD == uri.lastPathSegment) {
                    aurl.type = TYPE.THREAD
                    if (uri.getQueryParameter(Constants.PARAM_THREAD_ID) != null) {
                        aurl.id = AwfulUtils.safeParseLong(
                            uri.getQueryParameter(Constants.PARAM_THREAD_ID)!!,
                            0
                        )
                    }
                    if (uri.getQueryParameter(Constants.PARAM_GOTO) != null) {
                        aurl.gotoParam = uri.getQueryParameter(Constants.PARAM_GOTO)
                        if (Constants.VALUE_POST.equals(aurl.gotoParam, ignoreCase = true)) {
                            aurl.type = TYPE.POST
                            aurl.id = AwfulUtils.safeParseLong(
                                uri.getQueryParameter(Constants.PARAM_POST_ID)!!,
                                0
                            )
                        }
                    }
                    if (Constants.ACTION_SHOWPOST.equals(
                            uri.getQueryParameter(Constants.PARAM_ACTION),
                            ignoreCase = true
                        )
                    ) {
                        aurl.type = TYPE.POST
                        aurl.id = AwfulUtils.safeParseLong(
                            uri.getQueryParameter(Constants.PARAM_POST_ID)!!,
                            0
                        )
                    }
                } else if (Constants.PATH_BANLIST == uri.lastPathSegment) {
                    aurl.type = TYPE.BANLIST
                    aurl.id =
                        AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_USER_ID)!!, 0)
                } else if(Constants.PATH_MODQUEUE.equals(uri.getLastPathSegment())
                    && (Constants.ACTION_REQUEST_PROBATION.equals(uri.getQueryParameter(Constants.PARAM_ACTION))
                            || Constants.ACTION_REQUEST_BAN.equals(uri.getQueryParameter(Constants.PARAM_ACTION)))){
                    aurl.type = TYPE.MODQUEUE_REQUEST;
                    aurl.isBanRequest = Constants.ACTION_REQUEST_BAN.equals(uri.getQueryParameter(Constants.PARAM_ACTION));
                    aurl.id = AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_USER_ID)!!, 0);
                    aurl.targetPostId = AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_TARGET_POST_ID)!!, 0);
                    aurl.threadId = AwfulUtils.safeParseLong(uri.getQueryParameter(Constants.PARAM_THREAD_ID)!!, 0);
                } else if ("index.php".equals(
                        uri.lastPathSegment,
                        ignoreCase = true
                    ) || uri.path == null || uri.path!!.length < 2
                ) {
                    aurl.type = TYPE.INDEX
                } else {
                    aurl.type = TYPE.EXTERNAL
                    aurl.externalURL = url
                }
                aurl.fragment = uri.fragment
            } else {
                aurl.type = TYPE.EXTERNAL
                aurl.externalURL = url
            }
            Log.i("AwfulURL", "Parsed URL: " + aurl.uRL)
            return aurl
        }

        fun convertPerPage(originalPageNum: Long, originalPerPage: Long, newPerPage: Long): Long {
            var pageNum = originalPageNum
            if (originalPerPage != newPerPage) {
                pageNum = ceil((originalPageNum * originalPerPage).toDouble() / newPerPage).toLong()
            }
            return pageNum
        }
    }
}
