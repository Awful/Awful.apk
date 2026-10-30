package com.ferg.awfulapp

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.ferg.awfulapp.MessageFragment.PrivateMessageCallbacks
import com.ferg.awfulapp.constants.Constants

class MessageDisplayActivity : AwfulSheetActivity(), PrivateMessageCallbacks {
    var mToolbar: Toolbar? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.private_message_activity)
        mToolbar = findViewById(R.id.awful_toolbar)
        setSupportActionBar(mToolbar)
        setUpActionBar()
        setActionbarTitle("Message")
        setContentPane()
    }

    fun setContentPane() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_pane) == null) {
            val fragment = MessageFragment(
                intent.getStringExtra(Constants.PARAM_USERNAME), intent.getIntExtra(
                    Constants.PARAM_PRIVATE_MESSAGE_ID, 0
                )
            )

            setupBottomSheet()

            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_pane, fragment)
            transaction.commit()
        }
    }


    override fun onMessageClosed() {
        finish()
    }
}
