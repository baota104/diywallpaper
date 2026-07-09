package com.example.diywallpaper.ui.feature.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diywallpaper.R
import com.example.diywallpaper.core.utils.trackings.TrackingEvents
import com.example.diywallpaper.domain.model.Language
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.PlusJakartaSans
import com.example.diywallpaper.core.utils.manager.LocaleManager
import com.example.diywallpaper.core.utils.trackings.Trackings
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.collections.get
import kotlin.math.roundToInt

@Composable
fun LanguageScreen(
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current

    val languages = remember {
        listOf(
            Language("en", "English", R.drawable.ic_flag_en),
            Language("pt", "Portuguese", R.drawable.ic_flag_pt),
            Language("id", "Indonesian", R.drawable.ic_flag_id),
            Language("es", "Espanol", R.drawable.ic_flag_es),
            Language("hi", "Hindi", R.drawable.ic_flag_hi),
            Language("fr", "French", R.drawable.ic_flag_fr),
            Language("vi", "Tiếng Việt", R.drawable.ic_flag_vi)
        )
    }

    val countryMap = remember {
        mapOf(
            "en" to "United Kingdom",
            "pt" to "Portugal",
            "id" to "Indonesia",
            "es" to "Spain",
            "hi" to "India",
            "fr" to "France",
            "vi" to "Vietnam"
        )
    }

    val isPreview = LocalInspectionMode.current
    val systemLangCode = remember {
        try {
            val deviceLanguage = Locale.getDefault().language
            if (deviceLanguage == "in") "id" else deviceLanguage
        } catch (e: Exception) {
            "en"
        }
    }
    val targetLanguageCode = remember(systemLangCode, languages) {
        languages.firstOrNull { it.code == systemLangCode }?.code ?: "en"
    }
    val sortedLanguages = remember(languages, targetLanguageCode) {
        val mutableLangs = languages.toMutableList()
        val targetLang = mutableLangs.find { it.code == targetLanguageCode }
        if (targetLang != null) {
            mutableLangs.remove(targetLang)
            mutableLangs.add(2, targetLang)
        }
        mutableLangs
    }

    val savedLang = remember { if (isPreview) "" else LocaleManager.getLocale(context) }
    var selectedLang by remember { mutableStateOf(savedLang) }
    var hasStartedDoneTimer by remember { mutableStateOf(savedLang.isNotEmpty()) }
    var isDoneVisible by remember { mutableStateOf(savedLang.isNotEmpty()) }
    var adReloadKey by remember { mutableIntStateOf(1) }
    var hasReloadedLanguageAd by remember { mutableStateOf(savedLang.isNotEmpty()) }

    LaunchedEffect(Unit) {
        val source = if (savedLang.isNotEmpty()) "set" else "spl"
        Trackings.logFirebaseTracking(TrackingEvents.langView(source))
    }

    LaunchedEffect(hasStartedDoneTimer, savedLang) {
        if (savedLang.isEmpty() && hasStartedDoneTimer) {
            delay(3_000L)
            isDoneVisible = true
        }
    }

    LanguageContent(
        sortedLanguages = sortedLanguages,
        countryMap = countryMap,
        selectedLang = selectedLang,
        isDoneVisible = isDoneVisible,
        adReloadKey = adReloadKey,
        onLanguageSelect = { code ->
            Trackings.logFirebaseTracking(TrackingEvents.langSelect(code))
            if (selectedLang != code) {
                selectedLang = code
                if (!hasStartedDoneTimer && savedLang.isEmpty()) {
                    hasStartedDoneTimer = true
                }
                if (!hasReloadedLanguageAd && savedLang.isEmpty()) {
                    hasReloadedLanguageAd = true
                    adReloadKey++
                }
            }
        },
        onDoneClick = {
            onLanguageSelected(selectedLang)
        }
    )
}

