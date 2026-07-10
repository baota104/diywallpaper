package com.example.diywallpaper.ui.theme


import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.diywallpaper.R

val PlusJakartaSans = FontFamily(
    Font(
        resId = R.font.plusjakartasans_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.plusjakartasans_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.plusjakartasans_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.plusjakartasans_bold,
        weight = FontWeight.Bold
    )
)

val Inter = FontFamily(
    Font(
        resId = R.font.inter_18pt_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.inter_18pt_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.inter_18pt_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.inter_18pt_bold,
        weight = FontWeight.Bold
    )
)

val AppTypography  = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),

    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)