package com.ferg.awfulapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.DialogInterface
import android.database.ContentObserver
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Messenger
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.android.volley.VolleyError
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.reply.MessageComposer
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.PMReplyRequest
import com.ferg.awfulapp.task.PMRequest
import com.ferg.awfulapp.task.SendPrivateMessageRequest
import com.ferg.awfulapp.thread.AwfulHtmlPage
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.webview.AwfulWebView
import com.ferg.awfulapp.webview.WebViewJsInterface
import com.ferg.awfulapp.widget.ThreadIconPicker
import timber.log.Timber.Forest.i
import androidx.core.view.isVisible

class MessageFragment : AwfulFragment, View.OnClickListener {
    companion object {
        private const val TAG = "MessageFragment"

        fun newInstance(aUser: String?, aId: Int): MessageFragment {
            return MessageFragment(aUser, aId)
        }
    }
    private var pmId = -1
    private var recipient: String? = null

    private var messageWebView: AwfulWebView? = null
    private var messageComposer: MessageComposer? = null
    private var mHideButton: ImageButton? = null
    private var mUsername: TextView? = null
    private var mPostdate: TextView? = null
    private var mTitle: TextView? = null
    private var mRecipient: EditText? = null
    private var mSubject: EditText? = null
    private var mBackground: View? = null
    private var threadIconPicker: ThreadIconPicker? = null

    private lateinit var mPrefs: AwfulPreferences

    private var mDialog: ProgressDialog? = null