@Composable
private fun LanguageContent(
    sortedLanguages: List<Language>,
    countryMap: Map<String, String>,
    selectedLang: String,
    isDoneVisible: Boolean,
    adReloadKey: Int,
    onLanguageSelect: (String) -> Unit,
    onDoneClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isPreview = LocalInspectionMode.current
    var radioButtonPosition by remember { mutableStateOf(Offset.Zero) }
    var containerRootPosition by remember { mutableStateOf(Offset.Zero) }
    var showLottie by remember { mutableStateOf(selectedLang.isEmpty()) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .systemBarsPadding(),
        containerColor = colorScheme.background,
        topBar = {
            LanguageHeader(
                onDoneClick = onDoneClick,
                showCheck = isDoneVisible
            )
        }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        if (!isPreview) {
                            containerRootPosition = coordinates.positionInRoot()
                        }
                    }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(sortedLanguages) { index, lang ->
                        LanguageItemComponent(
                            lang = lang,
                            countryName = countryMap[lang.code].orEmpty(),
                            isSelected = lang.code == selectedLang,
                            onClick = {
                                onLanguageSelect(lang.code)
                                showLottie = false
                            },
                            onRadioButtonPositioned = if (index == 2 && showLottie && !isPreview) {
                                { position -> radioButtonPosition = position }
                            } else null
                        )
                    }
                }

                if (showLottie && radioButtonPosition != Offset.Zero && containerRootPosition != Offset.Zero && !isPreview) {
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.ic_select_lg)
                    )

                    LottieAnimation(
                        composition = composition,
                        modifier = Modifier
                            .offset {
                                val offsetX = radioButtonPosition.x - containerRootPosition.x - 45.dp.toPx()
                                val offsetY = radioButtonPosition.y - containerRootPosition.y - 45.dp.toPx()
                                IntOffset(
                                    x = offsetX.roundToInt(),
                                    y = offsetY.roundToInt()
                                )
                            }
                            .size(120.dp),
                        isPlaying = true,
                        iterations = LottieConstants.IterateForever
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageHeader(
    onDoneClick: () -> Unit,
    showCheck: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.language),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            ),
            fontSize = 30.sp,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 5.dp)
        )
        if (showCheck) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDoneClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.common_continue),
                        tint = colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageItemComponent(
    lang: Language,
    countryName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRadioButtonPositioned: ((Offset) -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = if (isSelected) colorScheme.primary.copy(alpha = 0.34f) else colorScheme.surface
    val borderColor = if (isSelected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.22f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(lang.flagRes),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lang.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface
                )
                if (countryName.isNotEmpty()) {
                    Text(
                        text = countryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface.copy(alpha = 0.64f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (onRadioButtonPositioned != null) {
                            Modifier.onGloballyPositioned { coords ->
                                onRadioButtonPositioned(coords.positionInRoot())
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = if (isSelected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colorScheme.secondary, CircleShape)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Language Content Initial Selection")
@Composable
fun LanguageContentPreviewInitial() {
    val languages = listOf(
        Language("en", "English", R.drawable.ic_flag_en),
        Language("pt", "Portuguese", R.drawable.ic_flag_pt),
        Language("id", "Indonesian", R.drawable.ic_flag_id)
    )
    val countryMap = mapOf(
        "en" to "United Kingdom",
        "pt" to "Portugal",
        "id" to "Indonesia"
    )

    DIYWallpaperTheme (dynamicColor = false) {
        LanguageContent(
            sortedLanguages = languages,
            countryMap = countryMap,
            selectedLang = "",
            isDoneVisible = false,
            adReloadKey = 1,
            onLanguageSelect = {},
            onDoneClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Language Content Selected")
@Composable
fun LanguageContentPreviewSelected() {
    val languages = listOf(
        Language("en", "English", R.drawable.ic_flag_en),
        Language("pt", "Portuguese", R.drawable.ic_flag_pt),
        Language("id", "Indonesian", R.drawable.ic_flag_id)
    )
    val countryMap = mapOf(
        "en" to "United Kingdom",
        "pt" to "Portugal",
        "id" to "Indonesia"
    )

    DIYWallpaperTheme(dynamicColor = false) {
        LanguageContent(
            sortedLanguages = languages,
            countryMap = countryMap,
            selectedLang = "en",
            isDoneVisible = true,
            adReloadKey = 1,
            onLanguageSelect = {},
            onDoneClick = {}
        )
    }
}
