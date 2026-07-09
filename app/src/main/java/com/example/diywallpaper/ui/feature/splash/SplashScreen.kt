package com.example.diywallpaper.ui.feature.splash

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.R
import com.example.diywallpaper.core.utils.trackings.TrackingEvents
import com.example.diywallpaper.ui.feature.splash.SplashLaunchState
import com.example.diywallpaper.ui.feature.splash.SplashViewModel
import com.example.diywallpaper.core.utils.trackings.Trackings
import com.example.diywallpaper.ui.theme.AuroraGradient
import com.example.diywallpaper.ui.theme.MintSoft
import com.example.diywallpaper.ui.theme.Pink
import com.example.diywallpaper.ui.theme.PinkSoft
import com.example.diywallpaper.ui.theme.PinkStrong
import com.example.diywallpaper.ui.theme.PurPleLight
import com.example.diywallpaper.ui.theme.Surface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onSplashFinished: (SplashLaunchState) -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableIntStateOf(0) }
    var hasNavigated by remember { mutableStateOf(false) }

    val launchState = remember { viewModel.consumeSplashLaunchState() }
    val isSdkInitialized by viewModel.isSdkInitialized.collectAsState()

    val navigateToNext = { reason: String ->
        if (!hasNavigated) {
            hasNavigated = true
            Log.d("baotq_ads", "Splash navigate next: reason=$reason")
            Trackings.logFirebaseTracking(TrackingEvents.splNext(reason))
            onSplashFinished(launchState)
        }
    }
    LaunchedEffect(key1 = true) {
        launch {
            while (progress < 100) {
                delay(if (progress < 82) 28L else 90L)
                if (progress < 100) {
                    progress += 1
                }
            }
        }
    }

    LaunchedEffect(isSdkInitialized, progress) {
        if (isSdkInitialized && progress >= 100 ) {
            navigateToNext("no_ads")
        }
    }
    SplashContent(progress = progress)
}

@Composable
fun SplashContent(progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface) // Nền sáng cơ bản
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PinkSoft, Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.2f),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.1f, size.height * 0.2f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PurPleLight, Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.5f),
                        radius = size.width * 0.8f
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.9f, size.height * 0.5f)
                )

                // Đốm xanh mint ở góc dưới bên phải
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(MintSoft, Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.9f),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.8f, size.height * 0.9f)
                )
            }
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(1.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color.Black.copy(alpha = 0.04f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_splash),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.splash_brand_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.splash_brand_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .padding(bottom = 8.dp),
                color = PinkStrong,
                strokeWidth = 3.dp,
                trackColor = Color.Transparent
            )
        }
    }
}

@Preview(showBackground = true, name = "Splash Preview")
@Composable
fun SplashContentPreview() {
    DIYWallpaperTheme (dynamicColor = false) {
        SplashContent(progress = 65)
    }
}
