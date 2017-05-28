package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.task.ImgurUploadRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.OnTextChanged;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static butterknife.ButterKnife.bind;

/**
 * Created by baka kaba on 31/05/2017.
 * <p>
 * A dialog that allows the user to host an image on Imgur, and inserts the resulting BBcode.
 * <p>
 * The user can choose an upload type (an image file, or the URL of an image already elsewhere on
 * the internet), add their source, and pick any relevant options. If the upload is successful, the
 * image is inserted as BBcode. Use {@link DialogFragment#setTargetFragment(Fragment, int)} to pass
 * the {@link MessageComposer} where the code will be inserted.
 */
public class ImgurInserter extends DialogFragment {

    public static final String TAG = "ImgurInserter";
    private static final int IMGUR_IMAGE_PICKER = 3452;

    private java.text.DateFormat dateFormat;
    private java.text.DateFormat timeFormat;

    @BindView(R.id.upload_type)
    Spinner uploadTypeSelector;

    @BindView(R.id.upload_image_section)
    ViewGroup uploadImageSection;
    @BindView(R.id.image_preview)
    ImageView imagePreview;
    @BindView(R.id.image_name)
    TextView imageNameLabel;
    @BindView(R.id.image_details)
    TextView imageDetailsLabel;

    @BindView(R.id.upload_url_edittext)
    EditText uploadUrlEditText;

    @BindView(R.id.use_thumbnail)
    CheckBox thumbnailCheckbox;

    @BindView(R.id.upload_status)
    TextView uploadStatus;
    @BindView(R.id.upload_progress_bar)
    ProgressBar uploadProgressBar;
    @BindView(R.id.remaining_uploads)
    TextView remainingUploads;
    @BindView(R.id.credits_reset_time)
    TextView creditsResetTime;

    private Button uploadButton;

    Uri imageFile = null;
    Bitmap previewBitmap = null;
    Request uploadTask = null;
    State state;
    boolean uploadSourceIsUrl;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        dateFormat = DateFormat.getDateFormat(activity);
        timeFormat = DateFormat.getTimeFormat(activity);

