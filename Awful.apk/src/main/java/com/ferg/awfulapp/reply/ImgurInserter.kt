package com.ferg.awfulapp.reply

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.text.format.Formatter
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.volley.Request
import com.android.volley.VolleyError
import com.ferg.awfulapp.AwfulApplication.Companion.appStatePrefs
import com.ferg.awfulapp.R
import com.ferg.awfulapp.databinding.InsertImgurDialogBinding
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.task.ImgurUploadRequest
import com.ferg.awfulapp.task.ImgurUploadRequest.Companion.currentUploadLimit
import org.apache.commons.lang3.StringUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.text.DateFormat
import java.util.Locale
import androidx.core.content.edit

/**
 * Created by baka kaba on 31/05/2017.
 * 
 * 
 * A dialog that allows the user to host an image on Imgur, and inserts the resulting BBcode.
 * 
 * 
 * The user can choose an upload type (an image file, or the URL of an image already elsewhere on
 * the internet), add their source, and pick any relevant options. If the upload is successful, the
 * image is inserted as BBcode. Use [DialogFragment.setTargetFragment] to pass
 * the [MessageComposer] where the code will be inserted.
 */
class ImgurInserter : DialogFragment() {

    companion object {
        const val TAG: String = "ImgurInserter"
        private const val IMGUR_IMAGE_PICKER = 3452
        private const val KEY_IMGUR_LAST_CHOSEN_UPLOAD_OPTION = "imgur_last_chosen_upload_option"
    }
    private var dateFormat: DateFormat? = null
    private var timeFormat: DateFormat? = null

    private lateinit var binding: InsertImgurDialogBinding
    private lateinit var uploadButton: Button

    var imageFile: Uri? = null
    var previewBitmap: Bitmap? = null
    var uploadTask: Request<*>? = null
    var imgurState: State? = null
    var uploadSourceIsUrl: Boolean = false


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = getActivity()
        dateFormat = android.text.format.DateFormat.getDateFormat(activity)
        timeFormat = android.text.format.DateFormat.getTimeFormat(activity)

