package com.ferg.awfulapp.util

import android.view.View


fun View.isVisible() = (this.visibility == View.VISIBLE)
fun View.isHidden() = (this.visibility == View.INVISIBLE || this.visibility == View.GONE)

fun View.hide() { this.visibility = View.GONE }
fun View.show() { this.visibility = View.VISIBLE }
fun View.setInvisible() { this.visibility = View.INVISIBLE }