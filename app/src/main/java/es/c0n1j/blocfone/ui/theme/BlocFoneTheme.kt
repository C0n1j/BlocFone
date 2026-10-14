package es.c0n1j.blocfone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFC23A45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF410009),
    secondary = Color(0xFF17598C),
    onSecondary = Color.White,
    background = Color(0xFFF3F0EE),
    onBackground = Color(0xFF211A1B),
    surface = Color(0xFFF3F0EE),
    onSurface = Color(0xFF211A1B),
    surfaceContainer = Color(0xFFDAD3D0),
    surfaceContainerHigh = Color(0xFFD2CAC7),
    surfaceVariant = Color(0xFFE1D8D5),
    onSurfaceVariant = Color(0xFF554746),
    outline = Color(0xFF887270),
    outlineVariant = Color(0xFFCFC3C0),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3B3),
    onPrimary = Color(0xFF680018),
    primaryContainer = Color(0xFF920024),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFA5CAEF),
    onSecondary = Color(0xFF003352),
    background = Color(0xFF151214),
    surface = Color(0xFF151214),
    surfaceVariant = Color(0xFF524344),
)

@Composable
fun BlocFoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
