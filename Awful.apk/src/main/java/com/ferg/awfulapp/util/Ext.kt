package com.ferg.awfulapp.util

import android.net.Uri


//check URI type
fun Uri.isExternalStorageDocument() = this.authority == "com.android.externalstorage.documents"
fun Uri.isDownloadsDocument() = this.authority == "com.android.providers.downloads.documents"
fun Uri.isGooglePhotosUri() = this.authority == "com.google.android.apps.photos.content"
fun Uri.isMediaDocument() = this.authority == "com.android.providers.media.documents"
