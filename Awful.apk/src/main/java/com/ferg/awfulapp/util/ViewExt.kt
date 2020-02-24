package com.ferg.awfulapp.util

import android.app.Activity
import android.support.v4.app.Fragment
import android.view.View
import android.view.ViewGroup


fun View.isVisible() = (this.visibility == View.VISIBLE)
fun View.isHidden() = (this.visibility == View.INVISIBLE || this.visibility == View.GONE)

fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.setInvisible() {
    this.visibility = View.INVISIBLE
}

/**
 * These let you late-bind views as vals, so long as you only access them after the view has been created
 * e.g. "val myTextView: TextView by bind(R.id.some_textview)"
 */
fun <T : View?> Fragment.bind(resId: Int): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) { view!!.findViewById<T>(resId) }

fun <T : View?> Activity.bind(resId: Int): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) { findViewById<T>(resId) }

fun <T : View?> ViewGroup.bind(resId: Int): Lazy<T> =
        lazy(LazyThreadSafetyMode.NONE) { findViewById<T>(resId) }