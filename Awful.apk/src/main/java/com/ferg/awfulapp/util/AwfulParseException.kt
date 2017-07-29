package com.ferg.awfulapp.util

/**
 * Created by baka kaba on 30/07/2017.
 */
class AwfulParseException: Exception {
    constructor(): super()
    constructor(ex: Exception): super(ex)
    constructor(message: String): super(message)
    constructor(message: String, ex: Exception?): super(message, ex)
}