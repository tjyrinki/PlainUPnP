package com.m3sv.plainupnp.compose

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val primaryColor = Color(0xFF81c784)

private val lightColors = lightColors(
    primary = primaryColor,
    secondary = primaryColor,
    secondaryVariant = primaryColor
)

private val darkColors = darkColors(
    primary = primaryColor,
    secondary = primaryColor,
    secondaryVariant = primaryColor
)

@Composable
fun AppTheme(isDarkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colors = if (isDarkTheme) darkColors else lightColors,
        content = content
    )
}

object AppTheme {
    val cornerRadius = 16.dp
}
