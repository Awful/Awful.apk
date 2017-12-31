/**
 * *****************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 *
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
 * *****************************************************************************
 */

package com.ferg.awfulapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.reply.MessageComposer
import com.ferg.awfulapp.task.*
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulMessage.*
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.util.*
import org.apache.commons.lang3.StringUtils
import timber.log.Timber
import java.io.File

class PostReplyFragment : AwfulFragment() {

    // UI components
    @BindView(R.id.thread_title) @JvmField var threadTitleView: TextView? = null
    lateinit var messageComposer: MessageComposer
    lateinit private var progressDialog: ProgressDialog

    // internal state
    private var savedDraft: SavedDraft? = null
    private var replyData: ContentValues? = null
    private var attachmentData: Intent? = null
    private var saveRequired = true

    // async stuff
    lateinit private var mContentResolver: ContentResolver
    private val draftLoaderCallback: DraftReplyLoaderCallback by lazy { DraftReplyLoaderCallback() }
    private val threadInfoCallback: ThreadInfoCallback by lazy { ThreadInfoCallback() }

    // thread/reply metadata
    private var mThreadId: Int = 0
    private var mPostId: Int = 0
    private var mReplyType: Int = 0
    private var mThreadTitle: String? = null

    // User's reply data
    private var mFileAttachment: String? = null
    private var disableEmotes = false
    private var postSignature = false


