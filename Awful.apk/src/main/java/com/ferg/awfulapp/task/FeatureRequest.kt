package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri

import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.util.AwfulError

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber

/**
 * An AwfulRequest that fetches the features active on the user's account (platinum etc.),
 * and stores that data in AwfulPreferences.
 */
class FeatureRequest(context: Context) : AwfulRequest<Void?>(context, Constants.FUNCTION_MEMBER) {

    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        return urlBuilder!!.appendQueryParameter(Constants.PARAM_ACTION, "accountfeatures").build().toString()
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        // grab the element containing the features info and validate its structure
        val features = doc.selectFirst(".features")?.select("dt")
        if (features == null) {
            throw AwfulError("Couldn't find features element")
        } else if (features.size != 3) {
            throw AwfulError("Unexpected number of feature elements (wanted 3, got ${features.size}")
        }

        mapOf(
                Keys.HAS_PLATINUM to features[0].enabled,
                Keys.HAS_ARCHIVES to features[1].enabled,
                Keys.HAS_NO_ADS to features[2].enabled
        ).forEach { (k, v) -> preferences.setPreference(k, v) }

        Timber.i("Updated account features\nPlatinum:${preferences.hasPlatinum} Archives:${preferences.hasArchives} NoAds:${preferences.hasNoAds}")
        return null
    }

    private val Element.enabled get() = this.hasClass("enabled")

}
