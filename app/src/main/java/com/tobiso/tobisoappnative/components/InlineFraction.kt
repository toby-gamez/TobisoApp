package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text

@Composable
fun InlineFraction(
    numerator: String,
    denominator: String,
    modifier: Modifier = Modifier,
    lineThickness: Dp = 1.dp
) {
    val contentColor = LocalContentColor.current
    val surroundingStyle = LocalTextStyle.current
    val baseFontSize = if (surroundingStyle.fontSize == TextUnit.Unspecified) 14.sp else surroundingStyle.fontSize
    val smallFontSize = (baseFontSize.value * 0.75f).sp
    val smallStyle = surroundingStyle.copy(
        fontSize = smallFontSize,
        lineHeight = (smallFontSize.value * 1.1f).sp
    )

    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = numerator, style = smallStyle, color = contentColor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .height(lineThickness)
                .background(contentColor)
        )
        Text(text = denominator, style = smallStyle, color = contentColor)
    }
}
