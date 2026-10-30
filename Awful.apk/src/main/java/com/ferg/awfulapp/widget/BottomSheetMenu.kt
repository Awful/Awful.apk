package com.ferg.awfulapp.widget

import android.view.Menu
import android.view.MenuItem
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.get
import androidx.core.view.size
import com.ferg.awfulapp.provider.ColorProvider
import kotlinx.coroutines.launch

@Composable
fun BottomSheetMenu(
    visible: Boolean,
    sheetMenu: Menu?,
    onItemClick: ((MenuItem) -> Unit)?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    LaunchedEffect(visible) {
        if (visible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    LaunchedEffect(sheetState.currentValue) {
        if (
            sheetState.currentValue == ModalBottomSheetValue.Hidden &&
            visible
        ) {
            onDismiss()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = Color(ColorProvider.BACKGROUND.color),
        sheetContent = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 24.dp
                )
            ) {
                items(sheetMenu?.size ?: 0) { index ->
                    val item = sheetMenu!![index]

                    BottomSheetMenuItem(
                        item = item,
                        onClick = {
                            onItemClick?.invoke(item)

                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    )
                }
            }
        },
        content = content
    )
}


@Composable
private fun BottomSheetMenuItem(
    item: MenuItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item.icon?.let { drawable ->
            Image(bitmap = drawable.toBitmap().asImageBitmap(),
                contentDescription = "icon",
                modifier = Modifier
                    .size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if(!item.title.isNullOrEmpty()) {
            Text(
                text = item.title?.toString().orEmpty(),
                color = Color(ColorProvider.PRIMARY_TEXT.color),
                textAlign = TextAlign.Center
            )
        }
    }
}