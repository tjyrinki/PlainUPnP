package com.m3sv.plainupnp.presentation.onboarding.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.m3sv.plainupnp.common.ThemeManager
import com.m3sv.plainupnp.common.util.finishApp
import com.m3sv.plainupnp.common.util.pass
import com.m3sv.plainupnp.compose.ActivityNotFoundIndicator
import com.m3sv.plainupnp.compose.AppTheme
import com.m3sv.plainupnp.compose.FadedBackground
import com.m3sv.plainupnp.compose.LifecycleIndicator
import com.m3sv.plainupnp.compose.util.isDarkTheme
import com.m3sv.plainupnp.data.upnp.UriWrapper
import com.m3sv.plainupnp.interfaces.LifecycleManager
import com.m3sv.plainupnp.interfaces.LifecycleState
import com.m3sv.plainupnp.presentation.onboarding.ActivityNotFoundIndicatorState
import com.m3sv.plainupnp.presentation.onboarding.OnboardingViewModel
import com.m3sv.plainupnp.presentation.onboarding.screen.SelectFoldersScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ConfigureFolderActivity : ComponentActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var lifecycleManager: LifecycleManager

    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val contentUris: List<UriWrapper> by viewModel.contentUris.collectAsState()
            val theme by themeManager.theme.collectAsState()
            val activityNotFound: ActivityNotFoundIndicatorState by viewModel.activityNotFound.collectAsState()
            val lifecycleState: LifecycleState by lifecycleManager.lifecycleState.collectAsState()

            AppTheme(theme.isDarkTheme()) {
                Surface {
                    SelectFoldersScreen(
                        contentUris = contentUris,
                        selectDirectory = { openDirectory() },
                        onReleaseUri = { viewModel.releaseUri(it) },
                        onBackClick = { finish() }
                    )

                    LifecycleIndicator(lifecycleState = lifecycleState, ::finishApp)

                    Crossfade(targetState = activityNotFound) { state ->
                        when (state) {
                            ActivityNotFoundIndicatorState.SHOW -> FadedBackground {
                                ActivityNotFoundIndicator {
                                    viewModel.dismissActivityNotFound()
                                }
                            }
                            ActivityNotFoundIndicatorState.DISMISS -> pass
                        }
                    }
                }
            }
        }
    }

    private fun openDirectory() {
        runCatching {// Choose a directory using the system's file picker.
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                // Provide read access to files and sub-directories in the user-selected
                // directory.
                flags = DIRECTORY_PERMISSIONS
            }

            startActivityForResult(intent, REQUEST_DIRECTORY_CODE)
        }.onFailure { viewModel.onActivityNotFound() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_DIRECTORY_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.also { uri ->
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        viewModel.saveUri()
                    }
                }
            }
        }
    }

    companion object {
        private const val REQUEST_DIRECTORY_CODE = 12
        private const val DIRECTORY_PERMISSIONS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    }
}
