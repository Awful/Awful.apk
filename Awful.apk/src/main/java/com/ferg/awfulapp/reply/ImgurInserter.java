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
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
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

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

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

    @BindView(R.id.upload_url_text_input_layout)
    TextInputLayout uploadUrlTextWrapper;
    @BindView(R.id.upload_url_edittext)
    EditText uploadUrlEditText;

    @BindView(R.id.use_thumbnail)
    CheckBox thumbnailCheckbox;
    @BindView(R.id.add_gifs_as_video)
    CheckBox gifsAsVideoCheckbox;

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
                .setTitle(R.string.imgur_uploader_dialog_title)
                .setView(layout)
                .setPositiveButton(R.string.imgur_uploader_ok_button, null)
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
        creditsResetTime.setText(resetTime == null ? "" : getString(R.string.imgur_uploader_remaining_uploads_reset_time, timeFormat.format(resetTime), dateFormat.format(resetTime)));

        if (remaining == null) {
            remainingUploads.setText(R.string.imgur_uploader_remaining_uploads_unknown);
            creditsResetTime.setText("");
        } else {
            remainingUploads.setText(getResources().getQuantityString(R.plurals.imgur_uploader_remaining_uploads, remaining, remaining));
            if (remaining < 1) {
                setState(State.NO_UPLOAD_CREDITS);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Choosing an image file
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Display an image chooser to pick a file to upload.
     */
    @OnClick(R.id.upload_image_section)
    void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*");
        Intent chooser = Intent.createChooser(intent, getString(R.string.imgur_uploader_file_chooser_title));
        startActivityForResult(chooser, IMGUR_IMAGE_PICKER);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMGUR_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
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
        // check if this image looks invalid - if so, complain instead of using it
        String invalidReason = reasonImageIsInvalid(imageUri);
        if (invalidReason != null) {
            uploadStatus.setText(invalidReason);
            return;
        }

        // looks ok, so proceed with it
        setState(State.READY_TO_UPLOAD);
        imageFile = imageUri;
        displayImageDetails(imageUri);
        displayImagePreview(imageUri);
    }


    /**
     * Try to ascertain if an image is invalid.
     * <p>
     * This attempts to do some checks, e.g. file size, to determine if an image is <b>definitely</b>
     * invalid. It's possible that some checks can't be performed (e.g. if data isn't available),
     * so a value of <i>false</i> doesn't necessarily mean the image <b>is</b> valid.
     */
    @Nullable
    private String reasonImageIsInvalid(@NonNull Uri imageUri) {
        long maxUploadSize = 10L * 1024 * 1024; // 10MB limit
        Long imageSizeBytes = getFileNameAndSize(imageUri).second;
        if (imageSizeBytes != null && imageSizeBytes > maxUploadSize) {
            String fullFileSize = Formatter.formatFileSize(getContext(), imageSizeBytes);
            return getString(R.string.imgur_uploader_error_image_too_large, fullFileSize);
        }
        // haven't hit any obvious issues, so it's not invalid (as far as we can tell)
        return null;
    }


    /**
     * Get the name and size of a file, if possible.
     *
     * @return a [name, size] pair, where attributes are <b>null</b> if no data was available for them
     */
    @NonNull
    private Pair<String, Long> getFileNameAndSize(@NonNull Uri fileUri) {
        // TODO: 17/07/2017 this could be pulled out somewhere and used for this and attachment handling
        Cursor cursor = null;
        try {
            cursor = getActivity().getContentResolver().query(fileUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                Long size;
                try {
                    size = Long.parseLong(cursor.getString(cursor.getColumnIndex(OpenableColumns.SIZE)));
                } catch (NumberFormatException e) {
                    size = null;
                }
                return new Pair<>(name, size);
            } else {
                // no data for this Uri
                return new Pair<>(null, null);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    /**
     * Display file information for an image, if possible.
     */
    private void displayImageDetails(@NonNull Uri imageUri) {
        Pair<String, Long> nameAndSize = getFileNameAndSize(imageUri);
        if (nameAndSize.first == null && nameAndSize.second == null) {
            imageNameLabel.setText("");
            imageDetailsLabel.setText(R.string.imgur_uploader_no_file_details);
        } else {
            String name = (nameAndSize.first == null) ? getString(R.string.imgur_uploader_unknown_value) : nameAndSize.first;
            imageNameLabel.setText(getString(R.string.imgur_uploader_file_name, name));
            String size = (nameAndSize.second == null) ? getString(R.string.imgur_uploader_unknown_value) : Formatter.formatShortFileSize(getContext(), nameAndSize.second);
            imageDetailsLabel.setText(getString(R.string.imgur_uploader_file_size, size));
        }
    }


    /**
     * Display a preview thumbnail for an image.
     */
    private void displayImagePreview(@NonNull Uri imageUri) {
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
            setState(State.READY_TO_UPLOAD);
        }

        // if there's some text, do some validation warning checks
        if (urlIsEmpty) {
            return;
        }

        String url = uploadUrlEditText.getText().toString().toLowerCase();
        boolean looksLikeUrl = Patterns.WEB_URL.matcher(url).matches();
        String warningMessage = null;
        if (!StringUtils.startsWithAny(url, "http://", "https://")) {
            // TODO: 17/07/2017 uploading without these will fail - should really disable the button, or implicitly add a prefix (but then we have to guess which is valid...)
            warningMessage = getString(R.string.imgur_uploader_url_prefix_warning);
        } else if (!looksLikeUrl) {
            warningMessage = getString(R.string.imgur_uploader_url_validation_warning);
        }
        uploadUrlTextWrapper.setError(warningMessage);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Upload request and normal response handling
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Attempt to start an upload for the current image source, cancelling any upload in progress.
     */
    void startUpload() {
        if (state != State.READY_TO_UPLOAD) {
            return;
        }
        setState(State.UPLOADING);
        cancelUploadTask();

        // do a url if we have one
        if (uploadSourceIsUrl) {
            uploadTask = new ImgurUploadRequest(uploadUrlEditText.getText().toString(), this::parseUploadResponse, this::handleUploadError);
            NetworkUtils.queueRequest(uploadTask);
        } else {
            ContentResolver contentResolver = getActivity().getContentResolver();
            if (contentResolver != null) {
                try {
                    InputStream inputStream = contentResolver.openInputStream(imageFile);
                    if (inputStream != null) {
                        uploadTask = new ImgurUploadRequest(inputStream, this::parseUploadResponse, this::handleUploadError);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (uploadTask == null) {
                onUploadError(getString(R.string.imgur_uploader_error_file_access));
            } else {
                NetworkUtils.queueRequest(uploadTask);
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
            if (success) {
                if (previewBitmap != null) {
                    previewBitmap.recycle();
                }
                JSONObject data = response.getJSONObject("data");
                String videoUrl = StringUtils.defaultIfBlank(data.optString("gifv"), data.optString("mp4"));
                String imageUrl = data.getString("link");
                if (gifsAsVideoCheckbox.isChecked() && StringUtils.isNotBlank(videoUrl)) {
                    ((MessageComposer) getTargetFragment()).onHtml5VideoUploaded(videoUrl);
                } else {
                    ((MessageComposer) getTargetFragment()).onImageUploaded(imageUrl, thumbnailCheckbox.isChecked());
                }
                dismiss();
                return;
            }
            // no success? Guess it's an error then...?
            onUploadError(getErrorMessageFromResponseData(response));
        } catch (JSONException e) {
            onUploadError(getString(R.string.imgur_uploader_error_site_response_unrecognised));
            Log.w(TAG, "parseUploadResponse: failed to parse Imgur response, unexpected structure?", e);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Error handling
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Display an error message and fall back to the 'ready to upload' state.
     */
    private void onUploadError(@NonNull String errorMessage) {
        // revert back to pre-upload state
        setState(State.READY_TO_UPLOAD);
        uploadStatus.setText(errorMessage);
        updateRemainingUploads();
    }


    /**
     * Handle network errors and Imgur-specific errors from an upload request.
     */
    private void handleUploadError(VolleyError error) {
        Log.d(TAG, "Network error: " + error.getMessage(), error.getCause());
        // try and parse out some Imgur-specific error details from the response, and display their error message
        JSONObject responseData = null;
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                responseData = new JSONObject(new String(error.networkResponse.data, "UTF-8"));
            } catch (UnsupportedEncodingException | JSONException e) {
                Log.w(TAG, "handleUploadError: couldn't convert response data to JSON\n", e);
            }
        }
        onUploadError(getErrorMessageFromResponseData(responseData));
    }


    /**
     * Attempt to extract an error message from an Imgur response's JSON.
     *
     * @param responseData the returned JSON, or null to get a default error message
     */
    @NonNull
    private String getErrorMessageFromResponseData(@Nullable JSONObject responseData) {
        if (responseData != null) {
            try {
                // thanks for the inconsistent JSON structure for various errors guys - "error" is either a string or a bunch of data with a "message" string
                JSONObject errorData = responseData.getJSONObject("data");
                JSONObject errorObject = errorData.optJSONObject("error");
                return (errorObject != null) ? errorObject.getString("message") : errorData.getString("error");
            } catch (JSONException e) {
                Log.w(TAG, "getErrorMessageFromResponseData: failed to parse error response correctly\n" + responseData, e);
            }
        }
        // generic message for null/bad data
        return getString(R.string.imgur_uploader_error_upload_generic);
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
                    uploadUrlTextWrapper.setVisibility(VISIBLE);
                    uploadImageSection.setVisibility(GONE);
                } else {
                    uploadImageSection.setVisibility(VISIBLE);
                    uploadUrlTextWrapper.setVisibility(GONE);
                }
                imagePreview.setImageResource(R.drawable.ic_photo_dark);
                imageNameLabel.setText("");
                imageDetailsLabel.setText(R.string.imgur_uploader_tap_to_choose_file);
                uploadUrlEditText.setText("");
                uploadUrlTextWrapper.setError(null);

                uploadButton.setEnabled(false);
                uploadStatus.setText(uploadSourceIsUrl ? getString(R.string.imgur_uploader_status_enter_image_url) : getString(R.string.imgur_uploader_status_choose_source_file));
                uploadProgressBar.setVisibility(GONE);
                break;

            // upload source selected (either a URL entered, or a source file chosen)
            case READY_TO_UPLOAD:
                uploadButton.setEnabled(true);
                uploadStatus.setText(R.string.imgur_uploader_status_ready_to_upload);
                uploadProgressBar.setVisibility(GONE);
                break;

            // upload request in progress
            case UPLOADING:
                uploadButton.setEnabled(false);
                uploadStatus.setText(R.string.imgur_uploader_status_upload_in_progress);
                uploadProgressBar.setVisibility(VISIBLE);
                break;

            // error state for when we can't upload
            case NO_UPLOAD_CREDITS:
                // put on the brakes, hide everything and prevent uploads
                uploadButton.setEnabled(false);
                uploadStatus.setText(R.string.imgur_uploader_status_no_remaining_uploads);
                uploadImageSection.setVisibility(GONE);
                uploadUrlTextWrapper.setVisibility(GONE);
                uploadProgressBar.setVisibility(GONE);
                break;
        }
    }

    private enum State {CHOOSING, READY_TO_UPLOAD, UPLOADING, NO_UPLOAD_CREDITS}

}
