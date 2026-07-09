package com.example.diywallpaper.ui.feature.language

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.R
import com.example.diywallpaper.ui.theme.Surface
import com.example.diywallpaper.core.utils.manager.LocaleManager
import kotlinx.coroutines.delay
import kotlin.ranges.coerceAtMost
import kotlin.ranges.coerceIn

enum class ApplyingLanguageSource {
    FIRST_OPEN,
    SETTINGS
}

@Composable
fun ApplyingLanguageRoute(
    languageCode: String,
    source: ApplyingLanguageSource,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val savedLocale = LocaleManager.getLocale(context)
    var progress by remember { mutableFloatStateOf(0.12f) }
    var hasFinished by remember { mutableStateOf(false) }


    LaunchedEffect(languageCode, source) {
        while (!hasFinished) {
            delay(120L)
            val maxProgress = when (source) {
                ApplyingLanguageSource.SETTINGS -> 0.9f
                else -> 0.94f
            }
            if (progress < maxProgress) {
                progress = (progress + 0.035f).coerceAtMost(maxProgress)
            } else {
                if (savedLocale != languageCode) {
                    LocaleManager.setLocale(context, languageCode)
                }
                delay(if (source == ApplyingLanguageSource.SETTINGS) 250L else 400L)
                progress = 1f
                hasFinished = true
                onFinished()
            }
        }
    }

    ApplyingLanguageScreenContent(progress = progress)
}

@Composable
fun ApplyingLanguageScreenContent(progress: Float) {
    val colorScheme = MaterialTheme.colorScheme


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor =colorScheme.primary.copy(alpha = 0.12f),
                        spotColor = colorScheme.primary.copy(alpha = 0.16f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(id = R.string.applying_language_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.applying_language_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface.copy(alpha = 0.64f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = colorScheme.primary,
                trackColor = colorScheme.primary.copy(alpha = 0.14f),
                drawStopIndicator = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewApplyingLanguageScreen() {
    DIYWallpaperTheme (dynamicColor = false) {
        ApplyingLanguageScreenContent(progress = 0.65f)
    }
}
