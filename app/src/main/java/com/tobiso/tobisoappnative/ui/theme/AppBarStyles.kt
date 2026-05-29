package com.tobiso.tobisoappnative.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Use on TopAppBar titles for all non-bottom-bar screens.
// No fontFamily → system default (Roboto), keeping Poppins exclusive to bottom-bar LargeTopAppBars.
val SecondaryTopBarTitle = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp
)
