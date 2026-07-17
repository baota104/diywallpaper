package com.example.diywallpaper.ui.feature.dashboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.BuildConfig
import com.example.diywallpaper.R
import com.example.diywallpaper.core.utils.manager.LocaleManager
import com.example.diywallpaper.ui.theme.BlueSoftCard
import com.example.diywallpaper.ui.theme.SettingsCardBackground
import com.example.diywallpaper.ui.theme.SettingsLanguageIconBackground
import com.example.diywallpaper.ui.theme.SettingsPolicyGreen
import com.example.diywallpaper.ui.theme.SettingsSharePink
import com.example.diywallpaper.ui.theme.SettingsVersionIconBackground
import com.example.diywallpaper.ui.theme.SettingsVersionTint
import com.example.diywallpaper.ui.theme.SoftCard
import com.example.diywallpaper.ui.theme.SkyBlue
import com.example.diywallpaper.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSettingsScreen(
    onOpenLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    DashboardSettingsContent(
        currentLanguage = currentLanguageLabel(LocaleManager.getLocale(context)),
        versionName = BuildConfig.VERSION_NAME,
        onOpenLanguage = onOpenLanguage,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardSettingsContent(
    currentLanguage: String,
    versionName: String,
    onOpenLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
            ) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Text(
                            text = stringResource(id = R.string.settings_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SettingsSectionTitle(text = stringResource(id = R.string.settings_preferences))
                SettingsCard {
                    SettingsRowItem(
                        title = stringResource(id = R.string.settings_language_title),
                        subtitle = currentLanguage,
                        icon = Icons.Rounded.Language,
                        iconBackground = SettingsLanguageIconBackground,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenLanguage
                    )
                }

                SettingsSectionTitle(text = stringResource(id = R.string.settings_about))
                SettingsCard {
                    SettingsRowItem(
                        title = stringResource(id = R.string.settings_terms_title),
                        icon = Icons.Rounded.Description,
                        iconBackground = BlueSoftCard,
                        iconTint = SkyBlue
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRowItem(
                        title = stringResource(id = R.string.settings_privacy_title),
                        icon = Icons.Rounded.Policy,
                        iconBackground = com.example.diywallpaper.ui.theme.MintSoftCard,
                        iconTint = SettingsPolicyGreen
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowItem(
                        title = stringResource(id = R.string.settings_share_title),
                        icon = Icons.Rounded.Share,
                        iconBackground = SoftCard,
                        iconTint = SettingsSharePink
                    )
                }

                SettingsSectionTitle(text = stringResource(id = R.string.settings_storage))
                SettingsCard {
                    SettingsRowItem(
                        title = stringResource(id = R.string.settings_version_title),
                        subtitle = stringResource(id = R.string.settings_version_value, versionName),
                        icon = Icons.Rounded.Info,
                        iconBackground = SettingsVersionIconBackground,
                        iconTint = SettingsVersionTint,
                        showChevron = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SettingsCardBackground,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun SettingsRowItem(
    title: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsLeadingIconBadge(
            icon = icon,
            backgroundColor = iconBackground,
            tint = iconTint
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = if (subtitle == null) 8.dp else 10.dp)
                    .size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsLeadingIconBadge(
    icon: ImageVector,
    backgroundColor: Color,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = backgroundColor, shape = CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun currentLanguageLabel(languageCode: String): String {
    return when (languageCode) {
        "pt" -> stringResource(id = R.string.settings_language_value_portuguese)
        "id", "in" -> stringResource(id = R.string.settings_language_value_indonesian)
        "es" -> stringResource(id = R.string.settings_language_value_spanish)
        "hi" -> stringResource(id = R.string.settings_language_value_hindi)
        "fr" -> stringResource(id = R.string.settings_language_value_french)
        "vi" -> stringResource(id = R.string.settings_language_value_vietnamese)
        else -> stringResource(id = R.string.settings_language_value_english)
    }
}
