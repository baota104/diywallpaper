package com.example.diywallpaper.ui.feature.onboarding

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.app.Activity
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import com.example.diywallpaper.ui.feature.onboarding.OnboardingViewModel
import com.example.diywallpaper.core.utils.trackings.Trackings
import com.example.diywallpaper.R
import com.example.diywallpaper.core.utils.trackings.TrackingEvents
import com.example.diywallpaper.ui.common.VitalityPrimaryButton
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.Divider
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.PrimaryGradient

private data class OnboardingPageModel(
    val imageRes: Int,
    val titleRes: Int,
    val descriptionRes: Int
)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val currentPage = pagerState.currentPage
    val startTime = remember { System.currentTimeMillis() }
    var showLoadingOverlay by remember { mutableStateOf(false) }

    val pages = remember {
        listOf(
            OnboardingPageModel(
                imageRes = R.drawable.img_onboard1,
                titleRes = R.string.onboarding_title_1,
                descriptionRes = R.string.onboarding_description_1
            ),
            OnboardingPageModel(
                imageRes = R.drawable.img_onboard2,
                titleRes = R.string.onboarding_title_2,
                descriptionRes = R.string.onboarding_description_2
            ),
            OnboardingPageModel(
                imageRes = R.drawable.img_onboard3,
                titleRes = R.string.onboarding_title_3,
                descriptionRes = R.string.onboarding_description_3
            )
        )
    }


    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingContent(
            pages = pages,
            pagerState = pagerState,
            currentPage = currentPage,
            onPrimaryClick = {
                when (currentPage) {
                    0 -> {
                        Trackings.logFirebaseTracking(TrackingEvents.onbNextClick(1))
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }

                    }
                    1 -> {
                        Trackings.logFirebaseTracking(TrackingEvents.onbNextClick(2))
                            scope.launch {
                                pagerState.animateScrollToPage(2)
                            }


                    }
                    2 -> {
                        val timeSpent = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                        val bundle = Bundle().apply { putInt("time_spent", timeSpent) }
                        Trackings.logFirebaseTracking(TrackingEvents.ONB_FINISH_CLICK, bundle)
                        viewModel.completeOnboarding()
                        onOnboardingFinished()
                    }
                }
            }
        )

        if (showLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
    }
}

@Composable
private fun OnboardingContent(
    pages: List<OnboardingPageModel>,
    pagerState: PagerState,
    currentPage: Int,
    onPrimaryClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 24.dp, bottom = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                val item = pages[page]

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                            if (page == 2) {
                                // Màn onboard 3: Render điện thoại kèm 3 icon lơ lửng xung quanh có hiệu ứng
                                val infiniteTransition = rememberInfiniteTransition(label = "bobbing")
                                val bobbingOffset1 by infiniteTransition.animateFloat(
                                    initialValue = -5f,
                                    targetValue = 5f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "icon1"
                                )
                                val bobbingOffset2 by infiniteTransition.animateFloat(
                                    initialValue = 4f,
                                    targetValue = -4f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1400, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "icon2"
                                )
                                val bobbingOffset3 by infiniteTransition.animateFloat(
                                    initialValue = -3f,
                                    targetValue = 3f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1100, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "icon3"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(0.52f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Ảnh điện thoại ở giữa
                                    Image(
                                        painter = painterResource(id = item.imageRes),
                                        contentDescription = stringResource(item.titleRes),
                                        contentScale = ContentScale.FillBounds,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 1. Icon Download ở trên bên trái
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .offset(x = (-20).dp, y = (-70).dp + bobbingOffset1.dp)
                                            .shadow(8.dp, CircleShape)
                                            .background(Color.White, CircleShape)
                                            .padding(10.dp)
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // 2. Icon Share ở giữa bên phải
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .offset(x = 20.dp, y = (-20).dp + bobbingOffset2.dp)
                                            .shadow(8.dp, CircleShape)
                                            .background(Color.White, CircleShape)
                                            .padding(10.dp)
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // 3. Icon Apply (Set Wallpaper) ở dưới bên trái
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .offset(x = (-16).dp, y = 80.dp + bobbingOffset3.dp)
                                            .shadow(8.dp, CircleShape)
                                            .background(Color.White, CircleShape)
                                            .padding(10.dp)
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Default.Wallpaper,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else if (page == 1) {
                                // Màn onboard 2: Giới hạn chiều rộng 80% (0.8f) để vừa mắt, không bị bè thô
                                Image(
                                    painter = painterResource(id = item.imageRes),
                                    contentDescription = stringResource(item.titleRes),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .fillMaxHeight()
                                )
                            } else {
                                // Màn onboard 1: Hiển thị Fit bình thường để tránh mất chi tiết
                                Image(
                                    painter = painterResource(id = item.imageRes),
                                    contentDescription = stringResource(item.titleRes),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 0.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(item.titleRes),
                            textAlign = TextAlign.Center,
                           style = MaterialTheme.typography.displayMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(item.descriptionRes),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    }
                }
            }
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val selected = index == currentPage
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(width = 28.dp, height = 8.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = PrimaryGradient
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = Divider,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            VitalityPrimaryButton(
                text = stringResource(
                    if (currentPage == 2) R.string.onboarding_start else R.string.onboarding_next
                ),
                onClick = onPrimaryClick,
                modifier = Modifier
                    .width(120.dp)
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    val pages = listOf(
        OnboardingPageModel(
            imageRes = R.drawable.img_onboard1,
            titleRes = R.string.onboarding_title_1,
            descriptionRes = R.string.onboarding_description_1
        ),
        OnboardingPageModel(
            imageRes = R.drawable.img_onboard2,
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_description_2
        ),
        OnboardingPageModel(
            imageRes = R.drawable.img_onboard3,
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_description_3
        )
    )
    val pagerState = rememberPagerState(pageCount = { 3 })
    DIYWallpaperTheme (darkTheme = false) {
        OnboardingContent(
            pages = pages,
            pagerState = pagerState,
            currentPage = 0,
            onPrimaryClick = {}
        )
    }
}
