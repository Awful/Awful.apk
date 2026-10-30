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
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.reply.MessageComposer
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.EditRequest
import com.ferg.awfulapp.task.PreviewEditRequest
import com.ferg.awfulapp.task.PreviewPostRequest
import com.ferg.awfulapp.task.QuoteRequest
import com.ferg.awfulapp.task.ReplyRequest
import com.ferg.awfulapp.task.SendEditRequest
import com.ferg.awfulapp.task.SendPostRequest
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.util.AwfulUtils
import com.google.android.material.snackbar.Snackbar
import org.apache.commons.lang3.StringUtils
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import timber.log.Timber.Forest.v
import timber.log.Timber.Forest.w
import java.io.File
import java.util.Locale
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity

class PostReplyFragment : AwfulFragment() {
    // UI components
    private var messageComposer: MessageComposer? = null
    private var progressDialog: ProgressDialog? = null

    // internal state
    private var savedDraft: SavedDraft? = null
    private var replyData: ContentValues? = null
    private var saveRequired = true
    private var attachmentData: Intent? = null
    private var removeAttachment = false

    // async stuff
    private var mContentResolver: ContentResolver? = null
    private val draftLoaderCallback = DraftReplyLoaderCallback()
    private val threadInfoCallback = ThreadInfoCallback()

    // thread/reply metadata
    private var mThreadId = 0
    private var mPostId = 0
    private var mReplyType = 0
    private var mThreadTitle: String? = null

    // User's reply data
    private var mFileAttachment: String? = null
    private var disableEmotes = false
    private var postSignature = false


