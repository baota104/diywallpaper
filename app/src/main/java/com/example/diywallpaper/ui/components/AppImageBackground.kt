package com.example.diywallpaper.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.diywallpaper.R

@Composable
fun AppImageBackground(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.img_bg_app),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
