/**
 * *****************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * <p/>
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
 * <p/>
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

package com.ferg.awfulapp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.PreviewThreadRequest;
import com.ferg.awfulapp.task.SendThreadRequest;
import com.ferg.awfulapp.task.ThreadRequest;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.reply.MessageComposer;
import com.ferg.awfulapp.util.AwfulUtils;
import com.ferg.awfulapp.widget.ThreadIconPicker;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import timber.log.Timber;

import static com.ferg.awfulapp.constants.Constants.ATTACHMENT_MAX_BYTES;
import static com.ferg.awfulapp.constants.Constants.ATTACHMENT_MAX_HEIGHT;
import static com.ferg.awfulapp.constants.Constants.ATTACHMENT_MAX_WIDTH;
import static com.ferg.awfulapp.thread.AwfulMessage.REPLY_DISABLE_SMILIES;
import static com.ferg.awfulapp.thread.AwfulMessage.REPLY_SIGNATURE;

public class PostThreadFragment extends AwfulFragment {

    public static final int REQUEST_THREAD = 5;
    public static final int RESULT_POSTED = 6;
    public static final int RESULT_CANCELLED = 7;
    public static final int ADD_ATTACHMENT = 9;
    private static final String TAG = "PostThreadFragment";

    // UI components
    private MessageComposer messageComposer;
    @Nullable
    private ProgressDialog progressDialog;

    private ThreadIconPicker threadIconPicker;

    private EditText subject;

    // internal state
    @Nullable
    private SavedDraft savedDraft = null;
    @Nullable
    private ContentValues threadData = null;
    private boolean saveRequired = true;
    @Nullable
    private Intent attachmentData;

    // async stuff
    private ContentResolver mContentResolver;
    @NonNull
    private final DraftThreadLoaderCallback draftLoaderCallback = new DraftThreadLoaderCallback();
    @NonNull
    private final ForumInfoCallback forumInfoCallback = new ForumInfoCallback();

    // thread metadata
    private int mForumId;

    // User's thread data
    @Nullable
    private String mFileAttachment;
    private boolean disableEmotes = false;
    private boolean postSignature = false;


    ///////////////////////////////////////////////////////////////////////////
    // Activity and fragment initialisation
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.v("onCreate");
        setHasOptionsMenu(true);
        setRetainInstance(false);
    }


    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);
        Timber.v("onCreateView");
        View view = inflateView(R.layout.post_thread, aContainer, aInflater);
        getAwfulActivity().setPreferredFont(view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
        Timber.v("onActivityCreated");
        Activity activity = getActivity();

        messageComposer = (MessageComposer) getChildFragmentManager().findFragmentById(R.id.message_composer_fragment);
        messageComposer.setBackgroundColor(ColorProvider.BACKGROUND.getColor());
        messageComposer.setTextColor(ColorProvider.PRIMARY_TEXT.getColor());

        // grab all the important thread params
        Intent intent = activity.getIntent();
        mForumId = intent.getIntExtra(Constants.POST_FORUM_ID, 0);
        setActionBarTitle(getTitle());

        threadIconPicker = (ThreadIconPicker) getFragmentManager().findFragmentById(R.id.thread_icon_picker);
        threadIconPicker.useForumIcons(mForumId);

        subject = (EditText) activity.findViewById(R.id.thread_subject);

        // perform some sanity checking
        boolean badRequest = false;
        if (mForumId < 0 || mForumId == 0) {
            // we always need a valid forum ID
            badRequest = true;
        }
        if (badRequest) {
            Toast.makeText(activity, "Can't create thread! Bad parameters", Toast.LENGTH_LONG).show();
            String template = "Failed to init thread activity%n Forum ID: %d";
            Timber.w(template, mForumId);
            activity.finish();
        }

        mContentResolver = activity.getContentResolver();
        // load any related stored draft before starting the thread request
        // TODO: 06/04/2017 probably better to handle this as two separate, completable requests - combine thread and draft data when they're both finished, instead of assuming the draft loader finishes first
        getStoredDraft();
        refreshForumInfo();
        loadThread(mForumId);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ADD_ATTACHMENT) {
                if (AwfulUtils.isMarshmallow23()) {
                    int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        this.attachmentData = data;
                        if (AwfulUtils.isTiramisu33()) {
                            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, Constants.AWFUL_PERMISSION_READ_MEDIA_IMAGES);
                        } else {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE);
                        }
                        return;
                    }
                }
                addAttachment(data);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE:
            case Constants.AWFUL_PERMISSION_READ_MEDIA_IMAGES:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addAttachment();
                } else {
                    Toast.makeText(getActivity(), R.string.no_file_permission_attachment, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void getStoredDraft() {
        restartLoader(Constants.THREAD_DRAFT_LOADER_ID, null, draftLoaderCallback);
    }

    private void refreshForumInfo() {
        restartLoader(Constants.FORUM_LOADER_ID, null, forumInfoCallback);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Fetching data/drafts and populating editor
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Initiate a new thread by passing a request to the site and handling its response.
     *
     * @param mForumId  The ID of the forum
     */
    private void loadThread(int mForumId) {
        progressDialog = ProgressDialog.show(getActivity(), "Loading", "Fetching Message...", true, true);
        getAwfulActivity().setPreferredFont(progressDialog.findViewById(android.R.id.title));

        // create a callback to handle the thread data from the site
        AwfulRequest.AwfulResultCallback<ContentValues> loadCallback = new AwfulRequest.AwfulResultCallback<ContentValues>() {
            @Override
            public void success(ContentValues result) {
                threadData = result;
                // set any options and update the menu
                postSignature = getCheckedAndRemove(REPLY_SIGNATURE, result);
                disableEmotes = getCheckedAndRemove(REPLY_DISABLE_SMILIES, result);
                invalidateOptionsMenu();
                dismissProgressDialog();
                handleDraft();
            }

            @Override
            public void failure(VolleyError error) {
                dismissProgressDialog();
                //allow time for the error to display, then close the window
                getHandler().postDelayed(() -> leave(RESULT_CANCELLED), 3000);
            }
        };
        queueRequest(new ThreadRequest(getActivity(), mForumId).build(this, loadCallback));
    }

    /**
     * Removes a key from a ContentValues, returning true if it was set to "checked"
     */
    private boolean getCheckedAndRemove(@NonNull String key, @NonNull ContentValues values) {
        if (!values.containsKey(key)) {
            return false;
        }
        boolean checked = "checked".equals(values.getAsString(key));
        values.remove(key);
        return checked;
    }


    /**
     * Take care of any saved draft, allowing the user to use it if appropriate.
     */
    private void handleDraft() {
        // this implicitly relies on the Draft Thread Loader having already finished, assigning to savedDraft if it found any draft data
        if (savedDraft == null) {
            return;
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

        // got a useful draft, let the user decide what to do with it
        displayDraftAlert(savedDraft);
    }


    /**
     * Display a dialog allowing the user to use or discard an existing draft.
     *
     * @param draft a draft message relevant to this post
     */
    private void displayDraftAlert(@NonNull SavedDraft draft) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        String template = "You have a %s:" +
                "<br/><b>%s:</b><br/><br/>" +
                "<i>%s</i>" +
                "<br/><br/>" +
                "Saved %s ago";

        String type = "Saved Thread";

        final int MAX_PREVIEW_LENGTH = 140;
        String previewText = StringUtils.substring(draft.content, 0, MAX_PREVIEW_LENGTH).replaceAll("\\n", "<br/>");
        if (draft.content.length() > MAX_PREVIEW_LENGTH) {
            previewText += "...";
        }

        String message = String.format(template, type, draft.subject , previewText, epochToSimpleDuration(draft.timestamp));
        AlertDialog use = new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_reply_dark)
                .setTitle(type)
                .setMessage(Html.fromHtml(message))
                .setPositiveButton("Use", (dialog, which) -> {
                    String newContent = draft.content;
                    // If we're quoting something, stick it after the draft thread (and add some whitespace too)
                    messageComposer.setText(newContent, true);
                    subject.setText(draft.subject);
                    if(draft.iconId != null && draft.iconUrl != null && draft.iconUrl.length() > 0){
                        threadIconPicker.useIcon(draft.iconId, draft.iconUrl);
                    }
                })
                .setNegativeButton(R.string.discard, (dialog, which) -> deleteSavedThread())
                // avoid accidental draft losses by forcing a decision
                .setCancelable(false)
                .show();

        getAwfulActivity().setPreferredFont(use.findViewById(androidx.appcompat.R.id.alertTitle));
        getAwfulActivity().setPreferredFont(use.findViewById(android.R.id.message));
        getAwfulActivity().setPreferredFont(use.findViewById(android.R.id.button1));
        getAwfulActivity().setPreferredFont(use.findViewById(android.R.id.button2));
    }


    ///////////////////////////////////////////////////////////////////////////
    // Send/preview posts
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Display a dialog allowing the user to submit or preview their post
     */
    private void showSubmitDialog() {
        AlertDialog submit = new AlertDialog.Builder(getActivity())
                .setTitle("Confirm Post?")
                .setPositiveButton(R.string.submit,
                        (dialog, button) -> {
                            if (progressDialog == null && getActivity() != null) {
                                progressDialog = ProgressDialog.show(getActivity(), "Posting", "Hopefully it didn't suck...", true, true);
                                getAwfulActivity().setPreferredFont(progressDialog.findViewById(android.R.id.title));
                            }
                            saveThread();
                            submitThread();
                        })
                .setNeutralButton(R.string.preview, (dialog, button) -> previewPost())
                .setNegativeButton(R.string.cancel, (dialog, button) -> {
                }).show();

        getAwfulActivity().setPreferredFont(submit.findViewById(androidx.appcompat.R.id.alertTitle));
        getAwfulActivity().setPreferredFont(submit.findViewById(android.R.id.message));
        getAwfulActivity().setPreferredFont(submit.findViewById(android.R.id.button1));
        getAwfulActivity().setPreferredFont(submit.findViewById(android.R.id.button2));
        getAwfulActivity().setPreferredFont(submit.findViewById(android.R.id.button3));

    }


    /**
     * Actually submit the post/edit to the site.
     */
    private void submitThread() {
        ContentValues cv = prepareCV();
        if (cv == null) {
            return;
        }
        AwfulRequest.AwfulResultCallback<Void> postCallback = new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                dismissProgressDialog();
                deleteSavedThread();
                saveRequired = false;

                Context context = getContext();
                if (context != null) {
                    Toast.makeText(context, context.getString(R.string.post_sent), Toast.LENGTH_LONG).show();
                }
                mContentResolver.notifyChange(AwfulThread.CONTENT_URI, null);
                leave(RESULT_POSTED);
            }

            @Override
            public void failure(VolleyError error) {
                dismissProgressDialog();
                saveThread();
            }
        };
        queueRequest(new SendThreadRequest(getActivity(), cv).build(this, postCallback));
    }


    /**
     * Request a preview of the current post from the site, and display it.
     */
    private void previewPost() {
        ContentValues cv = prepareCV();
        Activity activity = getActivity();
        FragmentManager fragmentManager = getFragmentManager();
        if (cv == null || activity == null || fragmentManager == null) {
            return;
        }

        final PreviewFragment previewFrag = new PreviewFragment();
        previewFrag.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        previewFrag.show(fragmentManager, "Post Preview");

        AwfulRequest.AwfulResultCallback<String> previewCallback = new AwfulRequest.AwfulResultCallback<String>() {
            @Override
            public void success(final String result) {
                previewFrag.setContent(result);
            }

            @Override
            public void failure(VolleyError error) {
                // love dialogs and callbacks very elegant
                if (!previewFrag.isStateSaved() && previewFrag.getActivity() != null && !previewFrag.getActivity().isFinishing()) {
                    previewFrag.dismiss();
                }
                if (getView() != null) {
                    Snackbar.make(getView(), "Preview failed.", Snackbar.LENGTH_LONG)
                            .setAction("Retry", v -> previewPost()).show();
                }
            }
        };


        queueRequest(new PreviewThreadRequest(getActivity(), cv).build(this, previewCallback));
    }


    /**
     * Create a ContentValues representing the current post and its options.
     * <p>
     * Returns null if the data is invalid, e.g. an empty post
     *
     * @return The post data, or null if there was an error.
     */
    @Nullable
    private ContentValues prepareCV() {
        if (threadData == null || threadData.getAsInteger(AwfulMessage.ID) == null) {
            // TODO: if this ever happens, the ID never gets set (and causes an NPE in SendPostRequest) - handle this in a better way?
            // Could use the mThreadId value, but that might be incorrect at this point and post to the wrong thread? Is null thread data an exceptional event?
            Log.e(TAG, "No thread data in sendPost() - no thread ID to post to!");
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity, "Unknown thread ID - can't post!", Toast.LENGTH_LONG).show();
            }
            return null;
        }
        ContentValues cv = new ContentValues(threadData);
        if (isOPEmpty()) {
            dismissProgressDialog();
            getAlertView().setTitle(R.string.message_empty)
                    .setSubtitle(R.string.message_empty_subtext)
                    .show();
            return null;
        }
        if (!TextUtils.isEmpty(mFileAttachment)) {
            cv.put(AwfulMessage.REPLY_ATTACHMENT, mFileAttachment);
        }
        if (postSignature) {
            cv.put(REPLY_SIGNATURE, Constants.YES);
        }
        if (disableEmotes) {
            cv.put(AwfulMessage.REPLY_DISABLE_SMILIES, Constants.YES);
        }

        cv.put(AwfulMessage.POST_SUBJECT, subject.getText().toString());
        cv.put(AwfulMessage.POST_ICON_ID, threadIconPicker.getIcon().iconId);
        cv.put(AwfulMessage.POST_ICON_URL, threadIconPicker.getIcon().iconUrl);
        cv.put(AwfulMessage.POST_CONTENT, messageComposer.getText());
        return cv;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle/navigation stuff
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onResume() {
        super.onResume();
        Timber.v("onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Timber.v("onPause");
        cleanupTasks();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "onDestroyView");
        // final cleanup - some should have already been done in onPause (draft saving etc)
        getLoaderManager().destroyLoader(Constants.THREAD_DRAFT_LOADER_ID);
        getLoaderManager().destroyLoader(Constants.FORUM_LOADER_ID);
    }

    /**
     * Tasks to perform when the thread window moves from the foreground.
     * Basically saves a draft if required, and hides elements like the keyboard
     */
    private void cleanupTasks() {
        autoSave();
        dismissProgressDialog();
        messageComposer.hideKeyboard();
    }


    /**
     * Finish the thread activity, performing cleanup and returning a result code to the activity that created it.
     */
    private void leave(int activityResult) {
        final AwfulActivity activity = getAwfulActivity();
        if (activity != null) {
            activity.setResult(activityResult);
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getView() != null) {
                imm.hideSoftInputFromWindow(getView().getApplicationWindowToken(), 0);
            }
            activity.finish();
        }
    }


    /**
     * Call this when the user tries to leave the activity, so the Save/Discard dialog can be shown if necessary.
     */
    void onNavigateBack() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        } else if (isOPEmpty()) {
            leave(RESULT_CANCELLED);
            return;
        }
        AlertDialog save = new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_reply_dark)
                .setMessage("Save this thread?")
                .setPositiveButton(R.string.save, (dialog, button) -> {
                    // let #autoSave handle it on leaving
                    saveRequired = true;
                    leave(RESULT_CANCELLED);
                })
                .setNegativeButton(R.string.discard, (dialog, which) -> {
                    deleteSavedThread();
                    saveRequired = false;
                    leave(RESULT_CANCELLED);
                })
                .setNeutralButton(R.string.cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();


        getAwfulActivity().setPreferredFont(save.findViewById(androidx.appcompat.R.id.alertTitle));
        getAwfulActivity().setPreferredFont(save.findViewById(android.R.id.message));
        getAwfulActivity().setPreferredFont(save.findViewById(android.R.id.button1));
        getAwfulActivity().setPreferredFont(save.findViewById(android.R.id.button2));
        getAwfulActivity().setPreferredFont(save.findViewById(android.R.id.button3));
    }


    ///////////////////////////////////////////////////////////////////////////
    // Saving draft data
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Trigger a draft save, if required.
     */
    private void autoSave() {
        if (saveRequired && messageComposer != null) {
            if (isOPEmpty()) {
                Log.i(TAG, "Message unchanged, discarding.");
                // TODO: 12/02/2017 does this actually need to check if it's unchanged?
                deleteSavedThread();//if the thread is unchanged, throw it out.
                messageComposer.setText(null, false);
            } else {
                Log.i(TAG, "Message Unsent, saving.");
                saveThread();
            }
        }
    }


    /**
     * Delete any saved thread for the current thread
     */
    private void deleteSavedThread() {
        mContentResolver.delete(AwfulMessage.CONTENT_URI_THREAD, AwfulMessage.ID + "=?", AwfulProvider.int2StrArray(mForumId));
    }


    /**
     * Save a draft thread for the current thread.
     */
    private void saveThread() {
        if (getActivity() != null && mForumId > 0 && messageComposer != null) {
            String content = messageComposer.getText();
            // don't save if the message is empty/whitespace
            // not trimming the actual content, so we retain any whitespace e.g. blank lines after quotes
            if (!content.trim().isEmpty()) {
                Log.i(TAG, "Saving thread! " + content);
                ContentValues post = (threadData == null) ? new ContentValues() : new ContentValues(threadData);
                post.put(AwfulMessage.ID, mForumId);
                post.put(AwfulMessage.POST_CONTENT, content);
                post.put(AwfulMessage.EPOC_TIMESTAMP, System.currentTimeMillis());
                post.put(AwfulMessage.POST_SUBJECT, subject.getText().toString());
                post.put(AwfulMessage.POST_ICON_ID, threadIconPicker.getIcon().iconId);
                post.put(AwfulMessage.POST_ICON_URL, threadIconPicker.getIcon().iconUrl);
                if (mFileAttachment != null) {
                    post.put(AwfulMessage.REPLY_ATTACHMENT, mFileAttachment);
                }
                if (mContentResolver.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_THREAD, mForumId), post, null, null) < 1) {
                    mContentResolver.insert(AwfulMessage.CONTENT_URI_THREAD, post);
                }
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Menus
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Timber.v("onCreateOptionsMenu");
        inflater.inflate(R.menu.post_thread, menu);

        MenuItem attach = menu.findItem(R.id.add_attachment);
        if (attach != null && getPrefs() != null) {
            attach.setEnabled(getPrefs().hasPlatinum);
            attach.setVisible(getPrefs().hasPlatinum);
        }
        MenuItem remove = menu.findItem(R.id.remove_attachment);
        if (remove != null && getPrefs() != null && this.mFileAttachment != null) {
            remove.setEnabled(getPrefs().hasPlatinum);
            remove.setVisible(getPrefs().hasPlatinum);
            String[] filepath = this.mFileAttachment.split("/");
            String filename = filepath[filepath.length-1];
            remove.setTitle("Remove " + filename);
        }
        MenuItem disableEmoticons = menu.findItem(R.id.disableEmots);
        if (disableEmoticons != null) {
            disableEmoticons.setChecked(disableEmotes);
        }
        MenuItem sig = menu.findItem(R.id.signature);
        if (sig != null) {
            sig.setChecked(postSignature);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.v("onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.submit_button:
                showSubmitDialog();
                break;
            case R.id.add_attachment:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), ADD_ATTACHMENT);
                break;
            case R.id.remove_attachment:
                this.mFileAttachment = null;
                Toast removeToast = Toast.makeText(getAwfulActivity(), getAwfulActivity().getResources().getText(R.string.file_removed), Toast.LENGTH_SHORT);
                removeToast.show();
                invalidateOptionsMenu();
                break;
            case R.id.signature:
                item.setChecked(!item.isChecked());
                postSignature = item.isChecked();
                break;
            case R.id.disableEmots:
                item.setChecked(!item.isChecked());
                disableEmotes = item.isChecked();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }


    @Override
    public void onPreferenceChange(AwfulPreferences prefs, String key) {
        super.onPreferenceChange(prefs, key);
        //refresh the menu to show/hide attach option (plat only)
        invalidateOptionsMenu();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Attachment handling
    ///////////////////////////////////////////////////////////////////////////

    // TODO: 13/04/2017 make a separate attachment component and stick all this in there

    private void addAttachment() {
        addAttachment(attachmentData);
        attachmentData = null;
    }

    private void addAttachment(Intent data) {
        Uri selectedImageUri = data.getData();
        String path = getFilePath(selectedImageUri);
        if (path == null) {
            setAttachment(null, getString(R.string.file_error));
            return;
        }

        File attachment = new File(path);
        String filename = attachment.getName();
        if (!attachment.isFile() || !attachment.canRead()) {
            setAttachment(null, String.format(getString(R.string.file_unreadable), filename));
            return;
        } else if (!StringUtils.endsWithAny(filename.toLowerCase(), ".jpg", ".jpeg", ".png", ".gif")) {
            setAttachment(null, String.format(getString(R.string.file_wrong_filetype), filename));
            return;
        } else if (attachment.length() > ATTACHMENT_MAX_BYTES) {
            setAttachment(null, String.format(getString(R.string.file_too_big), filename));
            return;
        }

        // check the image size without creating a bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int height = options.outHeight;
        int width = options.outWidth;
        if (width > ATTACHMENT_MAX_WIDTH || height > ATTACHMENT_MAX_HEIGHT) {
            setAttachment(null, String.format(getString(R.string.file_resolution_too_big), filename, width, height));
            return;
        }

        setAttachment(path, String.format(getString(R.string.file_attached), filename));
    }


    private void setAttachment(@Nullable String attachment, @NonNull String toastMessage) {
        mFileAttachment = attachment;
        Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_LONG).show();
        invalidateOptionsMenu();
    }


    private String getFilePath(final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(this.getActivity(), uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
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
    private String getDataColumn(Uri uri, String selection,
                                 String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = this.getActivity().getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


    ///////////////////////////////////////////////////////////////////////////
    // Misc utility stuff
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Utility method to check if the composer contains an empty post
     */
    private boolean isOPEmpty() {
        return messageComposer.getText().trim().isEmpty();
    }

    /**
     * Convert an epoch timestamp to a duration relative to now.
     * <p>
     * Returns the duration in a "1d 4h 22m 30s" format, omitting units with zero values.
     */
    private String epochToSimpleDuration(long epoch) {
        Duration diff = Duration.between(Instant.ofEpochSecond((epoch / 1000)), Instant.now()).abs();
        String time = "";
        if (diff.toDays() > 0) {
            time += " " + diff.toDays() + "d";
            diff = diff.minusDays(diff.toDays());
        }
        if (diff.toHours() > 0) {
            time += " " + diff.toHours() + "h";
            diff = diff.minusHours(diff.toHours());
        }
        if (diff.toMinutes() > 0) {
            time += " " + diff.toMinutes() + "m";
            diff = diff.minusMinutes(diff.toMinutes());
        }

        time += " " + diff.getSeconds() + "s";
        return time;
    }


    ///////////////////////////////////////////////////////////////////////////
    // UI things
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Dismiss the progress dialog and set it to null, if it isn't already.
     */
    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public String getTitle() {
        return "Post Thread";
    }


    ///////////////////////////////////////////////////////////////////////////
    // Async classes etc
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Provides a Loader that pulls draft data for the current thread from the DB.
     */
    private class DraftThreadLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            Log.i(TAG, "Create Thread Cursor: " + mForumId);
            return new CursorLoader(getActivity(),
                    ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_THREAD, mForumId),
                    AwfulProvider.DraftThreadProjection,
                    null,
                    null,
                    null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
            if (aData.isClosed() || !aData.moveToFirst()) {
                // no draft saved for this thread
                return;
            }
            // if there's some quote data, deserialise it into a SavedDraft
            String quoteData = aData.getString(aData.getColumnIndex(AwfulMessage.POST_CONTENT));
            if (TextUtils.isEmpty(quoteData)) {
                return;
            }
            String subject = aData.getString(aData.getColumnIndex(AwfulMessage.POST_SUBJECT));
            long draftTimestamp = aData.getLong(aData.getColumnIndex(AwfulMessage.EPOC_TIMESTAMP));
            String draftThread = NetworkUtils.unencodeHtml(quoteData);

            String draftIconId = aData.getString(aData.getColumnIndex(AwfulMessage.POST_ICON_ID));
            String draftIconUrl = aData.getString(aData.getColumnIndex(AwfulMessage.POST_ICON_URL));

            savedDraft = new SavedDraft(draftThread, subject,draftIconId,draftIconUrl, draftTimestamp);
            if (Constants.DEBUG) {
                Log.i(TAG, "Saved thread message: " + draftThread);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {

        }
    }


    /**
     * Provides a Loader that gets metadata for the current thread, and dsiplays its title
     */
    private class ForumInfoCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulForum.CONTENT_URI, mForumId),
                    AwfulProvider.ForumProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
            Log.v(TAG, "Thread title finished, populating.");
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }
    }


    private static class SavedDraft {
        @NonNull
        private final String content;
        private final String iconId;
        private final String iconUrl;
        private final String subject;
        private final long timestamp;

        SavedDraft(@NonNull String content, String subject, String iconId, String iconUrl, long timestamp) {
            this.content = content;
            this.subject = subject;
            this.iconId = iconId;
            this.iconUrl = iconUrl;
            this.timestamp = timestamp;
        }
    }
}
