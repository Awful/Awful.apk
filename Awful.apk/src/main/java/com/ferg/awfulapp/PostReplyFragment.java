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
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.reply.MessageComposer;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.EditRequest;
import com.ferg.awfulapp.task.PreviewEditRequest;
import com.ferg.awfulapp.task.PreviewPostRequest;
import com.ferg.awfulapp.task.QuoteRequest;
import com.ferg.awfulapp.task.ReplyRequest;
import com.ferg.awfulapp.task.SendEditRequest;
import com.ferg.awfulapp.task.SendPostRequest;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.util.AwfulUtils;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.ferg.awfulapp.constants.Constants.ATTACHMENT_MAX_BYTES;
import static com.ferg.awfulapp.constants.Constants.ATTACHMENT_MAX_HEIGHT;
import static com.ferg.awfulapp.constants.Constants.ATTACHMENT_MAX_WIDTH;
import static com.ferg.awfulapp.thread.AwfulMessage.REPLY_DISABLE_SMILIES;
import static com.ferg.awfulapp.thread.AwfulMessage.REPLY_SIGNATURE;
import static com.ferg.awfulapp.thread.AwfulMessage.TYPE_EDIT;
import static com.ferg.awfulapp.thread.AwfulMessage.TYPE_NEW_REPLY;
import static com.ferg.awfulapp.thread.AwfulMessage.TYPE_QUOTE;

public class PostReplyFragment extends AwfulFragment {
    public static final int REQUEST_POST = 5;
    public static final int RESULT_POSTED = 6;
    public static final int RESULT_CANCELLED = 7;
    public static final int RESULT_EDITED = 8;
    public static final int ADD_ATTACHMENT = 9;
    private static final String TAG = "PostReplyFragment";
    @BindView(R.id.thread_title)
    TextView threadTitleView = null;
    private ContentResolver mContentResolver;
    @Nullable
    private ProgressDialog progressDialog;

    private int mThreadId;
    private int mPostId;
    private int mReplyType;
    private String mThreadTitle;
    private boolean saveRequired = true;
    private String mFileAttachment;
    private boolean disableEmots = false;
    private boolean postSignature = false;
    private ContentValues replyData = null;
    private ReplyCallback mReplyDataCallback = new ReplyCallback();
    private ThreadDataCallback mThreadLoaderCallback;
    private ThreadContentObserver mThreadObserver = new ThreadContentObserver(mHandler);
    private int draftReplyType;
    private String draftReplyData;
    private long draftReplyTimestamp;
    private Intent attachmentData;
    private MessageComposer messageComposer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.e(TAG, "onCreate");
        setHasOptionsMenu(true);
        setRetainInstance(false);
        mThreadLoaderCallback = new ThreadDataCallback();

        final Activity activity = getActivity();
        mContentResolver = activity.getContentResolver();
        Intent intent = activity.getIntent();

        mReplyType = intent.getIntExtra(Constants.EDITING, -999);
        mPostId = intent.getIntExtra(Constants.REPLY_POST_ID, 0);
        mThreadId = intent.getIntExtra(Constants.REPLY_THREAD_ID, 0);

        boolean badRequest = false;
        if (mReplyType < 0 || mThreadId == 0) {
            // we always need a valid type and thread ID
            badRequest = true;
        } else if (mPostId == 0 &&
                (mReplyType == TYPE_EDIT || mReplyType == TYPE_QUOTE)) {
            // edits and quotes always need a post ID too
            badRequest = true;
        }