    /**//////////////////////////////////////////////////////////////////////// */ // Activity and fragment initialisation
    /**//////////////////////////////////////////////////////////////////////// */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v("onCreate")
        setHasOptionsMenu(true)
        setRetainInstance(false)
    }


    override fun onCreateView(
        aInflater: LayoutInflater,
        aContainer: ViewGroup?,
        aSavedState: Bundle?
    ): View {
        super.onCreateView(aInflater, aContainer, aSavedState)
        v("onCreateView")
        val view = inflateView(R.layout.post_reply, aContainer, aInflater)
        awfulActivity!!.setPreferredFont(view)
        return view
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        v("onActivityCreated")
        val activity: Activity? = getActivity()

        messageComposer =
            getChildFragmentManager().findFragmentById(R.id.message_composer_fragment) as MessageComposer?
        messageComposer!!.setBackgroundColor(ColorProvider.BACKGROUND.color)
        messageComposer!!.setTextColor(ColorProvider.PRIMARY_TEXT.color)

        // grab all the important reply params
        val intent = requireActivity().intent
        mReplyType = intent.getIntExtra(Constants.EDITING, -999)
        mPostId = intent.getIntExtra(Constants.REPLY_POST_ID, 0)
        mThreadId = intent.getIntExtra(Constants.REPLY_THREAD_ID, 0)
        setActionBarTitle(getTitle())

        // perform some sanity checking
        var badRequest = false
        if (mReplyType < 0 || mThreadId == 0) {
            // we always need a valid type and thread ID
            badRequest = true
        } else if (mPostId == 0 &&
            (mReplyType == AwfulMessage.TYPE_EDIT || mReplyType == AwfulMessage.TYPE_QUOTE)
        ) {
            // edits and quotes always need a post ID too
            badRequest = true
        }
        if (badRequest) {
            Toast.makeText(activity, "Can't create reply! Bad parameters", Toast.LENGTH_LONG).show()
            val template =
                "Failed to init reply activity%nReply type: %d, Thread ID: %d, Post ID: %d"
            w(template, mReplyType, mThreadId, mPostId)
            requireActivity().finish()
        }

        mContentResolver = requireActivity().contentResolver
        // load any related stored draft before starting the reply request
        // TODO: 06/04/2017 probably better to handle this as two separate, completable requests - combine reply and draft data when they're both finished, instead of assuming the draft loader finishes first
        this.storedDraft
        refreshThreadInfo()
        loadReply(mReplyType, mThreadId, mPostId)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ADD_ATTACHMENT) {
                val permissionCheck = ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    this.attachmentData = data
                    if (AwfulUtils.isTiramisu33) {
                        requestPermissions(
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                            Constants.AWFUL_PERMISSION_READ_MEDIA_IMAGES
                        )
                    } else {
                        requestPermissions(
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE
                        )
                    }
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE, Constants.AWFUL_PERMISSION_READ_MEDIA_IMAGES ->                 // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addAttachment()
                } else {
                    Toast.makeText(
                        activity,
                        R.string.no_file_permission_attachment,
                        Toast.LENGTH_LONG
                    ).show()
                }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    private val storedDraft: Unit
        get() {
            restartLoader(
                Constants.REPLY_LOADER_ID,
                null,
                draftLoaderCallback
            )
        }

    private fun refreshThreadInfo() {
        restartLoader(Constants.MISC_LOADER_ID, null, threadInfoCallback)
    }


    ////////////////////////////////////////////////////////////////////////
    // Fetching data/drafts and populating editor
    ////////////////////////////////////////////////////////////////////////
    /**
    * Initiate a new reply/edit by passing a request to the site and handling its response.
    *
    * @param mReplyType The type of request we're making (reply/quote/edit)
    * @param mThreadId The ID of the thread
    * @param mPostId The ID of the post being edited/quoted, if applicable
    */
    private fun loadReply(mReplyType: Int, mThreadId: Int, mPostId: Int) {
        progressDialog = ProgressDialog.show(activity, "Loading", "Fetching Message...", true, true)
        awfulActivity!!.setPreferredFont(progressDialog!!.findViewById(android.R.id.title))
        // create a callback to handle the reply data from the site
        val loadCallback: AwfulResultCallback<ContentValues> =
            object : AwfulResultCallback<ContentValues> {
                override fun success(result: ContentValues) {
                    replyData = result
                    if (result.containsKey(AwfulMessage.REPLY_CONTENT)) {
                        // update the message composer with the provided reply data
                        var replyData =
                            NetworkUtils.unencodeHtml(result.getAsString(AwfulMessage.REPLY_CONTENT))
                        if (!TextUtils.isEmpty(replyData)) {
                            if (replyData.endsWith("[/quote]")) {
                                replyData += "\n\n"
                            }
                            messageComposer!!.setText(replyData, true)
                        } else {
                            messageComposer!!.setText(null, false)
                        }
                    }
                    if (result.containsKey(AwfulMessage.REPLY_ATTACHMENT)) {
                        mFileAttachment = result.getAsString(AwfulMessage.REPLY_ATTACHMENT)
                    }
                    // set any options and update the menu
                    postSignature = getCheckedAndRemove(AwfulMessage.REPLY_SIGNATURE, result)
                    disableEmotes = getCheckedAndRemove(AwfulMessage.REPLY_DISABLE_SMILIES, result)
                    invalidateOptionsMenu()
                    dismissProgressDialog()
                    handleDraft()
                }

                override fun failure(error: VolleyError?) {
                    dismissProgressDialog()
                    //allow time for the error to display, then close the window
                    handler.postDelayed({ leave(RESULT_CANCELLED) }, 3000)
                }
            }
        when (mReplyType) {
            AwfulMessage.TYPE_NEW_REPLY -> queueRequest(
                ReplyRequest(
                    requireActivity(),
                    mThreadId
                ).build(this, loadCallback)
            )

            AwfulMessage.TYPE_QUOTE -> queueRequest(
                QuoteRequest(
                    requireActivity(),
                    mThreadId,
                    mPostId
                ).build(this, loadCallback)
            )

            AwfulMessage.TYPE_EDIT -> queueRequest(
                EditRequest(
                    requireActivity(),
                    mThreadId,
                    mPostId
                ).build(this, loadCallback)
            )

            else -> {
                // TODO: 13/04/2017 make an enum/intdef for reply types and just fail early if necessary, shouldn't need to keep checking for bad values everywhere
                Toast.makeText(
                    activity,
                    "Unknown reply type: $mReplyType",
                    Toast.LENGTH_LONG
                ).show()
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
        if (savedDraft == null) {
            return
        }
        /*
           This is where we decide whether to load an existing draft, or ignore it.
           The saved draft will end up getting replaced/deleted anyway (when the post is either posted or saved),
           this just decides whether it's relevant to the current context, and the user needs to know about it.

           We basically ignore a draft if:
           - we're currently editing a post, and the draft isn't an edit
           - the draft is an edit, but not for this post
           in both cases we need to avoid replacing the original post (that we're trying to edit) with some other post's draft
        */
        // TODO: 11/04/2017 might be better to treat edits and posts/quotes as two separate things, so you can have 1 post and 1 edit saved per thread without them deleting each other
        if ((savedDraft?.type == AwfulMessage.TYPE_EDIT && savedDraft?.postId != mPostId)
            || savedDraft?.type != AwfulMessage.TYPE_EDIT && mReplyType == AwfulMessage.TYPE_EDIT
        ) {
            return
        }
        // got a useful draft, let the user decide what to do with it
        displayDraftAlert(savedDraft ?: return)
    }


    /**
     * Display a dialog allowing the user to use or discard an existing draft.
     * 
     * @param draft a draft message relevant to this post
     */
    private fun displayDraftAlert(draft: SavedDraft) {
        val activity: FragmentActivity = activity ?: return

        val template = "You have a %s:" +
                "<br/><br/>" +
                "<i>%s</i>" +
                "<br/><br/>" +
                "Saved %s ago"
        val type = when (draft.type) {
            AwfulMessage.TYPE_EDIT -> "Saved Edit"
            AwfulMessage.TYPE_QUOTE -> "Saved Quote"
            AwfulMessage.TYPE_NEW_REPLY -> "Saved Reply"
            else -> "Saved Reply"
        }

        val MAX_PREVIEW_LENGTH = 140
        var previewText = StringUtils.substring(draft.content, 0, MAX_PREVIEW_LENGTH)
            .replace("\\n".toRegex(), "<br/>")
        if (draft.content.length > MAX_PREVIEW_LENGTH) {
            previewText += "..."
        }

        val message = String.format(
            template,
            type.lowercase(Locale.getDefault()),
            previewText,
            epochToSimpleDuration(draft.timestamp)
        )
        val positiveLabel = if (mReplyType == AwfulMessage.TYPE_QUOTE) "Add" else "Use"
        val use = AlertDialog.Builder(activity)
            .setIcon(R.drawable.ic_reply_dark)
            .setTitle(type)
            .setMessage(Html.fromHtml(message))
            .setPositiveButton(
                positiveLabel
            ) { dialog: DialogInterface?, which: Int ->
                var newContent = draft.content
                // If we're quoting something, stick it after the draft reply (and add some whitespace too)
                if (mReplyType == AwfulMessage.TYPE_QUOTE) {
                    newContent += "\n\n" + messageComposer!!.text
                }
                messageComposer!!.setText(newContent, true)
            }
            .setNegativeButton(
                R.string.discard
            ) { dialog: DialogInterface?, which: Int -> deleteSavedReply() } // avoid accidental draft losses by forcing a decision
            .setCancelable(false)
            .show()

        awfulActivity!!.setPreferredFont(use.findViewById(androidx.appcompat.R.id.alertTitle))
        awfulActivity!!.setPreferredFont(use.findViewById(android.R.id.message))
        awfulActivity!!.setPreferredFont(use.findViewById(android.R.id.button1))
        awfulActivity!!.setPreferredFont(use.findViewById(android.R.id.button2))
    }


    /////////////////////////////////////////////////////////////////////////
    // Send/preview posts
    /////////////////////////////////////////////////////////////////////////
    /**
    * Display a dialog allowing the user to submit or preview their post
    */
    private fun showSubmitDialog() {
        val submit = AlertDialog.Builder(requireActivity())
            .setTitle(
                String.format(
                    "Confirm %s?",
                    if (mReplyType == AwfulMessage.TYPE_EDIT) "Edit" else "Post"
                )
            )
            .setPositiveButton(
                R.string.submit
            ) { dialog: DialogInterface?, button: Int ->
                if (progressDialog == null && activity != null) {
                    progressDialog = ProgressDialog.show(
                        activity,
                        "Posting",
                        "Hopefully it didn't suck...",
                        true,
                        true
                    )
                    awfulActivity!!.setPreferredFont(
                        progressDialog!!.findViewById(
                            android.R.id.title
                        )
                    )
                }
                saveReply()
                submitPost()
            }
            .setNeutralButton(
                R.string.preview
            ) { dialog: DialogInterface?, button: Int -> previewPost() }
            .setNegativeButton(
                R.string.cancel
            ) { dialog: DialogInterface?, button: Int -> }
            .show()

        awfulActivity!!.setPreferredFont(submit.findViewById(androidx.appcompat.R.id.alertTitle))
        awfulActivity!!.setPreferredFont(submit.findViewById(android.R.id.message))
        awfulActivity!!.setPreferredFont(submit.findViewById(android.R.id.button1))
        awfulActivity!!.setPreferredFont(submit.findViewById(android.R.id.button2))
        awfulActivity!!.setPreferredFont(submit.findViewById(android.R.id.button3))
    }


    /**
     * Actually submit the post/edit to the site.
     */
    private fun submitPost() {
        val cv = prepareCV() ?: return
        val postCallback: AwfulResultCallback<Void?> = object : AwfulResultCallback<Void?> {
            override fun success(result: Void?) {
                dismissProgressDialog()
                deleteSavedReply()
                saveRequired = false

                val context = getContext()
                if (context != null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.post_sent),
                        Toast.LENGTH_LONG
                    ).show()
                }
                mContentResolver!!.notifyChange(AwfulThread.CONTENT_URI, null)
                leave(if (mReplyType == AwfulMessage.TYPE_EDIT) mPostId else RESULT_POSTED)
            }

            override fun failure(error: VolleyError?) {
                dismissProgressDialog()
                saveReply()
            }
        }
        when (mReplyType) {
            AwfulMessage.TYPE_QUOTE, AwfulMessage.TYPE_NEW_REPLY -> queueRequest(
                SendPostRequest(
                    requireActivity(),
                    cv
                ).build(this, postCallback)
            )

            AwfulMessage.TYPE_EDIT -> queueRequest(
                SendEditRequest(requireActivity(), cv).build(
                    this,
                    postCallback
                )
            )
        }
    }


    /**
     * Request a preview of the current post from the site, and display it.
     */
    private fun previewPost() {
        val cv = prepareCV()
        val activity: Activity? = getActivity()
        val fragmentManager = getFragmentManager()
        if (cv == null || activity == null || fragmentManager == null) {
            return
        }

        val previewFrag = PreviewFragment()
        previewFrag.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
        previewFrag.show(fragmentManager, "Post Preview")

        val previewCallback: AwfulResultCallback<String> = object : AwfulResultCallback<String> {
            override fun success(result: String) {
                previewFrag.setContent(result)
            }

            override fun failure(error: VolleyError?) {
                // love dialogs and callbacks very elegant
                if (!previewFrag.isStateSaved() && previewFrag.activity != null && !previewFrag.requireActivity()
                        .isFinishing
                ) {
                    previewFrag.dismiss()
                }
                if (view != null) {
                    Snackbar.make(view!!, "Preview failed.", Snackbar.LENGTH_LONG)
                        .setAction("Retry") { v: View? -> previewPost() }
                        .show()
                }
            }
        }

        if (mReplyType == AwfulMessage.TYPE_EDIT) {
            queueRequest(PreviewEditRequest(requireActivity(), cv).build(this, previewCallback))
        } else {
            queueRequest(PreviewPostRequest(requireActivity(), cv).build(this, previewCallback))
        }
    }


    /**
     * Create a ContentValues representing the current post and its options.
     * 
     * 
     * Returns null if the data is invalid, e.g. an empty post
     * 
     * @return The post data, or null if there was an error.
     */
    private fun prepareCV(): ContentValues? {
        if (replyData == null || replyData!!.getAsInteger(AwfulMessage.ID) == null) {
            // TODO: if this ever happens, the ID never gets set (and causes an NPE in SendPostRequest) - handle this in a better way?
            // Could use the mThreadId value, but that might be incorrect at this point and post to the wrong thread? Is null reply data an exceptional event?
            Log.e(Companion.TAG, "No reply data in sendPost() - no thread ID to post to!")
            val activity: Activity? = getActivity()
            if (activity != null) {
                Toast.makeText(activity, "Unknown thread ID - can't post!", Toast.LENGTH_LONG)
                    .show()
            }
            return null
        }
        val cv = ContentValues(replyData)
        if (this.isReplyEmpty) {
            dismissProgressDialog()
            alertView.setTitle(R.string.message_empty)
                .setSubtitle(R.string.message_empty_subtext)
                .show()
            return null
        }
        if (!TextUtils.isEmpty(mFileAttachment)) {
            cv.put(AwfulMessage.REPLY_ATTACHMENT, mFileAttachment)
        } else if (mReplyType == AwfulMessage.TYPE_EDIT && removeAttachment) {
            cv.put(AwfulMessage.REPLY_ATTACHMENT_ACTION, Constants.DELETE)
        }
        if (postSignature) {
            cv.put(AwfulMessage.REPLY_SIGNATURE, Constants.YES)
        }
        if (disableEmotes) {
            cv.put(AwfulMessage.REPLY_DISABLE_SMILIES, Constants.YES)
        }
        cv.put(AwfulMessage.REPLY_CONTENT, messageComposer!!.text)
        return cv
    }


    /**//////////////////////////////////////////////////////////////////////// */ // Lifecycle/navigation stuff
    /**//////////////////////////////////////////////////////////////////////// */
    override fun onResume() {
        super.onResume()
        updateThreadTitle()
        v("onResume")
    }

    override fun onPause() {
        super.onPause()
        v("onPause")
        cleanupTasks()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.e(Companion.TAG, "onDestroyView")
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
        messageComposer?.hideKeyboard()
    }


    /**
     * Finish the reply activity, performing cleanup and returning a result code to the activity that created it.
     */
    private fun leave(activityResult: Int) {
        val activity = awfulActivity
        if (activity != null) {
            activity.setResult(activityResult)
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(requireView().applicationWindowToken, 0)
            }
            activity.finish()
        }
    }


    /**
     * Call this when the user tries to leave the activity, so the Save/Discard dialog can be shown if necessary.
     */
    fun onNavigateBack() {
        val activity: Activity? = getActivity()
        if (activity == null) {
            return
        } else if (this.isReplyEmpty) {
            leave(RESULT_CANCELLED)
            return
        }
        val save = AlertDialog.Builder(activity)
            .setIcon(R.drawable.ic_reply_dark)
            .setMessage(
                String.format(
                    "Save this %s?",
                    if (mReplyType == AwfulMessage.TYPE_EDIT) "edit" else "post"
                )
            )
            .setPositiveButton(
                R.string.save
            ) { dialog: DialogInterface?, button: Int ->
                // let #autoSave handle it on leaving
                saveRequired = true
                leave(RESULT_CANCELLED)
            }
            .setNegativeButton(
                R.string.discard
            ) { dialog: DialogInterface?, which: Int ->
                deleteSavedReply()
                saveRequired = false
                leave(RESULT_CANCELLED)
            }
            .setNeutralButton(
                R.string.cancel
            ) { dialog: DialogInterface?, which: Int -> }
            .setCancelable(true)
            .show()

        awfulActivity!!.setPreferredFont(save.findViewById(androidx.appcompat.R.id.alertTitle))
        awfulActivity!!.setPreferredFont(save.findViewById(android.R.id.message))
        awfulActivity!!.setPreferredFont(save.findViewById(android.R.id.button1))
        awfulActivity!!.setPreferredFont(save.findViewById(android.R.id.button2))
        awfulActivity!!.setPreferredFont(save.findViewById(android.R.id.button3))
    }


    /////////////////////////////////////////////////////////////////////////
    // Saving draft data
    /////////////////////////////////////////////////////////////////////////
    /**
    * Trigger a draft save, if required.
    */
    private fun autoSave() {
        if (saveRequired && messageComposer != null) {
            if (this.isReplyEmpty) {
                Log.i(Companion.TAG, "Message unchanged, discarding.")
                // TODO: 12/02/2017 does this actually need to check if it's unchanged?
                deleteSavedReply() //if the reply is unchanged, throw it out.
                messageComposer!!.setText(null, false)
            } else {
                Log.i(Companion.TAG, "Message Unsent, saving.")
                saveReply()
            }
        }
    }


    /**
     * Delete any saved reply for the current thread
     */
    private fun deleteSavedReply() {
        mContentResolver!!.delete(
            AwfulMessage.CONTENT_URI_REPLY,
            AwfulMessage.ID + "=?",
            AwfulProvider.int2StrArray(mThreadId)
        )
    }


    /**
     * Save a draft reply for the current thread.
     */
    private fun saveReply() {
        if (activity != null && mThreadId > 0 && messageComposer != null) {
            val content = messageComposer!!.text
            // don't save if the message is empty/whitespace
            // not trimming the actual content, so we retain any whitespace e.g. blank lines after quotes
            if (content.trim { it <= ' ' }.isNotEmpty()) {
                Log.i(Companion.TAG, "Saving reply! $content")
                val post = if (replyData == null) ContentValues() else ContentValues(replyData)
                post.put(AwfulMessage.ID, mThreadId)
                post.put(AwfulMessage.TYPE, mReplyType)
                post.put(AwfulMessage.REPLY_CONTENT, content)
                post.put(AwfulMessage.EPOC_TIMESTAMP, System.currentTimeMillis())
                if (mFileAttachment != null) {
                    post.put(AwfulMessage.REPLY_ATTACHMENT, mFileAttachment)
                }
                if (mContentResolver!!.update(
                        ContentUris.withAppendedId(
                            AwfulMessage.CONTENT_URI_REPLY,
                            mThreadId.toLong()
                        ), post, null, null
                    ) < 1
                ) {
                    mContentResolver!!.insert(AwfulMessage.CONTENT_URI_REPLY, post)
                }
            }
        }
    }


    /**//////////////////////////////////////////////////////////////////////// */ // Menus
    /**//////////////////////////////////////////////////////////////////////// */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        v("onCreateOptionsMenu")
        inflater.inflate(R.menu.post_reply, menu)

        val attach = menu.findItem(R.id.add_attachment)
        if (attach != null) {
            attach.isEnabled = prefs.hasPlatinum && mReplyType != AwfulMessage.TYPE_EDIT
            attach.isVisible = prefs.hasPlatinum && mReplyType != AwfulMessage.TYPE_EDIT
        }
        val remove = menu.findItem(R.id.remove_attachment)
        if (remove != null && this.mFileAttachment != null) {
            remove.isEnabled = prefs.hasPlatinum
            remove.isVisible = prefs.hasPlatinum
            val filepath: Array<String?> =
                this.mFileAttachment!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val filename = filepath[filepath.size - 1]
            remove.title = "Remove $filename"
        }
        val disableEmoticons = menu.findItem(R.id.disableEmots)
        if (disableEmoticons != null) {
            disableEmoticons.isChecked = disableEmotes
        }
        val sig = menu.findItem(R.id.signature)
        if (sig != null) {
            sig.isChecked = postSignature
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        v("onOptionsItemSelected")
        when (item.itemId) {
            R.id.submit_button -> showSubmitDialog()
            R.id.add_attachment -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(
                    Intent.createChooser(
                        intent,
                        "Select Picture"
                    ), ADD_ATTACHMENT
                )
            }

            R.id.remove_attachment -> {
                this.mFileAttachment = null
                if (mReplyType == AwfulMessage.TYPE_EDIT) {
                    removeAttachment = true
                }
                val removeToast = Toast.makeText(
                    awfulActivity,
                    awfulActivity!!.resources.getText(R.string.file_removed),
                    Toast.LENGTH_SHORT
                )
                removeToast.show()
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


    override fun onPreferenceChange(preferences: AwfulPreferences, key: String?) {
        super.onPreferenceChange(preferences, key)
        //refresh the menu to show/hide attach option (plat only)
        invalidateOptionsMenu()
    }


    /////////////////////////////////////////////////////////////////////////
    // Attachment handling
    /////////////////////////////////////////////////////////////////////////

    // TODO: 13/04/2017 make a separate attachment component and stick all this in there
    private fun addAttachment() {
        addAttachment(attachmentData!!)
        attachmentData = null
    }

    private fun addAttachment(data: Intent) {
        val selectedImageUri = data.data
        val path = getFilePath(selectedImageUri!!)
        if (path == null) {
            setAttachment(null, getString(R.string.file_error))
            return
        }

        val attachment = File(path)
        val filename = attachment.name
        if (!attachment.isFile || !attachment.canRead()) {
            setAttachment(null, String.format(getString(R.string.file_unreadable), filename))
            return
        } else if (!StringUtils.endsWithAny(
                filename.lowercase(Locale.getDefault()),
                ".jpg",
                ".jpeg",
                ".png",
                ".gif"
            )
        ) {
            setAttachment(null, String.format(getString(R.string.file_wrong_filetype), filename))
            return
        } else if (attachment.length() > Constants.ATTACHMENT_MAX_BYTES) {
            setAttachment(null, String.format(getString(R.string.file_too_big), filename))
            return
        }

        // check the image size without creating a bitmap
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val height = options.outHeight
        val width = options.outWidth
        if (width > Constants.ATTACHMENT_MAX_WIDTH || height > Constants.ATTACHMENT_MAX_HEIGHT) {
            setAttachment(
                null,
                String.format(getString(R.string.file_resolution_too_big), filename, width, height)
            )
            return
        }

        setAttachment(path, String.format(getString(R.string.file_attached), filename))
    }


    private fun setAttachment(attachment: String?, toastMessage: String) {
        mFileAttachment = attachment
        Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show()
        invalidateOptionsMenu()
    }


    private fun getFilePath(uri: Uri): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(this.activity, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split: Array<String?> =
                    docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    "content://downloads/public_downloads".toUri(), id.toLong()
                )

                return getDataColumn(contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split: Array<String?> =
                    docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                when (type) {
                    "image" -> {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    "video" -> {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                    "audio" -> {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )

                return getDataColumn(contentUri!!, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address

            if (isGooglePhotosUri(uri)) return uri.lastPathSegment

            return getDataColumn(uri, null, null)
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
    private fun getDataColumn(
        uri: Uri, selection: String?,
        selectionArgs: Array<String?>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf<String?>(
            column
        )

        try {
            cursor = this.requireActivity().contentResolver.query(
                uri, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }


    /////////////////////////////////////////////////////////////////////////
    // Misc utility stuff
    /////////////////////////////////////////////////////////////////////////
    /**
    * Utility method to check if the composer contains an empty post
    */
    private val isReplyEmpty: Boolean
        get() = messageComposer!!.text.trim { it <= ' ' }.isEmpty()

    /**
     * Convert an epoch timestamp to a duration relative to now.
     * 
     * 
     * Returns the duration in a "1d 4h 22m 30s" format, omitting units with zero values.
     */
    private fun epochToSimpleDuration(epoch: Long): String {
        var diff = Duration.between(Instant.ofEpochSecond((epoch / 1000)), Instant.now()).abs()
        var time = ""
        if (diff.toDays() > 0) {
            time += " " + diff.toDays() + "d"
            diff = diff.minusDays(diff.toDays())
        }
        if (diff.toHours() > 0) {
            time += " " + diff.toHours() + "h"
            diff = diff.minusHours(diff.toHours())
        }
        if (diff.toMinutes() > 0) {
            time += " " + diff.toMinutes() + "m"
            diff = diff.minusMinutes(diff.toMinutes())
        }

        time += " " + diff.seconds + "s"
        return time
    }


    //////////////////////////////////////////////////////////////////////////
    // UI things
    //////////////////////////////////////////////////////////////////////////
    /**
    * Dismiss the progress dialog and set it to null, if it isn't already.
    */
    private fun dismissProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }


    /**
     * Update the title view to show the current thread title, if we have it
     */
    private fun updateThreadTitle() {
        val threadTitleView = requireActivity().findViewById<TextView?>(R.id.thread_title)
        if (threadTitleView != null) {
            threadTitleView.text = if (mThreadTitle == null) "" else mThreadTitle
        }
        awfulActivity!!.setPreferredFont(threadTitleView)
    }

    override fun getTitle(): String {
        return when (mReplyType) {
            AwfulMessage.TYPE_EDIT -> "Editing"
            AwfulMessage.TYPE_QUOTE -> "Quote"
            AwfulMessage.TYPE_NEW_REPLY -> "Reply"
            else -> "Reply"
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Async classes etc
    ///////////////////////////////////////////////////////////////////////////
    /**
    * Provides a Loader that pulls draft data for the current thread from the DB.
    */
    private inner class DraftReplyLoaderCallback : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor?> {
            Log.i(Companion.TAG, "Create Reply Cursor: $mThreadId")
            return CursorLoader(
                activity!!,
                ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mThreadId.toLong()),
                AwfulProvider.DraftPostProjection,
                null,
                null,
                null
            )
        }

        override fun onLoadFinished(aLoader: Loader<Cursor?>, aData: Cursor) {
            if (aData.isClosed || !aData.moveToFirst()) {
                // no draft saved for this thread
                return
            }
            // if there's some quote data, deserialise it into a SavedDraft
            val quoteData = aData.getString(aData.getColumnIndexOrThrow(AwfulMessage.REPLY_CONTENT))
            if (TextUtils.isEmpty(quoteData)) {
                return
            }
            val draftType = aData.getInt(aData.getColumnIndexOrThrow(AwfulMessage.TYPE))
            val postId = aData.getInt(aData.getColumnIndexOrThrow(AwfulPost.EDIT_POST_ID))
            val draftTimestamp = aData.getLong(aData.getColumnIndexOrThrow(AwfulMessage.EPOC_TIMESTAMP))
            val draftReply = NetworkUtils.unencodeHtml(quoteData)

            savedDraft = SavedDraft(draftType, draftReply, postId, draftTimestamp)
            if (Constants.DEBUG) {
                Log.i(Companion.TAG, draftType.toString() + "Saved reply message: " + draftReply)
            }
        }

        override fun onLoaderReset(aLoader: Loader<Cursor?>) {
        }
    }


    /**
     * Provides a Loader that gets metadata for the current thread, and dsiplays its title
     */
    private inner class ThreadInfoCallback : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor> {
            return CursorLoader(
                requireActivity(),
                ContentUris.withAppendedId(AwfulThread.CONTENT_URI, mThreadId.toLong()),
                AwfulProvider.ThreadProjection,
                null,
                null,
                null
            )
        }

        override fun onLoadFinished(aLoader: Loader<Cursor?>, aData: Cursor) {
            Log.v(Companion.TAG, "Thread title finished, populating.")
            if (aData.moveToFirst()) {
                //threadClosed = aData.getInt(aData.getColumnIndex(AwfulThread.LOCKED))>0;
                mThreadTitle = aData.getString(aData.getColumnIndexOrThrow(AwfulThread.TITLE))
                updateThreadTitle()
            }
        }

        override fun onLoaderReset(aLoader: Loader<Cursor?>) {
        }
    }


    private class SavedDraft(
        var type: Int,
        var content: String,
        var postId: Int,
        var timestamp: Long
    )

    companion object {
        const val REQUEST_POST: Int = 5
        const val RESULT_POSTED: Int = 6
        const val RESULT_CANCELLED: Int = 7
        const val RESULT_EDITED: Int = 8
        const val ADD_ATTACHMENT: Int = 9
        private const val TAG = "PostReplyFragment"
    }
}
