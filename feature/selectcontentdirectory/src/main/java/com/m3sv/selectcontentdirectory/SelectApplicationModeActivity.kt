package com.m3sv.selectcontentdirectory

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.m3sv.plainupnp.Router
import com.m3sv.plainupnp.common.ThemeManager
import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import com.m3sv.plainupnp.common.util.asApplicationMode
import com.m3sv.plainupnp.common.util.finishApp
import com.m3sv.plainupnp.compose.AppTheme
import com.m3sv.plainupnp.compose.LifecycleIndicator
import com.m3sv.plainupnp.compose.SelectApplicationModeScreen
import com.m3sv.plainupnp.compose.util.isDarkTheme
import com.m3sv.plainupnp.interfaces.LifecycleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelectApplicationModeActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var lifecycleManager: LifecycleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var modeChanged by remember { mutableStateOf(false) }

            val preferences by preferencesRepository.preferences.collectAsState()
            val currentTheme by themeManager.theme.collectAsState()

            AppTheme(currentTheme.isDarkTheme()) {
                Surface {
                    val clickListener = {
                        if (modeChanged)
                            toSplash()
                        else {
                            finish()
                        }
                    }

                    SelectApplicationModeScreen(
                        onNextClick = clickListener,
                        onBackClick = clickListener,
                        nextText = stringResource(R.string.done),
                        initialMode = preferences.applicationMode.asApplicationMode()
                    ) { applicationMode ->
                        modeChanged = true
                        lifecycleScope.launch { preferencesRepository.setApplicationMode(applicationMode) }
                    }


                    val lifecycleState by lifecycleManager.lifecycleState.collectAsState()

                    LifecycleIndicator(lifecycleState = lifecycleState, ::finishApp)
                }
            }
        }
    }

    private fun toSplash() {
        startActivity((application as Router).getSplashActivityIntent(this).apply {
            flags += Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