        View layout = activity.getLayoutInflater().inflate(R.layout.insert_imgur_dialog, null);
        bind(this, layout);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Upload and insert Imgur")
                .setView(layout)
                .setPositiveButton("Upload", null)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dismiss())
                .show();
        // get the dialog's 'upload' positive button so we can enable and disable it
        // setting the click listener directly prevents the dialog from dismissing, so the upload can run
        uploadButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        uploadButton.setOnClickListener(view -> startUpload());
        // TODO: 05/06/2017 is that method guaranteed to be fired when the system creates the spinner and sets the first item?
        updateUploadType();
        updateRemainingUploads();
        return dialog;
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.d(TAG, "onDismiss: stopping upload task");
        cancelUploadTask();
        super.onDismiss(dialog);
    }


    /**
     * Cancel any currently running upload.
     */
    private void cancelUploadTask() {
        if (uploadTask != null) {
            uploadTask.cancel();
        }
        uploadTask = null;
    }


    /**
     * Check the currently selected upload type, and update state as necessary.
     */
    @OnItemSelected(R.id.upload_type)
    void updateUploadType() {
        // this assumes the first entry in the spinner is URL, and the second is IMAGE
        int position = uploadTypeSelector.getSelectedItemPosition();
        uploadSourceIsUrl = (position == 0);
        setState(State.CHOOSING);
    }


    /**
     * Check the number of uploads the user can perform, updating state as necessary.
     */
    void updateRemainingUploads() {
        Pair<Integer, Long> uploadLimit = ImgurUploadRequest.getCurrentUploadLimit();
        Integer remaining = uploadLimit.first;
        Long resetTime = uploadLimit.second;
        creditsResetTime.setText(resetTime == null ? "" : "Resets at " + timeFormat.format(resetTime) + " on " + dateFormat.format(resetTime));

        if (remaining == null) {
            remainingUploads.setText("Unknown upload availability");
            creditsResetTime.setText("");
        } else if (remaining > 0) {
            remainingUploads.setText(String.format("%d upload%s left", remaining, (remaining > 1) ? "s" : ""));
        } else {
            setState(State.NO_UPLOAD_CREDITS);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Choosing an image
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Display an image chooser to pick a file to upload.
     */
    @OnClick(R.id.upload_image_section)
    void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*");
        Intent chooser = Intent.createChooser(intent, "CHOOSE YOU ARE DESTINY");
        startActivityForResult(chooser, IMGUR_IMAGE_PICKER);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMGUR_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
            // TODO: 29/05/2017 FAILED?
            if (data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    onImageSelected(imageUri);
                }
            }
        }
    }


    /**
     * Handle a newly selected image source, displaying a preview and updating state.
     */
    private void onImageSelected(@NonNull Uri imageUri) {
        setState(State.READY_TO_UPLOAD);
        imageFile = imageUri;
        displayData(imageUri);
        displayPreview(imageUri);
    }


    /**
     * Display file information for an image, if possible.
     */
    private void displayData(@NonNull Uri imageUri) {
        Cursor cursor = getActivity().getContentResolver().query(imageUri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                String size;
                try {
                    long bytes = Long.parseLong(cursor.getString(sizeIndex));
                    size = Formatter.formatShortFileSize(getContext(), bytes);
                } catch (NumberFormatException e) {
                    size = "unknown";
                }
                imageNameLabel.setText("Name: " + name);
                imageDetailsLabel.setText("Size: " + size);
            } else {
                imageNameLabel.setText("");
                imageDetailsLabel.setText("Can't get file details");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    /**
     * Display a preview thumbnail an image.
     */
    private void displayPreview(Uri imageUri) {
        if (previewBitmap != null) {
            previewBitmap.recycle();
        }
        InputStream inputStream;
        try {
            // TODO: 30/05/2017 non-bad image preview
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            inputStream = getActivity().getContentResolver().openInputStream(imageUri);
            previewBitmap = BitmapFactory.decodeStream(inputStream, null, options);
            imagePreview.setImageDrawable(new BitmapDrawable(previewBitmap));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // TODO: 05/06/2017 'no preview' or something?
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Entering a URL
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Handle changes to the 'image source URL' field, updating state where necessary.
     */
    @OnTextChanged(R.id.upload_url_edittext)
    void onUrlTextChanged() {
        // change state when (and only when) the url contents no longer match the current state
        // this also avoids a circular call when the url is reset in #setState (url becomes empty
        // but state is already set appropriately to CHOOSING)
        boolean urlIsEmpty = uploadUrlEditText.length() == 0;
        if (urlIsEmpty && state == State.READY_TO_UPLOAD) {
            setState(State.CHOOSING);
        } else if (!urlIsEmpty && state == State.CHOOSING) {
            // TODO: 01/06/2017 'looks like a URL' check?
            setState(State.READY_TO_UPLOAD);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Upload request and response handling
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Attempt to start an upload for the current image source, cancelling any upload in progress.
     */
    void startUpload() {
        // TODO: 28/05/2017 rate limiting somewhere, especially for repeated attempts with the same image?
        if (state == State.READY_TO_UPLOAD) {
            setState(State.UPLOADING);
            cancelUploadTask();

            // do a url if we have one
            if (uploadSourceIsUrl) {
                uploadTask = new ImgurUploadRequest(uploadUrlEditText.getText().toString(), this::parseUploadResponse, this::handleUploadError);
            } else {
                ContentResolver contentResolver = getActivity().getContentResolver();
                if (contentResolver != null) {
                    try {
                        InputStream inputStream = contentResolver.openInputStream(imageFile);
                        uploadTask = new ImgurUploadRequest(inputStream, this::parseUploadResponse, this::handleUploadError);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

            // queue the request if we were able to make it
            if (uploadTask != null) {
                NetworkUtils.queueRequest(uploadTask);
            } else {
                onUploadError("Error accessing file or something");
            }
        }
    }


    /**
     * Parse and handle the response from the Imgur API.
     * <p>
     * This checks for a successful result, pulls out the hosted image's URL and inserts it into
     * the target fragment (which should be a {@link MessageComposer}). If the upload failed,
     * the error state is handled.
     *
     * @see <a href="https://apidocs.imgur.com/">https://apidocs.imgur.com/</a>
     */
    private void parseUploadResponse(JSONObject response) {
        try {
            boolean success = response.getBoolean("success");
            String url = response.getJSONObject("data").getString("link");
            if (success) {
                if (previewBitmap != null) {
                    previewBitmap.recycle();
                }
                ((MessageComposer) getTargetFragment()).onImageUploaded(url, thumbnailCheckbox.isChecked());
                dismiss();
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // fall-through error case
        onUploadError("Got a response, couldn't parse it though!");
    }


    private void handleUploadError(VolleyError error) {
        // TODO: 01/06/2017 better error messages (here and elsewhere e.g. inputstream-getting) and blocking repeated attempts
        Log.d(TAG, "Oh no an error: " + error.getMessage(), error.getCause());
        onUploadError("Volley error!");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Error handling
    ///////////////////////////////////////////////////////////////////////////

    /*
        ERRORS:
         can't access image (can't get inputstream etc)
         invalid url?
         no credits?
         response errors
     */

    /**
     * Handle errors by displaying an error message and falling back to the 'ready to upload' state.
     */
    private void onUploadError(@NonNull String errorMessage) {
        // revert back to pre-upload state
        setState(State.READY_TO_UPLOAD);
        uploadStatus.setText(errorMessage);
        updateRemainingUploads();
    }


    ///////////////////////////////////////////////////////////////////////////
    // State transitions
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Move to a new state, and update the UI as appropriate.
     * <p>
     * This method defines the different states that the dialog can be in, hiding/showing and
     * enabling/disabling UI elements to move between states and limit what the user can do at each
     * stage.
     */
    private void setState(State newState) {
        if (state == State.NO_UPLOAD_CREDITS) {
            // make this state permanent - if we hit it, no moving back to CHOOSING etc
            return;
        }
        state = newState;
        switch (state) {
            // initial state, choosing an upload source
            case CHOOSING:
                // this intentionally sets the appearing view to visible BEFORE removing the other
                // which avoids too much weirdness with the layout change animation
                if (uploadSourceIsUrl) {
                    uploadUrlEditText.setVisibility(VISIBLE);
                    uploadImageSection.setVisibility(GONE);
                } else {
                    uploadImageSection.setVisibility(VISIBLE);
                    uploadUrlEditText.setVisibility(GONE);
                }
                uploadImageSection.setVisibility(uploadSourceIsUrl ? GONE : VISIBLE);
                uploadUrlEditText.setVisibility(uploadSourceIsUrl ? VISIBLE : GONE);
                imagePreview.setImageResource(R.drawable.ic_photo_dark);
                imageNameLabel.setText("");
                imageDetailsLabel.setText("Tap to select an image");
                uploadUrlEditText.setText("");

                uploadButton.setEnabled(false);
                uploadStatus.setText(uploadSourceIsUrl ? "Enter an image URL above" : "Select an image above");
                uploadProgressBar.setIndeterminate(false);
                uploadProgressBar.setProgress(0);
                break;

            // upload source selected (either a URL entered, or a source file chosen)
            case READY_TO_UPLOAD:
                uploadButton.setEnabled(true);
                uploadStatus.setText("Ready to upload");
                uploadProgressBar.setIndeterminate(false);
                uploadProgressBar.setProgress(0);
                break;

            // upload request in progress
            case UPLOADING:
                uploadButton.setEnabled(false);
                uploadStatus.setText("Uploading...");
                // TODO: 27/06/2017 implement actual upload progress bar for files?
                uploadProgressBar.setIndeterminate(true);
                uploadProgressBar.setProgress(50);
                break;

            // error state for when we can't upload
            case NO_UPLOAD_CREDITS:
                // put on the brakes, hide everything and prevent uploads
                uploadButton.setEnabled(false);
                uploadStatus.setText("No uploads remaining!");
                uploadImageSection.setVisibility(GONE);
                uploadUrlEditText.setVisibility(GONE);
                uploadProgressBar.setIndeterminate(false);
                uploadProgressBar.setProgress(0);
                break;
        }
    }

    private enum State {CHOOSING, READY_TO_UPLOAD, UPLOADING, NO_UPLOAD_CREDITS}

}
