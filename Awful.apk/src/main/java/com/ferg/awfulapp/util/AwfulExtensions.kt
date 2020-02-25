package com.ferg.awfulapp.util

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher

/**
 * Created by baka kaba on 28/10/2018.
 *
 * General Kotlin extensions and functions for things that come up a lot. Put boilerplate here!
 */



/*
    nullable wrapper functions that return null instead of a default value
    these pass a "missing" default value to the #tryGet function, as a way of identifying when there's
    no mapping for the key - these values need to be safe, so they don't match an actual stored value
 */

private const val MISSING_INT_VALUE = -342534434

/** Get the Int value associated with a key, or null if no such value exists */
fun Bundle.tryGetInt(key: String): Int? {
    // seems like this is the only way to pass an overloaded function :/ can't just work it out by looking at what you're passing it into
    val temp: (String, Int) -> Int = this::getInt
    return temp.tryGet(key, MISSING_INT_VALUE)
}

/** Get the Int value associated with a key, or null if no such value exists */
fun Intent.tryGetIntExtra(key: String): Int? = this::getIntExtra.tryGet(key, MISSING_INT_VALUE)

private fun <T> ((String, T) -> T).tryGet(key: String, missing: T): T? {
    this(key, missing).let { result -> return if (result == missing) null else result }
}


/*
    Database/content values stuff
 */

/** This Boolean's SQLite representation (an int) */
val Boolean.toSqlBoolean: Int
    get() = if (this) 1 else 0

/** This Int's Boolean equivalent, when treated as a SQLite boolean */
val Int.fromSqlBoolean: Boolean
    get() = this >= 1


/*
    Utility classes
 */

/** A TextWatcher that does nothing - override whichever methods you actually need */
open class PassiveTextWatcher : TextWatcher {
    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}