    ///////////////////////////////////////////////////////////////////////////
    // Activity and fragment initialisation
    ///////////////////////////////////////////////////////////////////////////


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.w("onCreate")
        setHasOptionsMenu(true)
        retainInstance = false
    }


    override fun onCreateView(aInflater: LayoutInflater, aContainer: ViewGroup?, aSavedState: Bundle?): View? {
        super.onCreateView(aInflater, aContainer, aSavedState)
        Timber.w("onCreateView")
        return inflateView(R.layout.post_reply, aContainer, aInflater)
    }


    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        Timber.w("onActivityCreated")
        ButterKnife.bind(this, activity!!)

        messageComposer = childFragmentManager.findFragmentById(R.id.message_composer_fragment) as MessageComposer
        messageComposer.setBackgroundColor(ColorProvider.BACKGROUND.color)
        messageComposer.setTextColor(ColorProvider.PRIMARY_TEXT.color)


        // grab all the important reply params
        val intent = activity!!.intent
        mReplyType = intent.getIntExtra(Constants.EDITING, -999)
        mPostId = intent.getIntExtra(Constants.REPLY_POST_ID, 0)
        mThreadId = intent.getIntExtra(Constants.REPLY_THREAD_ID, 0)
        setTitle(getTitle())

        // perform some sanity checking
        var badRequest = false
        if (mReplyType < 0 || mThreadId == 0) {
            // we always need a valid type and thread ID
            badRequest = true
        } else if (mPostId == 0 && (mReplyType == TYPE_EDIT || mReplyType == TYPE_QUOTE)) {
            // edits and quotes always need a post ID too
            badRequest = true
        }
        if (badRequest) {
            makeToast("Can't create reply! Bad parameters")
            val template = "Failed to init reply activity%nReply type: %d, Thread ID: %d, Post ID: %d"
            Timber.w(String.format(template, mReplyType, mThreadId, mPostId))
            activity?.finish()
        }

        mContentResolver = activity!!.contentResolver
        // load any related stored draft before starting the reply request
        // TODO: 06/04/2017 probably better to handle this as two separate, completable requests - combine reply and draft data when they're both finished, instead of assuming the draft loader finishes first
        getStoredDraft()
        refreshThreadInfo()
        loadReply(mReplyType, mThreadId, mPostId)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == ADD_ATTACHMENT) {
            if (AwfulUtils.isMarshmallow()) {
                val permissionCheck = ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE)
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    this.attachmentData = data
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE)
                } else {
                    addAttachment(data)
                }
            } else {
                addAttachment(data)
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE ->
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addAttachment()
                } else {
                    makeToast(R.string.no_file_permission_attachment)
                }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    private fun getStoredDraft() {
        restartLoader(Constants.REPLY_LOADER_ID, null, draftLoaderCallback)
    }

    private fun refreshThreadInfo() {
        restartLoader(Constants.MISC_LOADER_ID, null, threadInfoCallback)
    }


    ///////////////////////////////////////////////////////////////////////////
    // Fetching data/drafts and populating editor
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Initiate a new reply/edit by passing a request to the site and handling its response.
     *
     * @param mReplyType The type of request we're making (reply/quote/edit)
     * @param mThreadId  The ID of the thread
     * @param mPostId    The ID of the post being edited/quoted, if applicable
     */
    private fun loadReply(mReplyType: Int, mThreadId: Int, mPostId: Int) {
        progressDialog = ProgressDialog.show(activity, "Loading", "Fetching Message...", true, true)

        // create a callback to handle the reply data from the site
        val loadCallback = object : AwfulRequest.AwfulResultCallback<ContentValues> {
            override fun success(result: ContentValues) {
                replyData = result
                if (result.containsKey(AwfulMessage.REPLY_CONTENT)) {
                    // update the message composer with the provided reply data
                    var replyData = NetworkUtils.unencodeHtml(result.getAsString(AwfulMessage.REPLY_CONTENT))
                    if (!TextUtils.isEmpty(replyData)) {
                        if (replyData.endsWith("[/quote]")) {
                            replyData += "\n\n"
                        }
                        messageComposer.setText(replyData, true)
                    } else {
                        messageComposer.setText(null, false)
                    }
                    // set any options and update the menu
                    postSignature = getCheckedAndRemove(REPLY_SIGNATURE, result)
                    disableEmotes = getCheckedAndRemove(REPLY_DISABLE_SMILIES, result)
                    invalidateOptionsMenu()
                }
                dismissProgressDialog()
                handleDraft()
            }

            override fun failure(error: VolleyError) {
                dismissProgressDialog()
                //allow time for the error to display, then close the window
                mHandler.postDelayed({ leave(RESULT_CANCELLED) }, 3000)
            }
        }
        when (mReplyType) {
            TYPE_NEW_REPLY -> queueRequest(ReplyRequest(activity, mThreadId).build(this, loadCallback))
            TYPE_QUOTE -> queueRequest(QuoteRequest(activity, mThreadId, mPostId).build(this, loadCallback))
            TYPE_EDIT -> queueRequest(EditRequest(activity, mThreadId, mPostId).build(this, loadCallback))
            else -> {
                // TODO: 13/04/2017 make an enum/intdef for reply types and just fail early if necessary, shouldn't need to keep checking for bad values everywhere
                makeToast("Unknown reply type: " + mReplyType)
                leave(RESULT_CANCELLED)
            }
        }
    }

    /**
     * Removes a key from a ContentValues, returning true if it was set to "checked"
     */
    private fun getCheckedAndRemove(key: String, values: ContentValues): Boolean {
        if (!values.containsKey(key)) {
            return false
        }
        val checked = "checked" == values.getAsString(key)
        values.remove(key)
        return checked
    }


    /**
     * Take care of any saved draft, allowing the user to use it if appropriate.
     */
    private fun handleDraft() {
        // this implicitly relies on the Draft Reply Loader having already finished, assigning to savedDraft if it found any draft data
        /*
           This is where we decide whether to load an existing draft, or ignore it.
           The saved draft will end up getting replaced/deleted anyway (when the post is either posted or saved),
           this just decides whether it's relevant to the current context, and the user needs to know about it.

           We basically ignore a draft if:
           - we're currently editing a post, and the draft isn't an edit
           - the draft is an edit, but not for this post
           in both cases we need to avoid replacing the original post (that we're trying to edit) with some other post's draft
        */
        savedDraft?.let { draft ->
            // TODO: 11/04/2017 might be better to treat edits and posts/quotes as two separate things, so you can have 1 post and 1 edit saved per thread without them deleting each other
            if (draft.type == TYPE_EDIT && draft.postId != mPostId || draft.type != TYPE_EDIT && mReplyType == TYPE_EDIT) {
                return
            }
            // got a useful draft, let the user decide what to do with it
            displayDraftAlert(draft)
        }
    }


    /**
     * Display a dialog allowing the user to use or discard an existing draft.
     *
     * @param draft a draft message relevant to this post
     */
    private fun displayDraftAlert(draft: SavedDraft) {
        val activity = activity ?: return

        val template = "You have a %s:" +
                "<br/><br/>" +
                "<i>%s</i>" +
                "<br/><br/>" +
                "Saved %s ago"

        val type = when (draft.type) {
            TYPE_EDIT -> "Saved Edit"
            TYPE_QUOTE -> "Saved Quote"
            TYPE_NEW_REPLY -> "Saved Reply"
            else -> "Saved Reply"
        }

        val maxLength = 140
        var previewText = StringUtils.substring(draft.content, 0, maxLength).replace("\\n".toRegex(), "<br/>")
        if (draft.content.length > maxLength) {
            previewText += "..."
        }

        val message = String.format(template, type.toLowerCase(), previewText, AwfulUtils.epochToSimpleDuration(draft.timestamp))
        val positiveLabel = if (mReplyType == TYPE_QUOTE) "Add" else "Use"
        AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_reply_dark)
                .setTitle(type)
                .setMessage(AwfulUtils.fromHtml(message))
                .setPositiveButton(positiveLabel) { _, _ ->
                    var newContent = draft.content
                    // If we're quoting something, stick it after the draft reply (and add some whitespace too)
                    if (mReplyType == TYPE_QUOTE) {
                        newContent += "\n\n" + messageComposer.text
                    }
                    messageComposer.setText(newContent, true)
                }
                .setNegativeButton(R.string.discard) { _, _ -> deleteSavedReply() }
                // avoid accidental draft losses by forcing a decision
                .setCancelable(false)
                .show()
    }


    ///////////////////////////////////////////////////////////////////////////
    // Send/preview posts
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Display a dialog allowing the user to submit or preview their post
     */
    private fun showSubmitDialog() {
        val activity = activity ?: return
        AlertDialog.Builder(activity)
                .setTitle(String.format("Confirm %s?", if (mReplyType == TYPE_EDIT) "Edit" else "Post"))
                .setPositiveButton(R.string.submit) { _, _ ->
                    progressDialog = ProgressDialog.show(activity, "Posting", "Hopefully it didn't suck...", true, true)
                    saveReply()
                    submitPost()
                }
                .setNeutralButton(R.string.preview) { _, _ -> previewPost() }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
    }


    /**
     * Actually submit the post/edit to the site.
     */
    private fun submitPost() {
        val cv = prepareCV() ?: return
        val postCallback = object : AwfulRequest.AwfulResultCallback<Void> {
            override fun success(result: Void) {
                dismissProgressDialog()
                deleteSavedReply()
                saveRequired = false

                makeToast(R.string.post_sent)
                mContentResolver.notifyChange(AwfulThread.CONTENT_URI, null)
                leave(if (mReplyType == TYPE_EDIT) mPostId else RESULT_POSTED)
            }

            override fun failure(error: VolleyError) {
                dismissProgressDialog()
                saveReply()
            }
        }
        when (mReplyType) {
            TYPE_QUOTE, TYPE_NEW_REPLY -> queueRequest(SendPostRequest(activity, cv).build(this, postCallback))
            TYPE_EDIT -> queueRequest(SendEditRequest(activity, cv).build(this, postCallback))
        }
    }


    /**
     * Request a preview of the current post from the site, and display it.
     */
    private fun previewPost() {
        val cv = prepareCV() ?: return
        val previewFrag = PreviewFragment()
        previewFrag.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
        previewFrag.show(fragmentManager!!, "Post Preview")
        // TODO: 12/02/2017 this result should already be on the UI thread?
        val previewCallback = object : AwfulRequest.AwfulResultCallback<String> {
            override fun success(result: String) {
                awfulActivity?.runOnUiThread { previewFrag.setContent(result) }
            }

            override fun failure(error: VolleyError) {
                previewFrag.dismiss()
                view?.let { v ->
                    Snackbar.make(v, "Preview failed.", Snackbar.LENGTH_LONG)
                            .setAction("Retry") { _ -> previewPost() }.show()
                }
            }
        }
        if (mReplyType == TYPE_EDIT) {
            queueRequest(PreviewEditRequest(activity, cv).build(this, previewCallback))
        } else {
            queueRequest(PreviewPostRequest(activity, cv).build(this, previewCallback))
        }
    }


    /**
     * Create a ContentValues representing the current post and its options.
     * Returns null if the data is invalid, e.g. an empty post
     * @return The post data, or null if there was an error.
     */
    private fun prepareCV(): ContentValues? {
        if (replyData == null || replyData?.getAsInteger(AwfulMessage.ID) == null) {
            // TODO: if this ever happens, the ID never gets set (and causes an NPE in SendPostRequest) - handle this in a better way?
            // Could use the mThreadId value, but that might be incorrect at this point and post to the wrong thread? Is null reply data an exceptional event?
            Timber.e("No reply data in sendPost() - no thread ID to post to!")
            makeToast("Unknown thread ID - can't post!")
            return null
        }
        val cv = ContentValues(replyData)
        if (replyIsEmpty()) {
            dismissProgressDialog()
            AlertBuilder().setTitle(R.string.message_empty)
                    .setSubtitle(R.string.message_empty_subtext)
                    .show()
            return null
        }
        if (!TextUtils.isEmpty(mFileAttachment)) {
            cv.put(AwfulMessage.REPLY_ATTACHMENT, mFileAttachment)
        }
        if (postSignature) {
            cv.put(REPLY_SIGNATURE, Constants.YES)
        }
        if (disableEmotes) {
            cv.put(AwfulMessage.REPLY_DISABLE_SMILIES, Constants.YES)
        }
        cv.put(AwfulMessage.REPLY_CONTENT, messageComposer.text)
        return cv
    }


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle/navigation stuff
    ///////////////////////////////////////////////////////////////////////////


    override fun onResume() {
        super.onResume()
        Timber.w("onResume")
        updateThreadTitle()
    }

    override fun onPause() {
        super.onPause()
        Timber.w("onPause")
        cleanupTasks()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Timber.w("onDestroyView")
        // final cleanup - some should have already been done in onPause (draft saving etc)
        loaderManager.destroyLoader(Constants.REPLY_LOADER_ID)
        loaderManager.destroyLoader(Constants.MISC_LOADER_ID)
    }

    /**
     * Tasks to perform when the reply window moves from the foreground.
     * Basically saves a draft if required, and hides elements like the keyboard
     */
    private fun cleanupTasks() {
        autoSave()
        dismissProgressDialog()
        messageComposer.hideKeyboard()
    }



    /**
     * Finish the reply activity, performing cleanup and returning a result code to the activity that created it.
     */
    private fun leave(activityResult: Int) {
        activity?.let { context ->
            context.setResult(activityResult)
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            view?.let { v ->
                imm.hideSoftInputFromWindow(v.applicationWindowToken, 0)
            }
            context.finish()
        }
    }


    /**
     * Call this when the user tries to leave the activity, so the Save/Discard dialog can be shown if necessary.
     */
    fun onNavigateBack() {
        val activity = activity
        if (activity == null) {
            return
        } else if (replyIsEmpty()) {
            leave(RESULT_CANCELLED)
            return
        }
        AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_reply_dark)
                .setMessage(String.format("Save this %s?", if (mReplyType == TYPE_EDIT) "edit" else "post"))
                .setPositiveButton(R.string.save) { _, _ ->
                    // let #autoSave handle it on leaving
                    saveRequired = true
                    leave(RESULT_CANCELLED)
                }
                .setNegativeButton(R.string.discard) { _, _ ->
                    deleteSavedReply()
                    saveRequired = false
                    leave(RESULT_CANCELLED)
                }
                .setNeutralButton(R.string.cancel) { _, _ -> }
                .setCancelable(true)
                .show()

    }


    ///////////////////////////////////////////////////////////////////////////
    // Saving draft data
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Trigger a draft save, if required.
     */
    private fun autoSave() {
        if (saveRequired) {
            if (replyIsEmpty()) {
                Timber.i("Message unchanged, discarding.")
                // TODO: 12/02/2017 does this actually need to check if it's unchanged?
                deleteSavedReply()//if the reply is unchanged, throw it out.
                messageComposer.setText(null, false)
            } else {
                Timber.i("Message Unsent, saving.")
                saveReply()
            }
        }
    }


    /**
     * Delete any saved reply for the current thread
     */
    private fun deleteSavedReply() {
        mContentResolver.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID + "=?", AwfulProvider.int2StrArray(mThreadId))
    }


    /**
     * Save a draft reply for the current thread.
     */
    private fun saveReply() {
        if (mThreadId > 0) {
            val content = messageComposer.text
            // don't save if the message is empty/whitespace
            // not trimming the actual content, so we retain any whitespace e.g. blank lines after quotes
            if (!content.trim { it <= ' ' }.isEmpty()) {
                Timber.i("Saving reply! $content")
                val post = if (replyData == null) ContentValues() else ContentValues(replyData)
                post.put(AwfulMessage.ID, mThreadId)
                post.put(AwfulMessage.TYPE, mReplyType)
                post.put(AwfulMessage.REPLY_CONTENT, content)
                post.put(AwfulMessage.EPOC_TIMESTAMP, System.currentTimeMillis())
                if (mFileAttachment != null) {
                    post.put(AwfulMessage.REPLY_ATTACHMENT, mFileAttachment)
                }
                if (mContentResolver.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mThreadId.toLong()), post, null, null) < 1) {
                    mContentResolver.insert(AwfulMessage.CONTENT_URI_REPLY, post)
                }
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Menus
    ///////////////////////////////////////////////////////////////////////////


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Timber.w("onCreateOptionsMenu")
        inflater.inflate(R.menu.post_reply, menu)

        val attach = menu.findItem(R.id.add_attachment)
        if (attach != null && mPrefs != null) {
            attach.isEnabled = mPrefs.hasPlatinum
            attach.isVisible = mPrefs.hasPlatinum
        }
        val remove = menu.findItem(R.id.remove_attachment)
        if (remove != null && mPrefs != null) {
            remove.isEnabled = mPrefs.hasPlatinum && this.mFileAttachment != null
            remove.isVisible = mPrefs.hasPlatinum && this.mFileAttachment != null
        }
        menu.findItem(R.id.disableEmots)?.let {
            it.isChecked = disableEmotes
        }
        menu.findItem(R.id.signature)?.let {
            it.isChecked = postSignature
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.w("onOptionsItemSelected")
        when (item.itemId) {
            R.id.submit_button -> showSubmitDialog()
            R.id.add_attachment -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), ADD_ATTACHMENT)
            }
            R.id.remove_attachment -> {
                this.mFileAttachment = null
                makeToast(R.string.file_removed, Toast.LENGTH_SHORT)
                invalidateOptionsMenu()
            }
            R.id.signature -> {
                item.isChecked = !item.isChecked
                postSignature = item.isChecked
            }
            R.id.disableEmots -> {
                item.isChecked = !item.isChecked
                disableEmotes = item.isChecked
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }


    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        //refresh the menu to show/hide attach option (plat only)
        invalidateOptionsMenu()
    }


    ///////////////////////////////////////////////////////////////////////////
    // Attachment handling
    ///////////////////////////////////////////////////////////////////////////

    // TODO: 13/04/2017 make a separate attachment component and stick all this in there

    private fun addAttachment() {
        addAttachment(attachmentData)
        attachmentData = null
    }

    private fun addAttachment(data: Intent?) {
        getFilePath(data?.data)?.let { path ->
            val attachment = File(path)
            val filename = attachment.name
            if (!attachment.isFile || !attachment.canRead()) {
                setAttachment(null, String.format(getString(R.string.file_unreadable), filename))
                return
            } else if (!StringUtils.endsWithAny(filename.toLowerCase(), ".jpg", ".jpeg", ".png", ".gif")) {
                setAttachment(null, String.format(getString(R.string.file_wrong_filetype), filename))
                return
            } else if (attachment.length() > ATTACHMENT_MAX_BYTES) {
                setAttachment(null, String.format(getString(R.string.file_too_big), filename))
                return
            }

            // check the image size without creating a bitmap
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            val height = options.outHeight
            val width = options.outWidth
            if (width > ATTACHMENT_MAX_WIDTH || height > ATTACHMENT_MAX_HEIGHT) {
                setAttachment(null, String.format(getString(R.string.file_resolution_too_big), filename, width, height))
                return
            }

            setAttachment(path, String.format(getString(R.string.file_attached), filename))
            return
        }

        setAttachment(null, getString(R.string.file_error))
    }


    private fun setAttachment(attachment: String?, toastMessage: String) {
        mFileAttachment = attachment
        makeToast(toastMessage)
        invalidateOptionsMenu()
    }


    @SuppressLint("NewApi")
    private fun getFilePath(uri: Uri?): String? {
        if (uri == null) return null

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(this.activity, uri)) {

            if (uri.isExternalStorageDocument()) {
                // ExternalStorageProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                // TODO handle non-primary volumes

            } else if (uri.isDownloadsDocument()) {
                // DownloadsProvider

                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)!!)

                return getDataColumn(contentUri, null, null)
            } else if (uri.isMediaDocument()) {
                // MediaProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                val contentUri = when (type) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> null
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address
            return if (uri.isGooglePhotosUri()) uri.lastPathSegment else getDataColumn(uri, null, null)

        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(uri: Uri?, selection: String?,
                              selectionArgs: Array<String>?): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = activity?.contentResolver?.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }


    ///////////////////////////////////////////////////////////////////////////
    // UI things
    ///////////////////////////////////////////////////////////////////////////


    private fun dismissProgressDialog() {
        if (progressDialog.isShowing) progressDialog.dismiss()
    }


    /**
     * Update the title view to show the current thread title, if we have it
     */
    private fun updateThreadTitle() {
        mThreadTitle?.let {
            threadTitleView?.text = it
        }
    }

    override fun getTitle() = when (mReplyType) {
        TYPE_EDIT -> "Editing"
        TYPE_QUOTE -> "Quote"
        TYPE_NEW_REPLY -> "Reply"
        else -> "Reply"
    }


    ///////////////////////////////////////////////////////////////////////////
    // Async classes etc
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Provides a Loader that pulls draft data for the current thread from the DB.
     */
    private inner class DraftReplyLoaderCallback : LoaderManager.LoaderCallbacks<Cursor> {

        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor> {
            Timber.i("Create Reply Cursor: $mThreadId")
            return CursorLoader(activity!!,
                    ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mThreadId.toLong()),
                    AwfulProvider.DraftPostProjection, null, null, null)
        }

        override fun onLoadFinished(aLoader: Loader<Cursor>, aData: Cursor) {
            if (aData.isClosed || !aData.moveToFirst()) {
                // no draft saved for this thread
                return
            }
            // if there's some quote data, deserialise it into a SavedDraft
            val quoteData = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_CONTENT))
            if (TextUtils.isEmpty(quoteData)) return
            val draftType = aData.getInt(aData.getColumnIndex(AwfulMessage.TYPE))
            val postId = aData.getInt(aData.getColumnIndex(AwfulPost.EDIT_POST_ID))
            val draftTimestamp = aData.getLong(aData.getColumnIndex(AwfulMessage.EPOC_TIMESTAMP))
            val draftReply = NetworkUtils.unencodeHtml(quoteData)

            savedDraft = SavedDraft(draftType, draftReply, postId, draftTimestamp)
            Timber.i("$draftType Saved reply message: $draftReply")

        }

        override fun onLoaderReset(aLoader: Loader<Cursor>) {}
    }


    /**
     * Provides a Loader that gets metadata for the current thread, and dsiplays its title
     */
    private inner class ThreadInfoCallback : LoaderManager.LoaderCallbacks<Cursor> {

        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor> {
            return CursorLoader(activity!!, ContentUris.withAppendedId(AwfulThread.CONTENT_URI, mThreadId.toLong()),
                    AwfulProvider.ThreadProjection, null, null, null)
        }

        override fun onLoadFinished(aLoader: Loader<Cursor>, aData: Cursor) {
            Timber.v("Thread title finished, populating.")
            if (aData.moveToFirst()) {
                mThreadTitle = aData.getString(aData.getColumnIndex(AwfulThread.TITLE))
                updateThreadTitle()
            }
        }

        override fun onLoaderReset(aLoader: Loader<Cursor>) {}
    }


    // Utility method to check if the composer contains an empty post
    private fun replyIsEmpty() = messageComposer.text.trim { it <= ' ' }.isEmpty()

    private data class SavedDraft(val type: Int, val content: String, val postId: Int, val timestamp: Long)

    companion object {
        @JvmField
        val REQUEST_POST = 5
        val RESULT_POSTED = 6
        val RESULT_CANCELLED = 7
        val RESULT_EDITED = 8
        val ADD_ATTACHMENT = 9
    }
}
