package com.example.diywallpaper.ui.feature.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diywallpaper.R
import com.example.diywallpaper.ui.common.VitalityPrimaryButton
import com.example.diywallpaper.ui.theme.Border
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.Primary

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = hiltViewModel(),
    onNavigateNext: () -> Unit
) {
    WelcomeContent(
        onStartClick = {
            viewModel.completeWelcome()
            onNavigateNext()
        }
    )
}

@Composable
private fun WelcomeContent(
    onStartClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // 1. Mesh Background Aurora Glow (Sử dụng các token màu định nghĩa sẵn từ Color.kt)
        val meshPink = com.example.diywallpaper.ui.theme.PinkSoft.copy(alpha = 0.75f)
        val meshMint = com.example.diywallpaper.ui.theme.MintGreen.copy(alpha = 0.35f)
        val meshBlue = com.example.diywallpaper.ui.theme.SkySoft.copy(alpha = 0.8f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Điểm loang màu hồng nhạt ở góc trên bên trái
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(meshPink, Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = size.width * 0.8f
                ),
                center = Offset(0f, 0f),
                radius = size.width * 0.8f
            )

            // Điểm loang màu xanh mint lục ở giữa bên phải
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(meshMint, Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.55f),
                    radius = size.width * 0.75f
                ),
                center = Offset(size.width * 0.9f, size.height * 0.55f),
                radius = size.width * 0.75f
            )

            // Điểm loang màu xanh dương nhạt ở phía góc dưới bên trái/ở giữa đáy
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(meshBlue, Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.88f),
                    radius = size.width * 0.95f
                ),
                center = Offset(size.width * 0.3f, size.height * 0.88f),
                radius = size.width * 0.95f
            )
        }

        // 2. Nội dung giao diện chính cuộn được để tránh tràn màn hình nhỏ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Phần ảnh vẽ minh họa phía trên
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.38f)
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_wellcome),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tiêu đề & Mô tả
            Text(
                text = stringResource(id = R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.welcome_description),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Danh sách các thẻ tính năng (Glassmorphic)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WelcomeMenuCard(
                    icon = Icons.Default.Dashboard,
                    title = stringResource(id = R.string.welcome_menu_wallpapers_title),
                    description = stringResource(id = R.string.welcome_menu_wallpapers_desc)
                )

                WelcomeMenuCard(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(id = R.string.welcome_menu_templates_title),
                    description = stringResource(id = R.string.welcome_menu_templates_desc)
                )

                WelcomeMenuCard(
                    icon = Icons.Default.Brush,
                    title = stringResource(id = R.string.welcome_menu_create_title),
                    description = stringResource(id = R.string.welcome_menu_create_desc)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Nút bấm bắt đầu thiết kế
            VitalityPrimaryButton(
                text = stringResource(id = R.string.welcome_btn_start),
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
            )
        }
    }
}

@Composable
private fun WelcomeMenuCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                       Border.copy(alpha = 0.65f),
                        Border.copy(alpha = 0.35f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon tròn bên trái có shadow nhẹ nổi bật
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tiêu đề & mô tả chức năng
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PreviewWelcomeContent() {
    DIYWallpaperTheme(dynamicColor = false) {
        WelcomeContent(onStartClick = {})
    }
}
