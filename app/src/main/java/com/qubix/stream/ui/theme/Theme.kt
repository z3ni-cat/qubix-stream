package com.qubix.stream.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QubixViolet = Color(0xFF8B7CFF)
private val QubixBg = Color(0xFF0B0B14)
private val QubixSurface = Color(0xFF141422)
private val QubixOnBg = Color(0xFFEDEDF5)

private val DarkScheme = darkColorScheme(
    primary = QubixViolet,
    onPrimary = Color.White,
    background = QubixBg,
    onBackground = QubixOnBg,
    surface = QubixSurface,
    onSurface = QubixOnBg,
    surfaceVariant = Color(0xFF1E1E30),
    secondary = Color(0xFF6EE7D8)
)

@Composable
fun QubixStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