    private val mMessenger = Messenger(handler)
    private val mPMDataCallback = PMCallback(handler)
    private val pmReplyObserver: ContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            Log.i(Companion.TAG, "PM Data update.")
            restartLoader(pmId, null, mPMDataCallback)
        }
    }

    internal interface PrivateMessageCallbacks {
        fun onMessageClosed()
    }

    constructor()

    /**
     * Creates a new Message Display/Reply fragment.
     * @param user User ID to send message to. Optional: will not be used if replying to message.
     * @param id PM ID number to reply to. Will fetch message data from service automatically. Set to 0 for a blank message.
     */
    constructor(user: String?, id: Int) {
        pmId = id
        recipient = user
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        aInflater: LayoutInflater,
        aContainer: ViewGroup?,
        aSavedState: Bundle?
    ): View {
        super.onCreateView(aInflater, aContainer, aSavedState)
        mPrefs = AwfulPreferences.getInstance(requireActivity())

        setRetainInstance(true)

        val result = aInflater.inflate(R.layout.private_message_fragment, aContainer, false)

        messageWebView = result.findViewById(R.id.messagebody)
        mHideButton = result.findViewById(R.id.hide_message)
        mHideButton?.setOnClickListener(this)
        mRecipient = result.findViewById(R.id.message_user)
        mSubject = result.findViewById(R.id.message_subject)
        mUsername = result.findViewById(R.id.username)
        mPostdate = result.findViewById(R.id.post_date)
        mTitle = result.findViewById(R.id.message_title)

        messageComposer =
            getChildFragmentManager().findFragmentById(R.id.message_composer_fragment) as MessageComposer?
        threadIconPicker =
            getChildFragmentManager().findFragmentById(R.id.thread_icon_picker) as ThreadIconPicker?
        threadIconPicker?.usePrivateMessageIcons()

        mBackground = result
        updateColors(result, mPrefs)
        messageWebView?.setJavascriptHandler(WebViewJsInterface())
        messageWebView?.setContent(AwfulHtmlPage.getContainerHtml(mPrefs, null, false))

        if (pmId <= 0) {
            messageWebView?.visibility = View.GONE
        } else {
            syncPM()
        }

        awfulActivity?.setPreferredFont(result)

        ViewCompat.setOnApplyWindowInsetsListener(
            result.rootView
        ) { _: View?, insets: WindowInsetsCompat ->
            val innerPadding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            ViewCompat.setPaddingRelative(
                result.rootView,
                innerPadding.left,
                0,
                innerPadding.right,
                if (imeVisible) imeHeight else innerPadding.bottom
            )
            insets
        }

        return result
    }

    private fun updateColors(v: View?, prefs: AwfulPreferences?) {
        val color = ColorProvider.PRIMARY_TEXT.color
        messageComposer?.setTextColor(color)
        mRecipient?.setTextColor(color)
        mSubject?.setTextColor(color)
        mUsername?.setTextColor(color)
        mPostdate?.setTextColor(color)
        mTitle?.setTextColor(color)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.private_message_writing, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                closeMessage()
                return true
            }

            R.id.send_pm -> {
                showSubmitDialog()
                return true
            }

            R.id.new_pm -> {
                newMessage()
                return true
            }

            R.id.settings -> {
                awfulActivity?.navigate(NavigationEvent.Settings())
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        restartLoader(pmId, null, mPMDataCallback)
        requireActivity().contentResolver
            .registerContentObserver(AwfulMessage.CONTENT_URI, true, mPMDataCallback)
        requireActivity().contentResolver
            .registerContentObserver(AwfulMessage.CONTENT_URI_REPLY, true, pmReplyObserver)
    }


    private fun syncPM() {
        // TODO: rework this so we don't hold onto the activity - AwfulRequest wants to make a Toast so we can't just pass in the app context
        val activity: Activity? = getActivity()
        if (activity == null) {
            i("Activity unavailable - abandoning PM load")
            return
        }
        queueRequest(PMRequest(activity, pmId).build(this, object : AwfulResultCallback<Void?> {
            override fun success(result: Void?) {
                restartLoader(pmId, null, mPMDataCallback)
                queueRequest(
                    PMReplyRequest(activity, pmId).build(
                        this@MessageFragment,
                        object : AwfulResultCallback<Void?> {
                            override fun success(result: Void?) {
                                restartLoader(pmId, null, mPMDataCallback)
                            }

                            override fun failure(error: VolleyError?) {
                                //error is automatically displayed
                            }
                        })
                )
            }

            override fun failure(error: VolleyError?) {
                //error is automatically displayed
            }
        }))
    }


    /**
     * Display a dialog allowing the user to send their message
     */
    private fun showSubmitDialog() {
        AlertDialog.Builder(requireActivity())
            .setTitle("Send message?")
            .setPositiveButton(
                R.string.submit
            ) { _: DialogInterface?, _: Int ->
                if (mDialog == null && activity != null) {
                    mDialog = ProgressDialog.show(
                        activity,
                        "Sending",
                        "Hopefully it didn't suck...",
                        true,
                        true
                    )
                    awfulActivity?.setPreferredFont(mDialog?.findViewById(android.R.id.title))
                }
                saveReply()
                sendPM()
            }
            .setNegativeButton(
                R.string.cancel
            ) { _: DialogInterface?, _: Int -> }
            .show()
    }


    fun sendPM() {
        queueRequest(
            SendPrivateMessageRequest(requireActivity(), pmId).build(this,object : AwfulResultCallback<Void?> {
                override fun success(result: Void?) {
                    mDialog?.dismiss()
                    mDialog = null
                    alertView.setTitle("Message Sent!").setIcon(R.drawable.ic_check_circle)
                        .show()
                    closeMessage()
                }

                override fun failure(error: VolleyError?) {
                    mDialog?.dismiss()
                    mDialog = null
                    alertView.setTitle("Failed to send!").setSubtitle("Draft Saved").show()
                }
            })
        )
    }


    /**
     * Close this message, letting the activity handle it
     */
    private fun closeMessage() {
        val activity = activity as PrivateMessageCallbacks?
        activity?.onMessageClosed()
    }


    fun saveReply() {
        val content = requireActivity().contentResolver
        val values = ContentValues()
        values.put(AwfulMessage.ID, pmId)
        values.put(AwfulMessage.TITLE, mSubject?.text.toString())
        values.put(AwfulMessage.TYPE, AwfulMessage.TYPE_PM)
        values.put(AwfulMessage.RECIPIENT, mRecipient?.text.toString())
        values.put(AwfulMessage.REPLY_CONTENT, messageComposer?.text)
        values.put(AwfulMessage.REPLY_ICON, threadIconPicker?.icon?.iconId)
        if (content.update(
                ContentUris.withAppendedId(
                    AwfulMessage.CONTENT_URI_REPLY,
                    pmId.toLong()
                ), values, null, null
            ) < 1
        ) {
            content.insert(AwfulMessage.CONTENT_URI_REPLY, values)
        }
    }

    override fun onResume() {
        super.onResume()
        messageWebView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (pmId > 0) {
            saveReply()
        }
        messageWebView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        loaderManager.destroyLoader(pmId)
        requireActivity().contentResolver.unregisterContentObserver(mPMDataCallback)
        requireActivity().contentResolver.unregisterContentObserver(pmReplyObserver)
    }

    override fun onDetach() {
        super.onDetach()
        mDialog?.dismiss()
        mDialog = null
    }

    private fun newMessage() {
        loaderManager.destroyLoader(pmId)
        pmId = -1 //TODO getNextId();
        recipient = null
        messageComposer?.setText(null, false)
        mUsername?.text = ""
        mRecipient?.setText("")
        mPostdate?.text = ""
        messageWebView?.setBodyHtml(null)
        mTitle?.text = getString(R.string.new_message_placeholder)
        mSubject?.setText("")
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.hide_message -> messageWebView?.visibility = if (messageWebView?.isVisible == true) View.GONE else View.VISIBLE
        }
    }

    override fun onPreferenceChange(preferences: AwfulPreferences, key: String?) {
        super.onPreferenceChange(preferences, key)
        if (view != null) {
            updateColors(view, preferences)
        } else {
            if (mBackground != null) {
                updateColors(mBackground, preferences)
            }
        }
    }


    private inner class PMCallback(handler: Handler?) : ContentObserver(handler),
        LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor?> {
            // TODO: 05/05/2017 if pmId is negative (i.e. an invalid number) the load will fail - try and avoid doing it?
            Log.i(Companion.TAG, "Create PM Cursor:$pmId")
            return CursorLoader(
                requireActivity(),
                ContentUris.withAppendedId(AwfulMessage.CONTENT_URI, pmId.toLong()),
                AwfulProvider.PMReplyProjection,
                null,
                null,
                null
            )
        }

        override fun onLoadFinished(aLoader: Loader<Cursor?>, aData: Cursor?) {
            //TODO retain info if entered into reply window
            // the Cursor will be null if pmId is negative
            if (aData != null && aData.moveToFirst()) {
                Log.v(Companion.TAG, "PM load finished, populating: " + aData.count)
                messageWebView?.setBodyHtml(null)
                val title = aData.getString(aData.getColumnIndexOrThrow(AwfulMessage.TITLE))
                mTitle?.text = title
                messageWebView?.setBodyHtml(
                    AwfulMessage.getMessageHtml(
                        aData.getString(
                            aData.getColumnIndexOrThrow(
                                AwfulMessage.CONTENT
                            )
                        )
                    )
                )
                mPostdate?.text = aData.getString(aData.getColumnIndexOrThrow(AwfulMessage.DATE))
                val replyTitle = aData.getString(aData.getColumnIndexOrThrow(AwfulMessage.REPLY_TITLE))
                val replyContent = aData.getString(aData.getColumnIndexOrThrow(AwfulMessage.REPLY_CONTENT))
                if (replyContent != null) {
                    if (replyContent != messageComposer?.text) {
                        messageComposer?.setText(replyContent, false)
                    }
                } else {
                    messageComposer?.setText(null, false)
                }
                if (replyTitle != null) {
                    if (replyTitle != mSubject?.text.toString()) {
                        mSubject?.setText(replyTitle)
                    }
                } else {
                    mSubject?.setText(title)
                }
                awfulActivity?.setPreferredFont(mSubject)
                val author = aData.getString(aData.getColumnIndexOrThrow(AwfulMessage.AUTHOR))
                mUsername?.text = getString(R.string.message_sender).format(author)
                val recip = aData.getString(aData.getColumnIndexOrThrow(AwfulMessage.RECIPIENT))
                if (recip != null) {
                    mRecipient?.setText(recip)
                } else {
                    mRecipient?.setText(author)
                }
            } else {
                if (recipient != null) {
                    mRecipient?.setText(recipient)
                }
            }
            awfulActivity?.setPreferredFont(mRecipient)
        }

        override fun onLoaderReset(aLoader: Loader<Cursor?>) {
        }

        override fun onChange(selfChange: Boolean) {
            Log.i(Companion.TAG, "PM Data update.")
            restartLoader(pmId, null, this)
        }
    }


    override fun getTitle(): String {
        return mTitle?.text.toString()
    }


    override fun doScroll(down: Boolean): Boolean {
        if (down) {
            messageWebView?.pageDown(false)
        } else {
            messageWebView?.pageUp(false)
        }
        return true
    }
}
