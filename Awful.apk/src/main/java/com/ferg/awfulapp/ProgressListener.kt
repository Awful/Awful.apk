package com.ferg.awfulapp

import com.android.volley.VolleyError
import com.ferg.awfulapp.task.AwfulRequest


/**
 * Utility callbacks for AwfulRequest status updates.
 * This is for updating the actionbar within AwfulFragment.
 * You shouldn't need to use these, look at the AwfulResultCallback interface for success/failure results.
 */
interface ProgressListener {
    fun requestStarted(req: AwfulRequest<*>)
    fun requestUpdate(req: AwfulRequest<*>, percent: Int)
    fun requestEnded(req: AwfulRequest<*>, error: VolleyError?)
}