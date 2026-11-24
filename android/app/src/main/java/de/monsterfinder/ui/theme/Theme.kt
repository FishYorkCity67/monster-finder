package de.monsterfinder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Neon = Color(0xFF95D600)
val Ink = Color(0xFF0B0D0B)
val Surface1 = Color(0xFF141714)
val Surface2 = Color(0xFF1C201C)
val Outline = Color(0xFF2A302A)
val TextHigh = Color(0xFFF2F5F0)
val TextMid = Color(0xFF9BA39A)
val Warn = Color(0xFFFFB547)

private val colors = darkColorScheme(
    primary = Neon,
    onPrimary = Ink,
    background = Ink,
    onBackground = TextHigh,
    surface = Surface1,
    onSurface = TextHigh,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMid,
    outline = Outline,
    error = Color(0xFFFF6B6B),
)

private val typography = Typography(
    displaySmall = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
)

@Composable
fun MonsterTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = colors, typography = typography, content = content)

fun chainColor(chain: String): Color = when (chain) {
    "lidl" -> Color(0xFF0050AA)
    "aldi" -> Color(0xFF00B4DC)
    "kaufland" -> Color(0xFFE10915)
    "rewe" -> Color(0xFFCC071E)
    "edeka" -> Color(0xFFFFD400)
    "netto" -> Color(0xFFFFE500)
    "penny" -> Color(0xFFCD1414)
    "rossmann" -> Color(0xFFC3002D)
    "dm" -> Color(0xFF002878)
    "globus" -> Color(0xFF005E3C)
    else -> Color(0xFF4A524A)
}
