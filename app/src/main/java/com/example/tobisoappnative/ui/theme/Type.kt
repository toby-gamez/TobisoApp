package com.example.tobisoappnative.ui.theme


import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.tobisoappnative.R

val poppins = FontFamily(
    Font(R.font.poppins_medium, FontWeight.Normal)
)
val poppins_regular = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = poppins,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = poppins_regular,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    displayLarge = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Bold, fontSize = 25.sp),
    titleLarge = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Bold, fontSize = 35.sp),
    headlineLarge = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Medium, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Medium, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Medium, fontSize = 22.sp),
    labelSmall = TextStyle(fontFamily = poppins_regular, fontWeight = FontWeight.Medium, fontSize = 13.sp)
)

