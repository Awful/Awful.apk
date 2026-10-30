package com.ferg.awfulapp.forums

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ferg.awfulapp.R
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.ColorProvider

@Composable
fun ExpandableForumIndexList(
    forums: List<Forum>,
    showSubtitles: Boolean,
    onForumClick: (Forum) -> Unit,
    onForumFavToggle: (Forum) -> Unit,
    listState: LazyListState,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(ColorProvider.BACKGROUND.color)),
        state = listState
    ) {
        items(
            items = forums,
            key = { it.id },

        ) { forum ->
            if (forum.isType(ForumType.SECTION)) {
                ForumListSection(forum.title.orEmpty())
            } else {
                ForumListItem(
                    forum = forum,
                    showSubtitles = showSubtitles,
                    onForumClick = onForumClick,
                    onForumFavToggle = onForumFavToggle,
                )
            }
        }
    }
}

@Composable
private fun ForumListItem(
    forum: Forum,
    showSubtitles: Boolean,
    onForumClick: (Forum) -> Unit,
    onForumFavToggle: (Forum) -> Unit,
) {
    var expanded by rememberSaveable(forum.id) {
        mutableStateOf(false)
    }

    val hasChildren = forum.subforums.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = dimensionResource(R.dimen.material_list_item_height)
            ),
    ) {
        ForumRow(
            forum = forum,
            showSubtitles = showSubtitles,
            hasChildren = hasChildren,
            expanded = expanded,
            onExpand = {
                expanded = !expanded
            },
            onClick = {
                onForumClick(forum)
            },
            onForumFavToggle = {
                onForumFavToggle(forum)
            }
        )
        if (expanded && hasChildren) {
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.08f),
                                Color.Transparent,
                            )
                        )
                    )
                )

        }
        AnimatedVisibility(
            visible = expanded && hasChildren,
            enter = expandVertically(
                animationSpec = tween(250)
            ) + fadeIn(
                animationSpec = tween(150)
            ),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(250)
            ) + fadeOut(
                animationSpec = tween(150)
            ),
        ) {
            Column {
                forum.subforums.forEachIndexed { index, child ->
                    if (index > 0) {
                        ForumDivider()
                    }
                    ForumRow(
                        forum = child,
                        isChild = true,
                        showSubtitles = showSubtitles,
                        onClick = {
                            onForumClick(child)
                        },
                        onForumFavToggle = {
                            onForumFavToggle(child)
                        }
                    )
                }
            }
        }
    }

    ForumDivider()
}

@Composable
private fun ForumRow(
    forum: Forum,
    showSubtitles: Boolean,
    hasChildren: Boolean = false,
    isChild: Boolean = false,
    expanded: Boolean = false,
    onExpand: () -> Unit = {},
    onClick: () -> Unit,
    onForumFavToggle: (Forum) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .defaultMinSize(minHeight = dimensionResource(R.dimen.material_list_item_height))
            .background(
                Color(ColorProvider.BACKGROUND.color)
            )

    ) {
        if(isChild) {
            Spacer(Modifier.width(56.dp))
        } else {
            Column(
                modifier = Modifier
                    .width(dimensionResource(R.dimen.material_content_left_margin))
                    .fillMaxHeight()
                    .padding(dimensionResource(R.dimen.material_screen_edge_margin))
                    .clickable(
                        enabled = hasChildren,
                        onClick = onExpand
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (AwfulPreferences.getInstance().threadInfo_Tag && forum.tagUrl != null) {
                    FrogForumTag(forum)
                }

                if (hasChildren) {
                    ArrowOpenClose(expanded)
                }
            }
        }



        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick =  { onForumFavToggle(forum) }
                )
                .padding(
                    top = dimensionResource(R.dimen.material_list_item_vertical_padding),
                    end = dimensionResource(R.dimen.material_screen_edge_margin),
                    bottom = dimensionResource(R.dimen.material_list_item_vertical_padding)
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                if(forum.isFavourite) {
                    Image(
                        painterResource(R.drawable.ic_star_gold),
                        "favorite",
                        Modifier
                            .size(dimensionResource(R.dimen.thread_item_sticky_locked_icon_size), dimensionResource(R.dimen.thread_item_sticky_locked_icon_size))
                            .padding(end = 4.dp),

                        )
                }
                Text(
                    text = forum.title.orEmpty(),
                    color = Color(ColorProvider.PRIMARY_TEXT.color),
                    fontSize = 16.sp
                )
            }

            if (showSubtitles && !forum.subtitle.isNullOrEmpty()) {
                Text(
                    text = forum.subtitle.orEmpty(),
                    color = Color(ColorProvider.ALT_TEXT.color),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ArrowOpenClose(expanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) -540f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "dropdown rotation"
    )

    Icon(
        painter = painterResource(
            if (expanded) {
                R.drawable.ic_expand_less
            } else {
                R.drawable.ic_expand_more
            }
        ),
        contentDescription = if (expanded) {
            "Collapse"
        } else {
            "Expand"
        },
        modifier = Modifier
            .size(20.dp)
            .scale(1.3f)
            .rotate(rotation),
        tint = Color(ColorProvider.PRIMARY_TEXT.color),
    )
}

@Composable
private fun FrogForumTag(forum: Forum) {
    AndroidView(
        modifier = Modifier.size(40.dp),
        factory = { context ->
            SquareForumTag(context)
        },
        update = { view ->
            TagProvider.setSquareForumTag(view, forum)
        },
    )
}

@Composable
fun ForumListSection(
    title: String,
    modifier: Modifier = Modifier,
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.material_list_subtitle_height))
            .background(ColorProvider.getResourceColor(androidx.appcompat.R.attr.colorPrimary))
            .padding(
                horizontal = dimensionResource(R.dimen.material_screen_edge_margin)
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title,
            color = ColorProvider.getResourceColor(R.attr.iconColor),
        )
    }
}

@Composable
private fun ForumDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = ColorProvider.getResourceColor(android.R.attr.listDivider)
    )
}