package com.m3sv.plainupnp.presentation.onboarding.selecttheme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.m3sv.plainupnp.common.ThemeManager
import com.m3sv.plainupnp.common.util.finishApp
import com.m3sv.plainupnp.compose.AppTheme
import com.m3sv.plainupnp.compose.LifecycleIndicator
import com.m3sv.plainupnp.compose.util.isDarkTheme
import com.m3sv.plainupnp.interfaces.LifecycleManager
import com.m3sv.plainupnp.interfaces.LifecycleState
import com.m3sv.plainupnp.presentation.onboarding.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SelectThemeActivity : ComponentActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var lifecycleManager: LifecycleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentTheme by themeManager.theme.collectAsState()

            AppTheme(currentTheme.isDarkTheme()) {
                Surface {
                    SelectThemeScreen(
                        titleText = stringResource(R.string.set_theme_label),
                        buttonText = stringResource(id = R.string.done),
                        selectedTheme = currentTheme,
                        onThemeOptionSelected = themeManager::setTheme,
                        onClick = { finish() },
                        onBackClick = { finish() }
                    )

                    val lifecycleState: LifecycleState by lifecycleManager.lifecycleState.collectAsState()

                    LifecycleIndicator(lifecycleState = lifecycleState, ::finishApp)
                }
            }
        }
    }
}
