package com.ferg.awfulapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.ferg.awfulapp.widget.BottomSheetMenu


open class AwfulSheetActivity: AwfulActivity() {

    private lateinit var bottomSheetComposeView: ComposeView

    private var bottomSheetMenu by mutableStateOf<Menu?>(null)

    private var bottomSheetVisible by mutableStateOf(false)

    private var bottomSheetClickListener by mutableStateOf<((MenuItem) -> Unit)?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun setupBottomSheet() {
        bottomSheetComposeView = findViewById(R.id.bottom_sheet_compose)

        bottomSheetComposeView.setContent {
            BottomSheetMenu(
                visible = bottomSheetVisible,
                sheetMenu = bottomSheetMenu,
                onItemClick = bottomSheetClickListener,
                onDismiss = {
                    hideBottomSheet()
                }
            ) {}
        }
    }


    fun toggleBottomSheet(
        menu: Menu,
        onItemClick: ((MenuItem) -> Unit)) {
        if (bottomSheetVisible) {
            hideBottomSheet()
        } else {
            showBottomSheet(menu, onItemClick)
        }
    }

    fun showBottomSheet(
        menu: Menu,
        onItemClick: ((MenuItem) -> Unit)
    ) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)

        menu.let {
            this.bottomSheetMenu = it
        }
        onItemClick.let {
            this.bottomSheetClickListener = onItemClick
        }

        bottomSheetVisible = true
    }

    fun hideBottomSheet() {
        bottomSheetVisible = false
    }
}