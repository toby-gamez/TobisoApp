package com.tobiso.tobisoappnative.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tobiso.tobisoappnative.R

// Variable fonts: weight axis applies on API 26+, API 24-25 fall back to the default instance.
@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int, vararg weights: FontWeight) = FontFamily(
    weights.map { w ->
        Font(resId, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    }
)

val Baloo2 = variableFamily(R.font.baloo2, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
val Figtree = variableFamily(R.font.figtree, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Baloo2, fontWeight = FontWeight.Bold, fontSize = 25.sp),
    displayMedium = base.displayMedium.copy(fontFamily = Baloo2, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = Baloo2, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Baloo2, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = Baloo2, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = Baloo2, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = base.titleLarge.copy(fontFamily = Baloo2, fontWeight = FontWeight.Bold, fontSize = 35.sp),
    titleMedium = base.titleMedium.copy(fontFamily = Baloo2, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = base.titleSmall.copy(fontFamily = Baloo2, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = base.bodyMedium.copy(fontFamily = Figtree),
    bodySmall = base.bodySmall.copy(fontFamily = Figtree),
    labelLarge = base.labelLarge.copy(fontFamily = Figtree),
    labelMedium = base.labelMedium.copy(fontFamily = Figtree),
    labelSmall = base.labelSmall.copy(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 13.sp)
)
