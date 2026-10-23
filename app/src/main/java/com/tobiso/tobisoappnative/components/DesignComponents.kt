package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pill with a filled icon and a value (streak, points). */
@Composable
fun StatChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(scheme.primaryContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Rounded container whose rows are separated by hairline dividers. Use [GroupedListDivider] between rows. */
@Composable
fun GroupedList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupedListDivider() {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun GroupedListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = {
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    },
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

/** Large rounded colored card for hero blocks (Rychlý start, Pokračovat ve čtení, Řada, ...). */
@Composable
fun HeroCard(
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

/** Circle with an icon, tinted by [hue] (OKLCH-ish degrees) over the current background. */
@Composable
fun IconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = MaterialTheme.colorScheme.secondaryContainer,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Subject tint pair per the handoff: oklch(.74 .12 h) at 26% over bg, oklch(.56 .14 h) at 82% over onSurface. */
@Composable
fun subjectTint(hue: Float): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return oklch(0.74f, 0.12f, hue).copy(alpha = 0.26f).compositeOver(scheme.background) to
        oklch(0.56f, 0.14f, hue).copy(alpha = 0.82f).compositeOver(scheme.onSurface)
}

/** Hues from the design handoff, used to tint subject icons. */
fun subjectHue(name: String): Float = when (name) {
    "Český jazyk" -> 55f
    "Matematika" -> 250f
    "Chemie" -> 25f
    "Fyzika" -> 220f
    "Přírodopis" -> 140f
    "Zeměpis" -> 75f
    "Hudební výchova" -> 320f
    else -> (name.hashCode().mod(360)).toFloat()
}

enum class AnswerState { Idle, Selected, Correct, Wrong }

/** Quiz answer row: letter circle + text, 2dp border; correct = secondaryContainer, wrong = primaryContainer. */
@Composable
fun AnswerRow(
    letter: Char,
    text: String,
    state: AnswerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val container = when (state) {
        AnswerState.Idle -> Color.Transparent
        AnswerState.Selected -> scheme.primaryContainer
        AnswerState.Correct -> scheme.secondaryContainer
        AnswerState.Wrong -> scheme.primaryContainer
    }
    val border = when (state) {
        AnswerState.Idle -> scheme.outlineVariant
        AnswerState.Selected, AnswerState.Wrong -> scheme.primary
        AnswerState.Correct -> scheme.secondary
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .androidx_border(border)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val active = state != AnswerState.Idle
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(
                when (state) {
                    AnswerState.Correct -> scheme.secondary
                    AnswerState.Selected, AnswerState.Wrong -> scheme.primary
                    AnswerState.Idle -> scheme.surfaceContainerHigh
                }
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter.toString(),
                fontWeight = FontWeight.Bold,
                color = when (state) {
                    AnswerState.Correct -> scheme.onSecondary
                    AnswerState.Selected, AnswerState.Wrong -> scheme.onPrimary
                    AnswerState.Idle -> scheme.onSurfaceVariant
                }
            )
        }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = scheme.onSurface, modifier = Modifier.weight(1f))
        when (state) {
            AnswerState.Correct -> Icon(androidx.compose.material.icons.Icons.Rounded.CheckCircle, null, tint = scheme.secondary)
            AnswerState.Wrong -> Icon(androidx.compose.material.icons.Icons.Rounded.Cancel, null, tint = scheme.primary)
            else -> if (active) Unit
        }
    }
}

private fun Modifier.androidx_border(color: Color): Modifier =
    this.then(Modifier.border(2.dp, color, RoundedCornerShape(22.dp)))

/** Image that shows a centered progress circle while loading. */
@Composable
fun LoadingAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    coil.compose.SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        loading = {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            }
        }
    )
}

/** Exact OKLCH → sRGB conversion, used for the subject hue tints from the design. */
fun oklch(l: Float, c: Float, hDeg: Float): Color {
    val h = Math.toRadians(hDeg.toDouble())
    val a = c * Math.cos(h)
    val b = c * Math.sin(h)
    val l_ = l + 0.3963377774 * a + 0.2158037573 * b
    val m_ = l - 0.1055613458 * a - 0.0638541728 * b
    val s_ = l - 0.0894841775 * a - 1.2914855480 * b
    val ll = l_ * l_ * l_
    val mm = m_ * m_ * m_
    val ss = s_ * s_ * s_
    val r = 4.0767416621 * ll - 3.3077115913 * mm + 0.2309699292 * ss
    val g = -1.2684380046 * ll + 2.6097574011 * mm - 0.3413193965 * ss
    val bl = -0.0041960863 * ll - 0.7034186147 * mm + 1.7076147010 * ss
    fun enc(x: Double): Float {
        val v = x.coerceIn(0.0, 1.0)
        return (if (v <= 0.0031308) 12.92 * v else 1.055 * Math.pow(v, 1 / 2.4) - 0.055).toFloat()
    }
    return Color(enc(r), enc(g), enc(bl))
}

/** Non-collapsing large-title header used by Učivo / Procvičování / Kalendář / Profil (matches the mock). */
@Composable
fun LargeScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    titleTrailing: @Composable (() -> Unit)? = null
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            content = actions
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp,
                lineHeight = 38.sp
            )
            titleTrailing?.invoke()
        }
    }
}

/** "7. ročník ▾" chip that opens the grade picker; [filled] = profile style, otherwise outlined (home). */
@Composable
fun GradeChip(filled: Boolean, modifier: Modifier = Modifier) {
    val vm: com.tobiso.tobisoappnative.viewmodel.profile.ProfileViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val grades by vm.grades.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedId by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(com.tobiso.tobisoappnative.utils.loadGradeId(context))
    }
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.loadGrades() }
    val name = grades.firstOrNull { it.id == selectedId }?.name ?: return
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(if (filled) 26.dp else 32.dp)
            .clip(RoundedCornerShape(50))
            .then(
                if (filled) Modifier.background(scheme.primaryContainer)
                else Modifier.border(1.dp, scheme.outlineVariant, RoundedCornerShape(50))
            )
            .clickable { open = true }
            .padding(start = if (filled) 10.dp else 12.dp, end = if (filled) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            fontSize = if (filled) 12.5.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (filled) scheme.onPrimaryContainer else scheme.onSurfaceVariant
        )
        Icon(
            androidx.compose.material.icons.Icons.Rounded.ArrowDropDown,
            null,
            modifier = Modifier.size(20.dp),
            tint = if (filled) scheme.onPrimaryContainer else scheme.onSurfaceVariant
        )
    }
    if (open) {
        GradePickerSheet(
            grades = grades,
            activeGradeId = selectedId,
            onSelect = {
                selectedId = it.id
                com.tobiso.tobisoappnative.utils.saveGradeId(context, it.id)
                open = false
            },
            onDismiss = { open = false }
        )
    }
}
