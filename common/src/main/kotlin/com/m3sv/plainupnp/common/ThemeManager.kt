package com.m3sv.plainupnp.common

import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    private val scope = MainScope()

    val theme: StateFlow<ThemeOption> = preferencesRepository.theme

    fun setTheme(mode: ThemeOption) {
        scope.launch { preferencesRepository.setApplicationTheme(mode) }
    }
}
