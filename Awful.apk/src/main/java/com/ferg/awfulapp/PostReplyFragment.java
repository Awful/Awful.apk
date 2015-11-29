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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
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
import com.ferg.awfulapp.task.EditRequest;
import com.ferg.awfulapp.task.QuoteRequest;
import com.ferg.awfulapp.task.ReplyRequest;
import com.ferg.awfulapp.task.SendEditRequest;
import com.ferg.awfulapp.task.SendPostRequest;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.util.AwfulUtils;

import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;

import java.io.File;

public class PostReplyFragment extends AwfulFragment {
    public static final int REQUEST_POST = 5;
    public static final int RESULT_POSTED = 6;
    public static final int RESULT_CANCELLED = 7;
    public static final int RESULT_EDITED = 8;
    public static final int ADD_ATTACHMENT = 9;
    private static final String TAG = "PostReplyFragment";

    private ContentResolver mContentResolver;
    private EditText mMessage;
    private ProgressDialog mDialog;
    private int mThreadId;
    private int mPostId;
    private int mReplyType;
    private String mThreadTitle;
    private boolean sendSuccessful = false;
    private String originalReplyData = "";
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
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private Intent attachmentData;

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
                (mReplyType == AwfulMessage.TYPE_EDIT || mReplyType == AwfulMessage.TYPE_QUOTE)) {
            // edits and quotes always need a post ID too
            badRequest = true;
        }

        if (badRequest) {
            activity.finish();
        } else {
            loadReply(mReplyType, mThreadId, mPostId);
        }
    }

    private void loadReply(int mReplyType, int mThreadId, int mPostId) {
        mDialog = ProgressDialog.show(getActivity(), "Loading", "Fetching Message...", true, true);
        AwfulRequest.AwfulResultCallback<ContentValues> loadCallback = new AwfulRequest.AwfulResultCallback<ContentValues>() {
            @Override
            public void success(ContentValues result) {
                replyData = result;
                if (result.containsKey(AwfulMessage.REPLY_CONTENT)) {
                    String quoteData = NetworkUtils.unencodeHtml(result.getAsString(AwfulMessage.REPLY_CONTENT));
                    if (!TextUtils.isEmpty(quoteData)) {
                        if (quoteData.endsWith("[/quote]")) {
                            quoteData = quoteData + "\n\n";
                        }
                        originalReplyData = quoteData;
                        if (mMessage != null) {
                            mMessage.setText(quoteData);
                            mMessage.setSelection(quoteData.length());
                        }
                    } else {
                        originalReplyData = "";
                    }
                    if (result.containsKey(AwfulMessage.REPLY_SIGNATURE)) {
                        postSignature = "checked".equals(result.getAsString(AwfulMessage.REPLY_SIGNATURE));
                        invalidateOptionsMenu();
                        result.remove(AwfulMessage.REPLY_SIGNATURE);
                    }
                    if (result.containsKey(AwfulMessage.REPLY_DISABLE_SMILIES)) {
                        disableEmots = "checked".equals(result.getAsString(AwfulMessage.REPLY_DISABLE_SMILIES));
                        invalidateOptionsMenu();
                        result.remove(AwfulMessage.REPLY_DISABLE_SMILIES);
                    }
                }
                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                }
                displayDraftAlert();
            }

            @Override
            public void failure(VolleyError error) {
                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                }
                //allow time for the error to display, then close the window
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null) {
                            leave();
                        }
                    }
                }, 3000);
            }
        };
        switch (mReplyType) {
            case AwfulMessage.TYPE_NEW_REPLY:
                queueRequest(new ReplyRequest(getActivity(), mThreadId).build(this, loadCallback));
                break;
            case AwfulMessage.TYPE_QUOTE:
                queueRequest(new QuoteRequest(getActivity(), mThreadId, mPostId).build(this, loadCallback));
                break;
            case AwfulMessage.TYPE_EDIT:
                queueRequest(new EditRequest(getActivity(), mThreadId, mPostId).build(this, loadCallback));
                break;
            default:
                Toast.makeText(getActivity(), R.string.critical_error, Toast.LENGTH_LONG).show();
                leave();
        }
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);
        if (DEBUG) Log.e(TAG, "onCreateView");

        View result = inflateView(R.layout.post_reply, aContainer, aInflater);

        mMessage = aq.find(R.id.post_message).getEditText();
        mMessage.setText("");

        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
        if (DEBUG) Log.e(TAG, "onActivityCreated");

        mMessage.setBackgroundColor(ColorProvider.getBackgroundColor());
        mMessage.setTextColor(ColorProvider.getTextColor());
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
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
                }else{
                    addAttachment(data);
                }
            }
        }
    }
    protected void addAttachment() {
        addAttachment(attachmentData);
        attachmentData = null;
    }

    protected void addAttachment(Intent data) {
        Toast attachmentToast;
        Uri selectedImageUri = data.getData();
        String path = getFilePath(selectedImageUri);
        final Activity activity = this.getActivity();
        if (path == null) {
            attachmentToast = Toast.makeText(activity, this.getString(R.string.file_error), Toast.LENGTH_LONG);
            mFileAttachment = null;
        } else {
            File attachment = new File(path);
            if (attachment.isFile() && attachment.canRead()) {
                if (attachment.length() > (1024 * 1024)) {
                    attachmentToast = Toast.makeText(activity, String.format(this.getString(R.string.file_too_big), attachment.getName()), Toast.LENGTH_LONG);
                    mFileAttachment = null;
                } else {
                    mFileAttachment = path;
                    attachmentToast = Toast.makeText(activity, String.format(this.getString(R.string.file_attached), attachment.getName()), Toast.LENGTH_LONG);
                }
            } else {
                attachmentToast = Toast.makeText(activity, String.format(this.getString(R.string.file_unreadable), attachment.getName()), Toast.LENGTH_LONG);
                mFileAttachment = null;
            }
        }
        attachmentToast.show();
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
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
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
        if (DEBUG) Log.e(TAG, "onResume");
    }

    private void leave() {
        final AwfulActivity activity = getAwfulActivity();
        if (activity != null) {
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

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG) Log.e(TAG, "onStop");
        cleanupTasks();
    }

    private void autosave() {
        if (!sendSuccessful && mMessage != null) {
            if (mMessage.length() < 1 || mMessage.getText().toString().replaceAll("\\s", "").length() < 1 || this.sendSuccessful) {
                Log.i(TAG, "Message unchanged, discarding.");
                deleteReply();//if the reply is unchanged, throw it out.
                mMessage.setText("");
            } else {
                Log.i(TAG, "Message Unsent, saving.");
                saveReply();
            }
        }
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
        String title = null;
        String positiveButton = "Keep";
        StringBuilder message = new StringBuilder();
        switch (draftReplyType) {
            case AwfulMessage.TYPE_NEW_REPLY:
                title = "Saved Reply";
                message.append("You have a saved reply");
                break;
            case AwfulMessage.TYPE_QUOTE:
                title = "Saved Quote";
                message.append("You have a saved quote");
                break;
            case AwfulMessage.TYPE_EDIT:
                title = "Saved Edit";
                message.append("You have a saved edit");
                break;
        }
        if (mReplyType == AwfulMessage.TYPE_QUOTE) {
            positiveButton = "Append";
        }
        message.append(":<br/><br/><i>");
        if (draftReplyData.length() > 140) {
            message.append(draftReplyData.substring(0, 140).replaceAll("\\n", "<br/>"));
            message.append("...");
        } else {
            message.append(draftReplyData.replaceAll("\\n", "<br/>"));
        }
        message.append("</i>");
        if (draftReplyTimestamp > 0) {
            message.append("<br/><br/>Saved ");
            message.append(epocToSimpleDate(draftReplyTimestamp));
            message.append(" ago");
        }
        int[] attrs = { R.attr.iconMenuReplyDark};
        TypedArray ta = activity.getTheme().obtainStyledAttributes(attrs);
        new AlertDialog.Builder(activity)
                .setIcon(ta.getDrawable(0))
                .setTitle(title)
                .setMessage(android.text.Html.fromHtml(message.toString()))
                .setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mReplyType == AwfulMessage.TYPE_QUOTE) {
                            originalReplyData = draftReplyData + "\n" + originalReplyData;
                        } else if (mReplyType == AwfulMessage.TYPE_NEW_REPLY || mReplyType == AwfulMessage.TYPE_EDIT) {
                            originalReplyData = draftReplyData + "\n\n";
                        }
                        mMessage.setText(originalReplyData);
                        mMessage.setSelection(originalReplyData.length());
                    }
                })
                .setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    private String epocToSimpleDate(long epoc) {
        Period diff = new Period(epoc, System.currentTimeMillis(), PeriodType.standard());
        PeriodType type;
        if (diff.getMonths() > 0) {
            type = PeriodType.yearMonthDay();
        } else if (diff.getWeeks() > 0) {
            type = PeriodType.yearWeekDay();
        } else if (diff.getDays() > 0) {
            type = PeriodType.dayTime().withSecondsRemoved().withMillisRemoved().withMinutesRemoved();
        } else if (diff.getMinutes() > 0) {
            type = PeriodType.time().withMillisRemoved().withSecondsRemoved();
        } else {
            type = PeriodType.time().withMillisRemoved();
        }
        return PeriodFormat.getDefault().print(new Period(epoc, System.currentTimeMillis(), type));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.e(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.bbcode_bold:
                insertBBCode(BBCODE.BOLD);
                break;
            case R.id.bbcode_italics:
                insertBBCode(BBCODE.ITALICS);
                break;
            case R.id.bbcode_underline:
                insertBBCode(BBCODE.UNDERLINE);
                break;
            case R.id.bbcode_strikeout:
                insertBBCode(BBCODE.STRIKEOUT);
                break;
            case R.id.bbcode_url:
                insertBBCode(BBCODE.URL);
                break;
            case R.id.bbcode_video:
                insertBBCode(BBCODE.VIDEO);
                break;
            case R.id.bbcode_image:
                insertBBCode(BBCODE.IMAGE);
                break;
            case R.id.bbcode_thumbnail:
                insertBBCode(BBCODE.THUMBNAIL);
                break;
            case R.id.bbcode_quote:
                insertBBCode(BBCODE.QUOTE);
                break;
            case R.id.bbcode_spoiler:
                insertBBCode(BBCODE.SPOILER);
                break;
            case R.id.bbcode_code:
                insertBBCode(BBCODE.CODE);
                break;
            case R.id.submit_button:
                postReply();
                break;
            case R.id.discard:
                deleteReply();
                getActivity().setResult(RESULT_CANCELLED);
                leave();
                break;
            case R.id.save_draft:
                saveReply();
                getActivity().setResult(RESULT_CANCELLED);
                leave();
                break;
            case R.id.emotes:
                selectionStart = mMessage.getSelectionStart();
                new EmoteFragment(this).show(getFragmentManager(), "emotes");
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

    public void insertBBCode(BBCODE code) {
        if (selectionStart < 0) {//we might be getting this from an earlier point
            selectionStart = mMessage.getSelectionStart();
            selectionEnd = mMessage.getSelectionEnd();
        }
        boolean highlighted = selectionStart != selectionEnd;
        String startTag = null;
        String endTag = null;
        switch (code) {
            case BOLD:
                startTag = "[b]";
                endTag = "[/b]";
                break;
            case ITALICS:
                startTag = "[i]";
                endTag = "[/i]";
                break;
            case UNDERLINE:
                startTag = "[u]";
                endTag = "[/u]";
                break;
            case STRIKEOUT:
                startTag = "[s]";
                endTag = "[/s]";
                break;
            case URL:
                String link = this.getClipboardLink();
                if (link != null) {
                    startTag = "[url=" + link + "]";
                } else {
                    startTag = "[url]";
                }
                //startTag = "[url]";
                endTag = "[/url]";
                break;
            case QUOTE:
                startTag = "[quote]";
                endTag = "[/quote]";
                break;
            case IMAGE:
                String imageLink = this.getClipboardLink();
                if (imageLink != null) {
                    startTag = "[img]" + imageLink;
                } else {
                    startTag = "[img]";
                }
                endTag = "[/img]";
                break;
            case THUMBNAIL:
                String timageLink = this.getClipboardLink();
                if (timageLink != null) {
                    startTag = "[timg]" + timageLink;
                } else {
                    startTag = "[timg]";
                }
                endTag = "[/timg]";
                break;
            case VIDEO:
                startTag = "[video]";
                endTag = "[/video]";
                break;
            case SPOILER:
                startTag = "[spoiler]";
                endTag = "[/spoiler]";
                break;
            case CODE:
                startTag = "[code]";
                endTag = "[/code]";
                break;
        }
        if (startTag != null && endTag != null) {
            if (highlighted) {
                mMessage.getEditableText().insert(selectionStart, startTag);
                mMessage.getEditableText().insert(selectionEnd + startTag.length(), endTag);
                mMessage.setSelection(selectionStart + startTag.length());
            } else {
                mMessage.getEditableText().insert(selectionStart, startTag + endTag);
                mMessage.setSelection(selectionStart + startTag.length());
            }
        }
        selectionStart = -1;//reset them for next time
        selectionEnd = -1;
    }

    private void insertEmote(String emote) {
        selectionStart = mMessage.getSelectionStart();
        mMessage.getEditableText().insert(selectionStart, emote);
        mMessage.setSelection(selectionStart + emote.length());
        selectionStart = -1;//reset them for next time
        selectionEnd = -1;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "onDestroyView");
        autosave();
        getLoaderManager().destroyLoader(Constants.REPLY_LOADER_ID);
        getLoaderManager().destroyLoader(Constants.MISC_LOADER_ID);
        getActivity().getContentResolver().unregisterContentObserver(mThreadObserver);
        mMessage = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        cleanupTasks();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.e(TAG, "onDetach");
    }

    private void cleanupTasks() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    private void postReply() {
        new AlertDialog.Builder(getActivity())
                .setTitle((mReplyType == AwfulMessage.TYPE_EDIT) ? "Confirm Edit?" : "Confirm Post?")
                .setPositiveButton(R.string.post_reply,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface aDialog, int aWhich) {
                                if (mDialog == null && getActivity() != null) {
                                    mDialog = ProgressDialog.show(getActivity(), "Posting", "Hopefully it didn't suck...", true, true);
                                }
                                saveReply();
                                sendPost();
                            }
                        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface aDialog, int aWhich) {
                    }
                })
                .show();
    }

    private void sendPost() {
        if (replyData == null || replyData.getAsInteger(AwfulMessage.ID) == null) {
            // TODO: if this ever happens, the ID never gets set (and causes an NPE in SendPostRequest) - handle this in a better way?
            // Could use the mThreadId value, but that might be incorrect at this point and post to the wrong thread? Is null reply data an exceptional event?
            Log.e(TAG, "No reply data in sendPost() - no thread ID to post to!");
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity, "Unknown thread ID - can't post!", Toast.LENGTH_LONG).show();
            }
            return;
        }
        ContentValues cv = new ContentValues(replyData);
        String content = mMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            if (mDialog != null) {
                mDialog.dismiss();
            }
            displayAlert(R.string.message_empty, R.string.message_empty_subtext, 0);
            return;
        }
        if (!TextUtils.isEmpty(mFileAttachment)) {
            cv.put(AwfulMessage.REPLY_ATTACHMENT, mFileAttachment);
        }
        if (postSignature) {
            cv.put(AwfulMessage.REPLY_SIGNATURE, Constants.YES);
            System.out.println(AwfulMessage.REPLY_SIGNATURE + " " + Constants.YES);
        }
        if (disableEmots) {
            cv.put(AwfulMessage.REPLY_DISABLE_SMILIES, Constants.YES);
            System.out.println(AwfulMessage.REPLY_DISABLE_SMILIES + " " + Constants.YES);
        }
        cv.put(AwfulMessage.REPLY_CONTENT, content);
        AwfulRequest.AwfulResultCallback<Void> postCallback = new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                }
                sendSuccessful = true;
                mContentResolver.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID + "=?", AwfulProvider.int2StrArray(mThreadId));

                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, activity.getString(R.string.post_sent), Toast.LENGTH_LONG).show();
                    if (mReplyType == AwfulMessage.TYPE_EDIT) {
                        activity.setResult(mPostId);
                    } else {
                        activity.setResult(RESULT_POSTED);
                    }
                }
                leave();
            }

            @Override
            public void failure(VolleyError error) {
                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                }
                saveReply();
            }
        };
        switch (mReplyType) {
            case AwfulMessage.TYPE_QUOTE:
            case AwfulMessage.TYPE_NEW_REPLY:
                queueRequest(new SendPostRequest(getActivity(), cv).build(this, postCallback));
                break;
            case AwfulMessage.TYPE_EDIT:
                queueRequest(new SendEditRequest(getActivity(), cv).build(this, postCallback));
                break;
            default:
                getActivity().finish();
        }
    }

    private void deleteReply() {
        mContentResolver.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID + "=?", AwfulProvider.int2StrArray(mThreadId));
        mContentResolver.notifyChange(AwfulThread.CONTENT_URI, null);
        sendSuccessful = true;
    }

    private void saveReply() {
        if (getActivity() != null && mThreadId > 0 && mMessage != null) {
            String content = mMessage.getText().toString().trim();
            Log.e(TAG, "Saving reply! " + content);
            if (content.length() > 0) {
                ContentValues post;
                if(replyData == null){
                    post = new ContentValues();
                } else {
                    post = new ContentValues(replyData);
                }
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

    private void refreshLoader() {
        restartLoader(Constants.REPLY_LOADER_ID, null, mReplyDataCallback);
    }

    private void refreshThreadInfo() {
        restartLoader(Constants.MISC_LOADER_ID, null, mThreadLoaderCallback);
    }

    @Override
    public void onPageVisible() {

    }

    @Override
    public void onPageHidden() {
        autosave();
        if (getActivity() != null && mMessage != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mMessage.getApplicationWindowToken(), 0);
        }
    }

    @Override
    public String getTitle() {
        String title = "";
        if (mThreadTitle != null && mThreadTitle.length() > 0) {
            title = " - " + mThreadTitle;
        }
        switch (mReplyType) {
            case AwfulMessage.TYPE_EDIT:
                return "Editing" + title;
            case AwfulMessage.TYPE_NEW_REPLY:
                return "Reply" + title;
            case AwfulMessage.TYPE_QUOTE:
                return "Quote" + title;
        }
        return "Loading";
    }

    public void selectEmote(String contents) {
        insertEmote(contents);
    }

    @Override
    public String getInternalId() {
        return TAG;
    }

    @Override
    public boolean volumeScroll(KeyEvent event) {
        //I don't think that's necessary
        return false;
    }

    private String getClipboardLink() {
        String link = null;

        android.content.ClipboardManager cb = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        String copy = String.valueOf(cb.getText());
        if (copy.startsWith("http://") || copy.startsWith("https://")) {
            link = copy;
        }

        return link;
    }

    private enum BBCODE {BOLD, ITALICS, UNDERLINE, STRIKEOUT, URL, VIDEO, IMAGE, QUOTE, SPOILER, CODE, THUMBNAIL}

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
            if (aData != null && !aData.isClosed() && aData.moveToFirst()) {
                draftReplyType = aData.getInt(aData.getColumnIndex(AwfulMessage.TYPE));
                int postId = aData.getInt(aData.getColumnIndex(AwfulPost.EDIT_POST_ID));
                if ((draftReplyType == AwfulMessage.TYPE_EDIT && postId != mPostId) || draftReplyType != AwfulMessage.TYPE_EDIT && mReplyType == AwfulMessage.TYPE_EDIT) {
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
                setTitle(getTitle());
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }
    }

    private class ThreadContentObserver extends ContentObserver {
        public ThreadContentObserver(Handler aHandler) {
            super(aHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if(DEBUG) Log.v(TAG, "Thread Data update.");
            refreshThreadInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
}
