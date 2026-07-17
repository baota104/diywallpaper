package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.ui.theme.PrimaryGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onBackClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
    ) {
        CenterAlignedTopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            title = {
                Text(
                    text = stringResource(id = R.string.editor_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .basicMarquee(iterations = Int.MAX_VALUE)

                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(id = R.string.editor_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                val activeActionColor = MaterialTheme.colorScheme.onSurfaceVariant
                val inactiveActionColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUndoClick,
                        enabled = canUndo
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Undo,
                            contentDescription = stringResource(id = R.string.editor_undo),
                            tint = if (canUndo) activeActionColor else inactiveActionColor
                        )
                    }
                    IconButton(
                        onClick = onRedoClick,
                        enabled = canRedo
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Redo,
                            contentDescription = stringResource(id = R.string.editor_redo),
                            tint = if (canRedo) activeActionColor else inactiveActionColor
                        )
                    }
                    IconButton(onClick = onPreviewClick) {
                        Icon(
                            imageVector = Icons.Outlined.RemoveRedEye,
                            contentDescription = stringResource(id = R.string.editor_preview),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onNextClick,
                        modifier = Modifier
                            .height(36.dp)
                            .wrapContentWidth()
                            .padding(end = 12.dp),
                        colors = buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = horizontalGradient(PrimaryGradient),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.editor_next),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        )
    }
}
