package com.ferg.awfulapp.users

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import com.ferg.awfulapp.AwfulActivity
import com.ferg.awfulapp.NavigationEvent
import com.ferg.awfulapp.R
import com.ferg.awfulapp.util.bind


/**
 * Created by baka kaba on 29/07/2017.
 *
 * Basic activity to hold a [LepersColonyFragment]
 */
class LepersColonyActivity : AwfulActivity() {

    private val toolbar: Toolbar by bind(R.id.toolbar)
    private lateinit var fragment: LepersColonyFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.basic_activity)

        setSupportActionBar(toolbar)
        setUpActionBar()

        val fragmentName = LepersColonyFragment::class.java.name
        fragment = supportFragmentManager.findFragmentByTag(fragmentName) as LepersColonyFragment? ?: LepersColonyFragment()
        supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment, fragmentName)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (fragment.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    // TODO: maybe replace this with BasicActivity? The fragment handles the intent parsing and navigation, this routing is never actually used?

    override fun handleNavigation(event: NavigationEvent): Boolean {
        if (event is NavigationEvent.LepersColony) {
            return true.also { fragment.navigate(event) }
        }
        return super.handleNavigation(event)
    }

    override fun onBackPressed() {
        if (!fragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

}