        val layout = requireActivity().layoutInflater.inflate(R.layout.insert_imgur_dialog, null)
        binding = InsertImgurDialogBinding.bind(layout)
        val dialog = AlertDialog.Builder(requireActivity())
            .setTitle(R.string.imgur_uploader_dialog_title)
            .setView(layout)
            .setPositiveButton(R.string.imgur_uploader_ok_button, null)
            .setNegativeButton(
                R.string.cancel
            ) { _: DialogInterface?, _: Int -> dismiss() }
            .show()
        // get the dialog's 'upload' positive button so we can enable and disable it
        // setting the click listener directly prevents the dialog from dismissing, so the upload can run
        uploadButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        uploadButton.setOnClickListener { _: View? -> startUpload() }
        // TODO: 05/06/2017 is that method guaranteed to be fired when the system creates the spinner and sets the first item?
        binding.uploadType.setSelection(
            appStatePrefs!!.getInt(
                KEY_IMGUR_LAST_CHOSEN_UPLOAD_OPTION,
                0
            )
        )
        updateUploadType()
        updateRemainingUploads()
        binding.uploadType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateUploadType()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.uploadImageSection.setOnClickListener { launchImagePicker() }
        binding.uploadUrlEdittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                onUrlTextChanged()
            }
        })
        return dialog
    }


    override fun onDismiss(dialog: DialogInterface) {
        Log.d(TAG, "onDismiss: stopping upload task")
        cancelUploadTask()
        super.onDismiss(dialog)
    }


    /**
     * Cancel any currently running upload.
     */
    private fun cancelUploadTask() {
        if (uploadTask != null) {
            uploadTask!!.cancel()
        }
        uploadTask = null
    }


    /**
     * Check the currently selected upload type, and update state as necessary.
     */
    fun updateUploadType() {
        // this assumes the first entry in the spinner is URL, and the second is IMAGE
        val position = binding.uploadType.selectedItemPosition
        uploadSourceIsUrl = (position == 0)
        setState(State.CHOOSING)
    }


    /**
     * Check the number of uploads the user can perform, updating state as necessary.
     */
    fun updateRemainingUploads() {
        val uploadLimit = currentUploadLimit
        val remaining = uploadLimit.component1()
        val resetTime = uploadLimit.component2()
        binding.creditsResetTime.text = if (resetTime == null) "" else getString(
            R.string.imgur_uploader_remaining_uploads_reset_time,
            timeFormat!!.format(resetTime),
            dateFormat!!.format(resetTime)
        )

        if (remaining == null) {
            binding.remainingUploads.setText(R.string.imgur_uploader_remaining_uploads_unknown)
            binding.creditsResetTime.text = ""
        } else {
            binding.remainingUploads.text = resources.getQuantityString(
                R.plurals.imgur_uploader_remaining_uploads,
                remaining,
                remaining
            )
            if (remaining < 1) {
                setState(State.NO_UPLOAD_CREDITS)
            }
        }
    }


    /////////////////////////////////////////////////////////////////////////
    // Choosing an image file
    /////////////////////////////////////////////////////////////////////////
    /**
    * Display an image chooser to pick a file to upload.
    */
    fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("image/*")
        val chooser =
            Intent.createChooser(intent, getString(R.string.imgur_uploader_file_chooser_title))
        startActivityForResult(chooser, IMGUR_IMAGE_PICKER)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IMGUR_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val imageUri = data.data
                if (imageUri != null) {
                    onImageSelected(imageUri)
                }
            }
        }
    }


    /**
     * Handle a newly selected image source, displaying a preview and updating state.
     */
    private fun onImageSelected(imageUri: Uri) {
        // check if this image looks invalid - if so, complain instead of using it
        val invalidReason = reasonImageIsInvalid(imageUri)
        if (invalidReason != null) {
            binding.uploadStatus.text = invalidReason
            return
        }

        // looks ok, so proceed with it
        setState(State.READY_TO_UPLOAD)
        imageFile = imageUri
        displayImageDetails(imageUri)
        displayImagePreview(imageUri)
    }


    /**
     * Try to ascertain if an image is invalid.
     * 
     * 
     * This attempts to do some checks, e.g. file size, to determine if an image is **definitely**
     * invalid. It's possible that some checks can't be performed (e.g. if data isn't available),
     * so a value of *false* doesn't necessarily mean the image **is** valid.
     */
    private fun reasonImageIsInvalid(imageUri: Uri): String? {
        val maxUploadSize = 10L * 1024 * 1024 // 10MB limit
        val imageSizeBytes = getFileNameAndSize(imageUri).component2()
        if (imageSizeBytes != null && imageSizeBytes > maxUploadSize) {
            val fullFileSize = Formatter.formatFileSize(context, imageSizeBytes)
            return getString(R.string.imgur_uploader_error_image_too_large, fullFileSize)
        }
        // haven't hit any obvious issues, so it's not invalid (as far as we can tell)
        return null
    }


    /**
     * Get the name and size of a file, if possible.
     * 
     * @return a [name, size] pair, where attributes are **null** if no data was available for them
     */
    private fun getFileNameAndSize(fileUri: Uri): Pair<String?, Long?> {
        // TODO: 17/07/2017 this could be pulled out somewhere and used for this and attachment handling
        val cursor: Cursor? = requireActivity().contentResolver.query(fileUri, null, null, null, null)
        cursor.use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val size: Long? = try {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)).toLong()
                } catch (e: NumberFormatException) {
                    null
                }
                return Pair<String?, Long?>(name, size)
            } else {
                // no data for this Uri
                return Pair(null, null)
            }
        }
    }


    /**
     * Display file information for an image, if possible.
     */
    private fun displayImageDetails(imageUri: Uri) {
        val nameAndSize = getFileNameAndSize(imageUri)
        if (nameAndSize.component1() == null && nameAndSize.component2() == null) {
            binding.imageName.text = ""
            binding.imageDetails.setText(R.string.imgur_uploader_no_file_details)
        } else {
            val name: String =
                (if (nameAndSize.component1() == null) getString(R.string.imgur_uploader_unknown_value) else nameAndSize.component1())!!
            binding.imageName.text = getString(R.string.imgur_uploader_file_name, name)
            val size =
                if (nameAndSize.component2() == null) getString(R.string.imgur_uploader_unknown_value) else Formatter.formatShortFileSize(
                    context,
                    nameAndSize.component2()!!
                )
            binding.imageDetails.text = getString(R.string.imgur_uploader_file_size, size)
        }
    }


    /**
     * Display a preview thumbnail for an image.
     */
    private fun displayImagePreview(imageUri: Uri) {
        if (previewBitmap != null) {
            previewBitmap!!.recycle()
        }
        val inputStream: InputStream?
        try {
            // TODO: 30/05/2017 non-bad image preview
            val options = BitmapFactory.Options()
            options.inSampleSize = 4
            inputStream = requireActivity().contentResolver.openInputStream(imageUri)
            previewBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            binding.imagePreview.setImageDrawable(BitmapDrawable(previewBitmap))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            // TODO: 05/06/2017 'no preview' or something?
        }
    }


    /////////////////////////////////////////////////////////////////////////
    // Entering a URL
    /////////////////////////////////////////////////////////////////////////
    /**
    * Handle changes to the 'image source URL' field, updating state where necessary.
    */
    fun onUrlTextChanged() {
        // change state when (and only when) the url contents no longer match the current state
        // this also avoids a circular call when the url is reset in #setState (url becomes empty
        // but state is already set appropriately to CHOOSING)
        val urlIsEmpty = binding.uploadUrlEdittext.length() == 0
        if (urlIsEmpty && imgurState == State.READY_TO_UPLOAD) {
            setState(State.CHOOSING)
        } else if (!urlIsEmpty && imgurState == State.CHOOSING) {
            setState(State.READY_TO_UPLOAD)
        }

        // if there's some text, do some validation warning checks
        if (urlIsEmpty) {
            return
        }

        val url = binding.uploadUrlEdittext.getText().toString().lowercase(Locale.getDefault())
        val looksLikeUrl = Patterns.WEB_URL.matcher(url).matches()
        var warningMessage: String? = null
        if (!StringUtils.startsWithAny(url, "http://", "https://")) {
            // TODO: 17/07/2017 uploading without these will fail - should really disable the button, or implicitly add a prefix (but then we have to guess which is valid...)
            warningMessage = getString(R.string.imgur_uploader_url_prefix_warning)
        } else if (!looksLikeUrl) {
            warningMessage = getString(R.string.imgur_uploader_url_validation_warning)
        }
        binding.uploadUrlTextInputLayout.error = warningMessage
    }


    /////////////////////////////////////////////////////////////////////////
    // Upload request and normal response handling
    /////////////////////////////////////////////////////////////////////////
    /**
    * Attempt to start an upload for the current image source, cancelling any upload in progress.
    */
    fun startUpload() {
        if (imgurState != State.READY_TO_UPLOAD) {
            return
        }
        appStatePrefs?.edit {
            putInt(
                KEY_IMGUR_LAST_CHOSEN_UPLOAD_OPTION,
                binding.uploadType.selectedItemPosition
            )
        }
        setState(State.UPLOADING)
        cancelUploadTask()

        // do a url if we have one
        if (uploadSourceIsUrl) {
            uploadTask = ImgurUploadRequest(
                binding.uploadUrlEdittext.getText().toString(),
                { response: JSONObject? -> this.parseUploadResponse(response!!) },
                { error: VolleyError? -> this.handleUploadError(error!!) })
            NetworkUtils.queueRequest(uploadTask!!)
        } else {
            val contentResolver = requireActivity().contentResolver
            if (contentResolver != null) {
                try {
                    val inputStream = contentResolver.openInputStream(imageFile!!)
                    if (inputStream != null) {
                        uploadTask = ImgurUploadRequest(
                            inputStream,
                            { response: JSONObject? ->
                                this.parseUploadResponse(
                                    response!!
                                )
                            },
                            { error: VolleyError? ->
                                this.handleUploadError(error!!)
                            })
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
            if (uploadTask == null) {
                onUploadError(getString(R.string.imgur_uploader_error_file_access))
            } else {
                NetworkUtils.queueRequest(uploadTask!!)
            }
        }
    }


    /**
     * Parse and handle the response from the Imgur API.
     * 
     * 
     * This checks for a successful result, pulls out the hosted image's URL and inserts it into
     * the target fragment (which should be a [MessageComposer]). If the upload failed,
     * the error state is handled.
     * 
     * @see [https://apidocs.imgur.com/](https://apidocs.imgur.com/)
     */
    private fun parseUploadResponse(response: JSONObject) {
        try {
            val success = response.getBoolean("success")
            if (success) {
                if (previewBitmap != null) {
                    previewBitmap!!.recycle()
                }
                val data = response.getJSONObject("data")
                val videoUrl = StringUtils.defaultIfBlank<String?>(
                    data.optString("gifv"),
                    data.optString("mp4")
                )
                val imageUrl = data.getString("link")
                if (binding.addGifsAsVideo.isChecked && StringUtils.isNotBlank(videoUrl)) {
                    (targetFragment as MessageComposer).onHtml5VideoUploaded(videoUrl!!)
                } else {
                    (targetFragment as MessageComposer).onImageUploaded(
                        imageUrl,
                        binding.useThumbnail.isChecked
                    )
                }
                dismiss()
                return
            }
            // no success? Guess it's an error then...?
            onUploadError(getErrorMessageFromResponseData(response))
        } catch (e: JSONException) {
            onUploadError(getString(R.string.imgur_uploader_error_site_response_unrecognised))
            Log.w(
                TAG,
                "parseUploadResponse: failed to parse Imgur response, unexpected structure?",
                e
            )
        }
    }


    /////////////////////////////////////////////////////////////////////////
    // Error handling
    /////////////////////////////////////////////////////////////////////////
    /**
    * Display an error message and fall back to the 'ready to upload' state.
    */
    private fun onUploadError(errorMessage: String) {
        // revert back to pre-upload state
        setState(State.READY_TO_UPLOAD)
        binding.uploadStatus.text = errorMessage
        updateRemainingUploads()
    }


    /**
     * Handle network errors and Imgur-specific errors from an upload request.
     */
    private fun handleUploadError(error: VolleyError) {
        Log.d(TAG, "Network error: " + error.message, error.cause)
        // try and parse out some Imgur-specific error details from the response, and display their error message
        var responseData: JSONObject? = null
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                responseData = JSONObject(String(error.networkResponse.data, charset("UTF-8")))
            } catch (e: UnsupportedEncodingException) {
                Log.w(TAG, "handleUploadError: couldn't convert response data to JSON\n", e)
            } catch (e: JSONException) {
                Log.w(TAG, "handleUploadError: couldn't convert response data to JSON\n", e)
            }
        }
        onUploadError(getErrorMessageFromResponseData(responseData))
    }


    /**
     * Attempt to extract an error message from an Imgur response's JSON.
     * 
     * @param responseData the returned JSON, or null to get a default error message
     */
    private fun getErrorMessageFromResponseData(responseData: JSONObject?): String {
        if (responseData != null) {
            try {
                // thanks for the inconsistent JSON structure for various errors guys - "error" is either a string or a bunch of data with a "message" string
                val errorData = responseData.getJSONObject("data")
                val errorObject = errorData.optJSONObject("error")
                return if (errorObject != null) errorObject.getString("message") else errorData.getString(
                    "error"
                )
            } catch (e: JSONException) {
                Log.w(
                    TAG,
                    "getErrorMessageFromResponseData: failed to parse error response correctly\n" + responseData,
                    e
                )
            }
        }
        // generic message for null/bad data
        return getString(R.string.imgur_uploader_error_upload_generic)
    }


    /////////////////////////////////////////////////////////////////////////
    // State transitions
    /////////////////////////////////////////////////////////////////////////
    /**
    * Move to a new state, and update the UI as appropriate.
    *
    *
    * This method defines the different states that the dialog can be in , hiding/showing and
    * enabling/disabling UI elements to move between states and limit what the user can do at each
    * stage.
    */
    private fun setState(newState: State) {
        if (imgurState == State.NO_UPLOAD_CREDITS) {
            // make this state permanent - if we hit it, no moving back to CHOOSING etc
            return
        }
        imgurState = newState
        when (imgurState) {
            State.CHOOSING -> {
                // this intentionally sets the appearing view to visible BEFORE removing the other
                // which avoids too much weirdness with the layout change animation
                if (uploadSourceIsUrl) {
                    binding.uploadUrlTextInputLayout.visibility = View.VISIBLE
                    binding.uploadImageSection.visibility = View.GONE
                } else {
                    binding.uploadImageSection.visibility = View.VISIBLE
                    binding.uploadUrlTextInputLayout.visibility = View.GONE
                }
                binding.imagePreview.setImageResource(R.drawable.ic_photo_dark)
                binding.imageName.text = ""
                binding.imageDetails.setText(R.string.imgur_uploader_tap_to_choose_file)
                binding.uploadUrlEdittext.setText("")
                binding.uploadUrlTextInputLayout.error = null

                uploadButton.isEnabled = false
                binding.uploadStatus.text = if (uploadSourceIsUrl) getString(R.string.imgur_uploader_status_enter_image_url) else getString(
                    R.string.imgur_uploader_status_choose_source_file
                )
                binding.uploadProgressBar.visibility = View.GONE
            }

            State.READY_TO_UPLOAD -> {
                uploadButton.isEnabled = true
                binding.uploadStatus.setText(R.string.imgur_uploader_status_ready_to_upload)
                binding.uploadProgressBar.visibility = View.GONE
            }

            State.UPLOADING -> {
                uploadButton.isEnabled = false
                binding.uploadStatus.setText(R.string.imgur_uploader_status_upload_in_progress)
                binding.uploadProgressBar.visibility = View.VISIBLE
            }

            State.NO_UPLOAD_CREDITS -> {
                // put on the brakes, hide everything and prevent uploads
                uploadButton.isEnabled = false
                binding.uploadStatus.setText(R.string.imgur_uploader_status_no_remaining_uploads)
                binding.uploadImageSection.visibility = View.GONE
                binding.uploadUrlTextInputLayout.visibility = View.GONE
                binding.uploadProgressBar.visibility = View.GONE
            }

            else -> {}
        }
    }

    enum class State {
        CHOOSING, READY_TO_UPLOAD, UPLOADING, NO_UPLOAD_CREDITS
    }
}
