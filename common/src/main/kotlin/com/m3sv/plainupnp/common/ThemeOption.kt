package com.m3sv.plainupnp.common

import androidx.annotation.StringRes

enum class ThemeOption(@StringRes val text: Int) {
    System(text = R.string.system_theme_label),
    Light(text = R.string.light_theme_label),
    Dark(text = R.string.dark_theme_label);
}
