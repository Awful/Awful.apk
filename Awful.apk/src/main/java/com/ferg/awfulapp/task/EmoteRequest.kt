package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.FUNCTION_MISC
import com.ferg.awfulapp.constants.Constants.PARAM_ACTION
import com.ferg.awfulapp.thread.AwfulEmote
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * Request the current set of site emotes, updating the local database.
 */
class EmoteRequest(context: Context) : AwfulRequest<Void?>(context, FUNCTION_MISC) {

    init {
        parameters.add(PARAM_ACTION, "showsmilies")
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        val emotes = AwfulEmote.parseEmotes(doc)
        val inserted = contentResolver.bulkInsert(AwfulEmote.CONTENT_URI, emotes.toTypedArray())
        if (inserted < 0) throw AwfulError("Inserted $inserted emotes")
        return null
    }

}
