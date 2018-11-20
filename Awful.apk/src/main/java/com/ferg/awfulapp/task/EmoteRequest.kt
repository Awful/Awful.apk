package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.thread.AwfulEmote
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * Created by matt on 8/8/13.
 */
class EmoteRequest(context: Context) : AwfulRequest<Void?>(context, Constants.FUNCTION_MISC) {

    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        with(urlBuilder!!) {
            appendQueryParameter(Constants.PARAM_ACTION, "showsmilies")
            return build().toString()
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        val emotes = AwfulEmote.parseEmotes(doc)
        val inserted = contentResolver.bulkInsert(AwfulEmote.CONTENT_URI, emotes.toTypedArray())
        if (inserted < 0) throw AwfulError("Inserted $inserted emotes")
        return null
    }

}
