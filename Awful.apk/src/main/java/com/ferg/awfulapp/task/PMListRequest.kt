package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.FUNCTION_PRIVATE_MESSAGE
import com.ferg.awfulapp.constants.Constants.PARAM_FOLDERID
import com.ferg.awfulapp.constants.Constants.PARAM_SHOWALL
import com.ferg.awfulapp.constants.Constants.PRIVATE_MESSAGE_DEFAULT_FOLDER
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * A request that gets and updates the stored list of Private Messages in a particular [folder]
 */
class PMListRequest(context: Context, private val folder: Int = PRIVATE_MESSAGE_DEFAULT_FOLDER, loadAll: Boolean = false)
    : AwfulRequest<Void?>(context, FUNCTION_PRIVATE_MESSAGE) {

    init {
        parameters.add(PARAM_FOLDERID, folder.toString())
        if (loadAll) {
            parameters.add(PARAM_SHOWALL, "1")
        }
    }


    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        AwfulMessage.processMessageList(contentResolver, doc, folder)
        return null
    }

}
