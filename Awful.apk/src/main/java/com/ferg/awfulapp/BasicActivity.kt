package com.ferg.awfulapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View
import com.ferg.awfulapp.BasicActivity.Companion.intentFor

/**
 * Created by baka kaba on 31/07/2017.
 *
 * An AwfulActivity with a basic standard configuration, with a working Action Bar and a single fragment.
 *
 * This really exists to avoid adding multiple activities to the project when you just want to
 * host a fragment with the usual toolbar setup. Call [intentFor] to generate the appropriate intent
 * for a particular fragment, then you can call [startActivity] as usual.
 */


class BasicActivity : AwfulActivity() {

    companion object {
        private const val FRAGMENT_CLASS: String = "fragment class"
        private const val TITLE: String = "action bar title"

        fun intentFor(
            fragmentClass: Class<out Fragment>,
            context: Context,
            title: String = ""
        ): Intent =
            Intent(context, BasicActivity::class.java)
                .putExtra(FRAGMENT_CLASS, fragmentClass.name)
                .putExtra(TITLE, title)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.basic_activity)

        val fragmentName = intent.extras.getString(FRAGMENT_CLASS)
                ?: throw RuntimeException("No content fragment specified!")
        val fragment = Class.forName(fragmentName).newInstance() as Fragment
        supportFragmentManager
            .beginTransaction()
            .add(R.id.content_frame, fragment, fragmentName)
            .commit()

        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
        setUpActionBar()
        setActionbarTitle(intent.extras.getString(TITLE, "No title"))
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish(); return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}