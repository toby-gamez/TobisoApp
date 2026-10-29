package com.tobiso.tobisoappnative.components.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tobiso.tobisoappnative.components.buildAnnotatedStringFromParts
import com.tobiso.tobisoappnative.components.parseInlineParts
import com.tobiso.tobisoappnative.ui.theme.Baloo2
import com.tobiso.tobisoappnative.ui.theme.Figtree

internal fun parseDiagramColor(hex: String?, fallback: Color): Color {
    if (hex == null) return fallback
    var h = hex.removePrefix("#")
    if (h.length == 3) h = h.map { "$it$it" }.joinToString("")
    return h.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: fallback
}

/** The web's colour tokens (Tobiso.Web.App/wwwroot/css/variables.css) used by `.fc-*` and `.venn-*`, light and dark. */
internal class WebDiagramPalette(
    val bg: Color, val surface: Color, val text: Color,
    val accent100: Color, val accent600: Color, val accent900: Color,
    val accent2100: Color, val accent2600: Color,
    val neutral600: Color, val neutral700: Color,
    val vennHighlight: Color,
) {
    companion object {
        val Light = WebDiagramPalette(
            bg = Color(0xFFF5EAD8), surface = Color(0xFFEBDDC5), text = Color(0xFF201E1D),
            accent100 = Color(0xFFFDF3EC), accent600 = Color(0xFFB2622D), accent900 = Color(0xFF5C2B11),
            accent2100 = Color(0xFFF0F5E6), accent2600 = Color(0xFF607C43),
            neutral600 = Color(0xFF907860), neutral700 = Color(0xFF72604C),
            vennHighlight = Color(0xFFB2622D),
        )
        val Dark = WebDiagramPalette(
            bg = Color(0xFF1F1C18), surface = Color(0xFF2A2520), text = Color(0xFFF0E8DC),
            accent100 = Color(0xFF221B16), accent600 = Color(0xFF7F5E48), accent900 = Color(0xFFD3A992),
            accent2100 = Color(0xFF1D2218), accent2600 = Color(0xFF68794F),
            neutral600 = Color(0xFF958472), neutral700 = Color(0xFFB3A08D),
            vennHighlight = Color(0xFFE0915A),
        )
    }
}

@Composable
internal fun rememberWebPalette(): WebDiagramPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) WebDiagramPalette.Dark else WebDiagramPalette.Light

/** Diagram card on the web's page background, with optional title, the diagram [content], warnings and notes (the web's `<figure>`). */
@Composable
internal fun DiagramFrame(
    title: String?,
    warnings: List<String>,
    notes: List<String>,
    palette: WebDiagramPalette,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(palette.bg, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        title?.let {
            val annotated = remember(it, linkColor) { buildAnnotatedStringFromParts(parseInlineParts(it), linkColor) }
            Text(annotated, style = TextStyle(fontFamily = Baloo2, fontSize = 16.sp, color = palette.text), modifier = Modifier.padding(bottom = 8.dp))
        }
        content()
        warnings.forEach {
            Text(it, style = TextStyle(fontFamily = Figtree, fontSize = 13.sp, color = palette.accent900), modifier = Modifier.padding(top = 8.dp))
        }
        if (notes.isNotEmpty()) {
            Column(Modifier.padding(top = 8.dp)) {
                notes.forEach { note ->
                    val annotated = remember(note, linkColor) { buildAnnotatedStringFromParts(parseInlineParts(note), linkColor) }
                    Text(annotated, style = TextStyle(fontFamily = Figtree, fontSize = 13.sp, color = palette.neutral700), modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        }
    }
}

@Composable
internal fun DiagramError(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    )
}
