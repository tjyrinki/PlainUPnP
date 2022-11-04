package com.m3sv.plainupnp.compose.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.m3sv.plainupnp.common.ThemeOption

@Composable
fun ThemeOption.isDarkTheme(): Boolean = when (this) {
    ThemeOption.System -> isSystemInDarkTheme()
    else -> this == ThemeOption.Dark
}
