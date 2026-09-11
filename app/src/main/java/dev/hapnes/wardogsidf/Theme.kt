package dev.hapnes.wardogsidf

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object Console {
    val Ink = Color(0xFF0A0D0B)
    val Panel = Color(0xFF121713)
    val PanelDeep = Color(0xFF0E120F)
    val Edge = Color(0xFF243026)
    val Amber = Color(0xFFE8B23A)
    val Signal = Color(0xFF7DDB8A)
    val Alarm = Color(0xFFE5604D)
    val Dim = Color(0xFF8D9A8F)
    val Bright = Color(0xFFE9F0E9)
}

private val ConsoleColors = darkColorScheme(
    primary = Console.Amber,
    onPrimary = Console.Ink,
    background = Console.Ink,
    onBackground = Console.Bright,
    surface = Console.Panel,
    onSurface = Console.Bright,
    surfaceVariant = Console.PanelDeep,
    onSurfaceVariant = Console.Dim,
    outline = Console.Edge,
    error = Console.Alarm,
)

@Composable
fun WardogsIdfTheme(content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = ConsoleColors,
        typography = Typography(
            displayLarge = base.displayLarge.copy(fontFamily = FontFamily.Monospace),
            displayMedium = base.displayMedium.copy(fontFamily = FontFamily.Monospace),
            displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Monospace),
            headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Monospace),
            headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Monospace),
            headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Monospace),
            titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Monospace),
            titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Monospace),
            titleSmall = base.titleSmall.copy(fontFamily = FontFamily.Monospace),
            bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            bodySmall = base.bodySmall.copy(fontFamily = FontFamily.Monospace),
            labelLarge = base.labelLarge.copy(fontFamily = FontFamily.Monospace),
            labelMedium = base.labelMedium.copy(fontFamily = FontFamily.Monospace),
            labelSmall = base.labelSmall.copy(fontFamily = FontFamily.Monospace),
        ),
        content = content,
    )
}
