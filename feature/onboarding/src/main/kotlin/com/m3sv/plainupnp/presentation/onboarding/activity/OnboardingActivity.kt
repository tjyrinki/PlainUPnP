package com.m3sv.plainupnp.presentation.onboarding.activity

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.m3sv.plainupnp.common.ApplicationMode
import com.m3sv.plainupnp.common.ThemeManager
import com.m3sv.plainupnp.common.ThemeOption
import com.m3sv.plainupnp.common.util.pass
import com.m3sv.plainupnp.compose.AppTheme
import com.m3sv.plainupnp.compose.GreetingScreen
import com.m3sv.plainupnp.compose.SelectApplicationModeScreen
import com.m3sv.plainupnp.compose.util.isDarkTheme
import com.m3sv.plainupnp.data.upnp.UriWrapper
import com.m3sv.plainupnp.presentation.onboarding.OnboardingManager
import com.m3sv.plainupnp.presentation.onboarding.OnboardingScreen
import com.m3sv.plainupnp.presentation.onboarding.OnboardingViewModel
import com.m3sv.plainupnp.presentation.onboarding.R
import com.m3sv.plainupnp.presentation.onboarding.screen.SelectFoldersScreen
import com.m3sv.plainupnp.presentation.onboarding.screen.SelectPreconfiguredContainersScreen
import com.m3sv.plainupnp.presentation.onboarding.screen.StoragePermissionScreen
import com.m3sv.plainupnp.presentation.onboarding.selecttheme.SelectThemeScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    @Inject
    lateinit var onboardingManager: OnboardingManager

    @Inject
    lateinit var themeManager: ThemeManager

    private var onPermissionResult by mutableStateOf(-1)

    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LayoutContainer() }
    }

    override fun onBackPressed() {
        viewModel.onNavigateBack()
    }

    @Composable
    private fun LayoutContainer() {
        when (onPermissionResult) {
            0 -> {
                viewModel.onNavigateNext()
                onPermissionResult = -1
            }
            // TODO handle permission denied
            1 -> onPermissionResult = -1
            else -> pass
        }

        val contentUris by viewModel.contentUris.collectAsState()
        val currentScreen by viewModel.currentScreen.collectAsState()
        val currentTheme by themeManager.theme.collectAsState()

        val imageContainerEnabled = remember { viewModel.imageContainerEnabled }
        val videoContainerEnabled = remember { viewModel.videoContainerEnabled }
        val audioContainerEnabled = remember { viewModel.audioContainerEnabled }
        val pauseInBackground = remember { viewModel.pauseInBackground }

        Content(
            currentTheme = currentTheme,
            currentScreen = currentScreen,
            onSelectTheme = viewModel::onSelectTheme,
            onSelectApplicationMode = viewModel::onSelectMode,
            contentUris = contentUris,
            onNextClick = viewModel::onNavigateNext,
            onBackClick = viewModel::onNavigateBack,
            imageContainerEnabled = imageContainerEnabled,
            videoContainerEnabled = videoContainerEnabled,
            audioContainerEnabled = audioContainerEnabled,
            backgroundMode = pauseInBackground
        )
    }

    @Composable
    private fun Content(
        currentTheme: ThemeOption,
        currentScreen: OnboardingScreen,
        contentUris: List<UriWrapper> = listOf(),
        onSelectTheme: (ThemeOption) -> Unit,
        onSelectApplicationMode: (ApplicationMode) -> Unit,
        onNextClick: () -> Unit,
        onBackClick: () -> Unit,
        imageContainerEnabled: MutableState<Boolean>,
        videoContainerEnabled: MutableState<Boolean>,
        audioContainerEnabled: MutableState<Boolean>,
        backgroundMode: MutableState<Boolean>,
    ) {
        AppTheme(currentTheme.isDarkTheme()) {
            Crossfade(targetState = currentScreen) { screen ->
                Surface {
                    when (screen) {
                        OnboardingScreen.Greeting -> GreetingScreen(onNextClick)

                        OnboardingScreen.SelectTheme -> SelectThemeScreen(
                            titleText = stringResource(R.string.set_theme_label),
                            buttonText = stringResource(id = R.string.next),
                            selectedTheme = currentTheme,
                            onThemeOptionSelected = onSelectTheme,
                            onClick = onNextClick,
                            onBackClick = onBackClick
                        )

                        OnboardingScreen.SelectMode -> SelectApplicationModeScreen(
                            initialMode = ApplicationMode.Streaming,
                            onNextClick = onNextClick,
                            onBackClick = onBackClick,
                            onItemSelected = onSelectApplicationMode
                        )

                        OnboardingScreen.StoragePermission -> StoragePermissionScreen(onBackClick = onBackClick) {
                            checkStoragePermission(onNextClick)
                        }

                        OnboardingScreen.SelectPreconfiguredContainers -> SelectPreconfiguredContainersScreen(
                            onBackClick = onBackClick,
                            onNextClick = onNextClick,
                            imageEnabled = imageContainerEnabled,
                            videoEnabled = videoContainerEnabled,
                            audioEnabled = audioContainerEnabled
                        )

                        OnboardingScreen.SelectDirectories -> SelectFoldersScreen(
                            contentUris = contentUris,
                            selectDirectory = ::openDirectory,
                            onBackClick = onBackClick,
                            onNext = onNextClick,
                            onReleaseUri = viewModel::releaseUri,
                        )
                        OnboardingScreen.Finish -> finishOnboarding()
                    }
                }
            }
        }
    }

    private fun finishOnboarding() {
        onboardingManager.completeOnboarding(this)
    }

    private fun openDirectory() {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            // Provide read access to files and sub-directories in the user-selected
            // directory.
            flags = DIRECTORY_PERMISSIONS
        }

        startActivityForResult(intent, REQUEST_DIRECTORY_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_READ_EXTERNAL_STORAGE -> {
                onPermissionResult =
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        0
                    } else {
                        1
                    }
            }
        }
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

    private fun checkStoragePermission(onPermissionGranted: () -> Unit) {
        when {
            viewModel.hasStoragePermission() -> onPermissionGranted()
            ActivityCompat.shouldShowRequestPermissionRationale(this, OnboardingViewModel.STORAGE_PERMISSION)
            -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                // TODO explain why we need storage permission
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            else -> {
                requestReadStoragePermission()
            }
        }
    }

    private fun requestReadStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!viewModel.hasStoragePermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(OnboardingViewModel.STORAGE_PERMISSION),
                    REQUEST_READ_EXTERNAL_STORAGE
                )
            }
        }
    }

    companion object {
        private const val REQUEST_READ_EXTERNAL_STORAGE = 12345
        private const val REQUEST_DIRECTORY_CODE = 12
        private const val DIRECTORY_PERMISSIONS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION and FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    }
}
