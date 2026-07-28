package es.c0n1j.blocfone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF005D52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2E2),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4B635D),
    background = Color(0xFFF7F7F2),
    surface = Color(0xFFF7F7F2),
    surfaceVariant = Color(0xFFDCE5E1),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D5C6),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005047),
    secondary = Color(0xFFB3CCC5),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
    surfaceVariant = Color(0xFF3F4946),
)

@Composable
fun BlocFoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