        if (badRequest) {
            activity.finish();
        } else {
            loadReply(mReplyType, mThreadId, mPostId);
        }
    }


    /**
     * Initiate a new reply/edit by passing a request to the site and handling its response.
     *
     * @param mReplyType    The type of request we're making (reply/quote/edit)
     * @param mThreadId     The ID of the thread
     * @param mPostId       The ID of the post being edited/quoted, if applicable
     */
    private void loadReply(int mReplyType, int mThreadId, int mPostId) {
        progressDialog = ProgressDialog.show(getActivity(), "Loading", "Fetching Message...", true, true);

        // create a callback to handle the reply data from the site
        AwfulRequest.AwfulResultCallback<ContentValues> loadCallback = new AwfulRequest.AwfulResultCallback<ContentValues>() {
            @Override
            public void success(ContentValues result) {
                replyData = result;
                if (result.containsKey(AwfulMessage.REPLY_CONTENT)) {
                    // update the message composer with the provided reply data
                    String replyData = NetworkUtils.unencodeHtml(result.getAsString(AwfulMessage.REPLY_CONTENT));
                    if (!TextUtils.isEmpty(replyData)) {
                        if (replyData.endsWith("[/quote]")) {
                            replyData = replyData + "\n\n";
                        }
                        messageComposer.setText(replyData, true);
                    } else {
                        messageComposer.setText(null, false);
                    }
                    // set any options and update the menu
                    postSignature = getCheckedAndRemove(REPLY_SIGNATURE, result);
                    disableEmots = getCheckedAndRemove(REPLY_DISABLE_SMILIES, result);
                    invalidateOptionsMenu();
                }
                dismissProgressDialog();
                displayDraftAlert();
            }

            @Override
            public void failure(VolleyError error) {
                dismissProgressDialog();
                //allow time for the error to display, then close the window
                mHandler.postDelayed(() -> leave(RESULT_CANCELLED), 3000);
            }
        };
        switch (mReplyType) {
            case TYPE_NEW_REPLY:
                queueRequest(new ReplyRequest(getActivity(), mThreadId).build(this, loadCallback));
                break;
            case TYPE_QUOTE:
                queueRequest(new QuoteRequest(getActivity(), mThreadId, mPostId).build(this, loadCallback));
                break;
            case TYPE_EDIT:
                queueRequest(new EditRequest(getActivity(), mThreadId, mPostId).build(this, loadCallback));
                break;
            default:
                Toast.makeText(getActivity(), "Unknown reply type: " + mReplyType, Toast.LENGTH_LONG).show();
                leave(RESULT_CANCELLED);
        }
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
     * Dismiss the progress dialog and set it to null, if it isn't already.
     */
    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);
        if (DEBUG) Log.e(TAG, "onCreateView");
        return inflateView(R.layout.post_reply, aContainer, aInflater);
    }


    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
        if (DEBUG) Log.e(TAG, "onActivityCreated");
        Activity activity = getActivity();
        ButterKnife.bind(this, activity);

        messageComposer = (MessageComposer) getChildFragmentManager().findFragmentById(R.id.message_composer_fragment);
        messageComposer.setBackgroundColor(ColorProvider.BACKGROUND.getColor());
        messageComposer.setTextColor(ColorProvider.PRIMARY_TEXT.getColor());
        setTitle(getTitle());

        activity.getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
        refreshLoader();
        refreshThreadInfo();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ADD_ATTACHMENT) {
                if (AwfulUtils.isMarshmallow()) {
                    int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        this.attachmentData = data;
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE);
                    } else {
                        addAttachment(data);
                    }
                } else {
                    addAttachment(data);
                }
            }
        }
    }


    /**
     * Update the title view to show the current thread title, if we have it
     */
    private void updateThreadTitle() {
        if (threadTitleView != null) {
            threadTitleView.setText(mThreadTitle == null ? "" : mThreadTitle);
        }
    }

    protected void addAttachment() {
        addAttachment(attachmentData);
        attachmentData = null;
    }

    protected void addAttachment(Intent data) {
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


    public String getFilePath(final Uri uri) {

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
    public String getDataColumn(Uri uri, String selection,
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
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateThreadTitle();
        if (DEBUG) Log.e(TAG, "onResume");
    }

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

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.e(TAG, "onPause");
        cleanupTasks();
    }


    void onNavigateBack() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        } else if (isReplyEmpty()) {
            leave(RESULT_CANCELLED);
            return;
        }
        new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_reply_dark)
                .setMessage("Save this post?")
                .setPositiveButton("Save", (dialog, button) -> {
                    // let #autoSave handle it on leaving
                    saveRequired = true;
                    leave(RESULT_CANCELLED);
                })
                .setNegativeButton("Discard", (dialog, which) -> {
                    deleteSavedReply();
                    saveRequired = false;
                    leave(RESULT_CANCELLED);
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                })
                .setCancelable(true)
                .show();

    }


    private void autoSave() {
        if (saveRequired && messageComposer != null) {
            if (isReplyEmpty()) {
                Log.i(TAG, "Message unchanged, discarding.");
                // TODO: 12/02/2017 does this actually need to check if it's unchanged?
                deleteSavedReply();//if the reply is unchanged, throw it out.
                messageComposer.setText(null, false);
            } else {
                Log.i(TAG, "Message Unsent, saving.");
                saveReply();
            }
        }
    }


    private boolean isReplyEmpty() {
        return messageComposer.getText().trim().isEmpty();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) Log.e(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.post_reply, menu);

        MenuItem attach = menu.findItem(R.id.add_attachment);
        if (attach != null && mPrefs != null) {
            attach.setEnabled(mPrefs.hasPlatinum);
            attach.setVisible(mPrefs.hasPlatinum);
        }
        MenuItem remove = menu.findItem(R.id.remove_attachment);
        if (remove != null && mPrefs != null) {
            remove.setEnabled((mPrefs.hasPlatinum && this.mFileAttachment != null));
            remove.setVisible(mPrefs.hasPlatinum && this.mFileAttachment != null);
        }
        MenuItem disableEmoticons = menu.findItem(R.id.disableEmots);
        if (disableEmoticons != null) {
            disableEmoticons.setChecked(disableEmots);
        }
        MenuItem sig = menu.findItem(R.id.signature);
        if (sig != null) {
            sig.setChecked(postSignature);
        }
    }

    @Override
    public void onPreferenceChange(AwfulPreferences prefs, String key) {
        super.onPreferenceChange(prefs, key);
        //refresh the menu to show/hide attach option (plat only)
        invalidateOptionsMenu();
    }

    private void displayDraftAlert() {
        Activity activity = getActivity();
        if (activity == null || TextUtils.isEmpty(draftReplyData)) {
            return;
        }

        String template = "You have a %s:" +
                "<br/><br/>" +
                "<i>%s</i>" +
                "<br/><br/>" +
                "Saved %s ago";

        String type;
        switch (draftReplyType) {
            case TYPE_EDIT:
                type = "Saved Edit";
                break;
            case TYPE_QUOTE:
                type = "Saved Quote";
                break;
            case TYPE_NEW_REPLY:
            default:
                type = "Saved Reply";
                break;
        }

        final int MAX_PREVIEW_LENGTH = 140;
        String previewText = StringUtils.substring(draftReplyData, 0, MAX_PREVIEW_LENGTH).replaceAll("\\n", "<br/>");
        if (draftReplyData.length() > MAX_PREVIEW_LENGTH) {
            previewText += "...";
        }

        String message = String.format(template, type.toLowerCase(), previewText, epocToSimpleDate(draftReplyTimestamp));
        String positiveLabel = (mReplyType == TYPE_QUOTE) ? "Append" : "Use";
        new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_reply_dark)
                .setTitle(type)
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(positiveLabel, (dialog, which) -> {
                    String newContent = draftReplyData;
                    // If we're quoting something, stick it after the draft reply (and add some whitespace too)
                    if (mReplyType == TYPE_QUOTE) {
                        newContent += "\n\n" + messageComposer.getText();
                    }
                    messageComposer.setText(newContent, true);
                })
                .setNegativeButton("Discard", (dialog, which) -> deleteSavedReply())
                // avoid accidental draft losses by forcing a decision
                .setCancelable(false)
                .show();
    }

    private String epocToSimpleDate(long epoc) {
        Duration diff = Duration.between(Instant.ofEpochSecond( (epoc/1000) ),Instant.now()).abs();
        String time = "";
        if (diff.toDays() > 0) {
            time+=" "+ diff.toDays()+"d";
            diff = diff.minusDays(diff.toDays());
        }
        if (diff.toHours() > 0) {
            time+=" "+ diff.toHours()+"h";
            diff = diff.minusHours(diff.toHours());
        }
        if (diff.toMinutes() > 0) {
            time+=" "+ diff.toMinutes()+"m";
            diff = diff.minusMinutes(diff.toMinutes());
        }

        time+=" "+ diff.getSeconds()+"s";
        return time;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.e(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.submit_button:
                postReply();
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
                disableEmots = item.isChecked();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "onDestroyView");
        // final cleanup - some should have already been done in onPause (draft saving etc)
        getLoaderManager().destroyLoader(Constants.REPLY_LOADER_ID);
        getLoaderManager().destroyLoader(Constants.MISC_LOADER_ID);
        getActivity().getContentResolver().unregisterContentObserver(mThreadObserver);
        messageComposer = null;
    }

    /**
     * Tasks to perform when the reply window moves from the foreground.
     * Basically saves a draft if required, and hides elements like the keyboard
     */
    private void cleanupTasks() {
        autoSave();
        dismissProgressDialog();
        messageComposer.hideKeyboard();
    }

    private void postReply() {
        new AlertDialog.Builder(getActivity())
                .setTitle((mReplyType == TYPE_EDIT) ? "Confirm Edit?" : "Confirm Post?")
                .setPositiveButton(R.string.post_reply,
                        (dialog, button) -> {
                            if (progressDialog == null && getActivity() != null) {
                                progressDialog = ProgressDialog.show(getActivity(), "Posting", "Hopefully it didn't suck...", true, true);
                            }
                            saveReply();
                            sendPost();
                        })
                .setNeutralButton("Preview", (dialog, button) -> previewPost())
                .setNegativeButton(R.string.cancel, (dialog, button) -> {})
                .show();
    }

    private void sendPost() {
        ContentValues cv = prepareCV();
        if (cv == null) {
            return;
        }
        AwfulRequest.AwfulResultCallback<Void> postCallback = new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                dismissProgressDialog();
                deleteSavedReply();
                saveRequired = false;

                Context context = getContext();
                if (context != null) {
                    Toast.makeText(context, context.getString(R.string.post_sent), Toast.LENGTH_LONG).show();
                }
                mContentResolver.notifyChange(AwfulThread.CONTENT_URI, null);
                leave(mReplyType == TYPE_EDIT ? mPostId : RESULT_POSTED);
            }

            @Override
            public void failure(VolleyError error) {
                dismissProgressDialog();
                saveReply();
            }
        };
        switch (mReplyType) {
            case TYPE_QUOTE:
            case TYPE_NEW_REPLY:
                queueRequest(new SendPostRequest(getActivity(), cv).build(this, postCallback));
                break;
            case TYPE_EDIT:
                queueRequest(new SendEditRequest(getActivity(), cv).build(this, postCallback));
                break;
            default:
                getActivity().finish();
        }
    }

    /**
     * Delete any saved reply for the current thread
     */
    private void deleteSavedReply() {
        mContentResolver.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID + "=?", AwfulProvider.int2StrArray(mThreadId));
    }

    /**
     * Save a draft reply for the current thread.
     */
    private void saveReply() {
        if (getActivity() != null && mThreadId > 0 && messageComposer != null) {
            String content = messageComposer.getText();
            // don't save if the message is empty/whitespace
            // not trimming the actual content, so we retain any whitespace e.g. blank lines after quotes
            if (!content.trim().isEmpty()) {
                Log.i(TAG, "Saving reply! " + content);
                ContentValues post = (replyData == null) ? new ContentValues() : new ContentValues(replyData);
                post.put(AwfulMessage.ID, mThreadId);
                post.put(AwfulMessage.TYPE, mReplyType);
                post.put(AwfulMessage.REPLY_CONTENT, content);
                post.put(AwfulMessage.EPOC_TIMESTAMP, System.currentTimeMillis());
                if (mFileAttachment != null) {
                    post.put(AwfulMessage.REPLY_ATTACHMENT, mFileAttachment);
                }
                if (mContentResolver.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mThreadId), post, null, null) < 1) {
                    mContentResolver.insert(AwfulMessage.CONTENT_URI_REPLY, post);
                }
            }
        }
    }

    private void previewPost() {
        ContentValues cv = prepareCV();
        if (cv == null) {
            return;
        }
        final PreviewFragment previewFrag = new PreviewFragment();
        previewFrag.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        previewFrag.show(getFragmentManager(), "Post Preview");
        // TODO: 12/02/2017 this result should already be on the UI thread?
        AwfulRequest.AwfulResultCallback<String> previewCallback = new AwfulRequest.AwfulResultCallback<String>() {
            @Override
            public void success(final String result) {
                getAwfulActivity().runOnUiThread(() -> previewFrag.setContent(result));
            }

            @Override
            public void failure(VolleyError error) {
                previewFrag.dismiss();
                if (getView() != null) {
                    Snackbar.make(getView(), "Preview failed.", Snackbar.LENGTH_LONG)
                            .setAction("Retry", v -> previewPost()).show();
                }
            }
        };
        if (mReplyType == TYPE_EDIT) {
            queueRequest(new PreviewEditRequest(getActivity(), cv).build(this, previewCallback));
        } else {
            queueRequest(new PreviewPostRequest(getActivity(), cv).build(this, previewCallback));
        }
    }

    private ContentValues prepareCV() {
        if (replyData == null || replyData.getAsInteger(AwfulMessage.ID) == null) {
            // TODO: if this ever happens, the ID never gets set (and causes an NPE in SendPostRequest) - handle this in a better way?
            // Could use the mThreadId value, but that might be incorrect at this point and post to the wrong thread? Is null reply data an exceptional event?
            Log.e(TAG, "No reply data in sendPost() - no thread ID to post to!");
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity, "Unknown thread ID - can't post!", Toast.LENGTH_LONG).show();
            }
            return null;
        }
        ContentValues cv = new ContentValues(replyData);
        if (isReplyEmpty()) {
            dismissProgressDialog();
            new AlertBuilder().setTitle(R.string.message_empty)
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
        if (disableEmots) {
            cv.put(AwfulMessage.REPLY_DISABLE_SMILIES, Constants.YES);
        }
        cv.put(AwfulMessage.REPLY_CONTENT, messageComposer.getText());
        return cv;
    }

    private void refreshLoader() {
        restartLoader(Constants.REPLY_LOADER_ID, null, mReplyDataCallback);
    }

    private void refreshThreadInfo() {
        restartLoader(Constants.MISC_LOADER_ID, null, mThreadLoaderCallback);
    }

    @Override
    public String getTitle() {
        switch (mReplyType) {
            case TYPE_EDIT:
                return "Editing";
            case TYPE_QUOTE:
                return "Quote";
            case TYPE_NEW_REPLY:
            default:
                return "Reply";
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addAttachment();
                } else {
                    Toast.makeText(getActivity(), R.string.no_file_permission_attachment, Toast.LENGTH_LONG).show();
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private class ReplyCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            Log.i(TAG, "Create Reply Cursor: " + mThreadId);
            return new CursorLoader(getActivity(),
                    ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mThreadId),
                    AwfulProvider.DraftPostProjection,
                    null,
                    null,
                    null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
            Log.e(TAG, "Reply load finished, populating: " + aData.getCount());
            if (!aData.isClosed() && aData.moveToFirst()) {
                draftReplyType = aData.getInt(aData.getColumnIndex(AwfulMessage.TYPE));
                int postId = aData.getInt(aData.getColumnIndex(AwfulPost.EDIT_POST_ID));
                if ((draftReplyType == TYPE_EDIT && postId != mPostId) || draftReplyType != TYPE_EDIT && mReplyType == TYPE_EDIT) {
                    //if the saved draft message is an edit, but not for this post, ignore it.
                    draftReplyType = 0;
                    return;
                }
                draftReplyTimestamp = aData.getLong(aData.getColumnIndex(AwfulMessage.EPOC_TIMESTAMP));
                String quoteData = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_CONTENT));
                if (!TextUtils.isEmpty(quoteData)) {
                    draftReplyData = NetworkUtils.unencodeHtml(quoteData);
                    Log.i(TAG, draftReplyType + "Saved reply message: " + draftReplyData);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {

        }
    }

    private class ThreadDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulThread.CONTENT_URI, mThreadId),
                    AwfulProvider.ThreadProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
            Log.v(TAG, "Thread title finished, populating.");
            if (aData.getCount() > 0 && aData.moveToFirst()) {
                //threadClosed = aData.getInt(aData.getColumnIndex(AwfulThread.LOCKED))>0;
                mThreadTitle = aData.getString(aData.getColumnIndex(AwfulThread.TITLE));
                updateThreadTitle();
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }
    }

    private class ThreadContentObserver extends ContentObserver {
        ThreadContentObserver(Handler aHandler) {
            super(aHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) Log.v(TAG, "Thread Data update.");
            refreshThreadInfo();
        }
    }
}
