package com.tobiso.tobisoappnative.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tobiso.tobisoappnative.ui.theme.Figtree

enum class ReadingFont(val label: String, val family: FontFamily) {
    FIGTREE("Figtree", Figtree),
    SERIF("Patkové", FontFamily.Serif),
    MONO("Mono", FontFamily.Monospace)
}

enum class ReadingSize(val label: String, val scale: Float) {
    SMALL("S", 0.9f),
    MEDIUM("M", 1f),
    LARGE("L", 1.15f)
}

enum class ReadingWidth(val label: String, val maxWidth: Dp?) {
    NARROW("Úzký", 480.dp),
    MEDIUM("Střední", 640.dp),
    WIDE("Široký", null)
}

data class ReadingPrefs(
    val font: ReadingFont = ReadingFont.FIGTREE,
    val size: ReadingSize = ReadingSize.MEDIUM,
    val width: ReadingWidth = ReadingWidth.WIDE
)

private const val PREFS_READING = "ReadingPrefs"

private inline fun <reified E : Enum<E>> SharedPrefsEnum(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

fun loadReadingPrefs(context: Context): ReadingPrefs {
    val prefs = context.getSharedPreferences(PREFS_READING, Context.MODE_PRIVATE)
    return ReadingPrefs(
        font = SharedPrefsEnum(prefs.getString("font", null), ReadingFont.FIGTREE),
        size = SharedPrefsEnum(prefs.getString("size", null), ReadingSize.MEDIUM),
        width = SharedPrefsEnum(prefs.getString("width", null), ReadingWidth.WIDE)
    )
}

fun saveReadingPrefs(context: Context, value: ReadingPrefs) {
    context.getSharedPreferences(PREFS_READING, Context.MODE_PRIVATE).edit()
        .putString("font", value.font.name)
        .putString("size", value.size.name)
        .putString("width", value.width.name)
        .apply()
}

/** Applies the reader's font, text size and column width to everything inside [content]. */
@Composable
fun ReadingStyle(prefs: ReadingPrefs, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val base = MaterialTheme.typography
    val typography = remember(base, prefs.font) {
        if (prefs.font == ReadingFont.FIGTREE) base else {
            val family = prefs.font.family
            base.copy(
                bodyLarge = base.bodyLarge.copy(fontFamily = family),
                bodyMedium = base.bodyMedium.copy(fontFamily = family),
                bodySmall = base.bodySmall.copy(fontFamily = family)
            )
        }
    }
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * prefs.size.scale)
    ) {
        MaterialTheme(typography = typography) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                val widthModifier = prefs.width.maxWidth?.let { Modifier.widthIn(max = it) } ?: Modifier
                Box(Modifier.fillMaxHeight().then(widthModifier)) { content() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    prefs: ReadingPrefs,
    onChange: (ReadingPrefs) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Nastavení čtení", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            SettingRow("Písmo", ReadingFont.entries, prefs.font, { it.label }) { onChange(prefs.copy(font = it)) }
            SettingRow("Velikost", ReadingSize.entries, prefs.size, { it.label }) { onChange(prefs.copy(size = it)) }
            SettingRow("Šířka sloupce", ReadingWidth.entries, prefs.width, { it.label }) { onChange(prefs.copy(width = it)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> SettingRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
    ) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}
