package com.m3sv.plainupnp.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.m3sv.plainupnp.common.ThemeManager
import com.m3sv.plainupnp.common.ThemeOption
import com.m3sv.plainupnp.common.preferences.Preferences
import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import com.m3sv.plainupnp.common.util.asApplicationMode
import com.m3sv.plainupnp.common.util.finishApp
import com.m3sv.plainupnp.common.util.pass
import com.m3sv.plainupnp.compose.AppTheme
import com.m3sv.plainupnp.compose.LifecycleIndicator
import com.m3sv.plainupnp.compose.OnePane
import com.m3sv.plainupnp.compose.OneTitle
import com.m3sv.plainupnp.compose.OneToolbar
import com.m3sv.plainupnp.compose.util.isDarkTheme
import com.m3sv.plainupnp.interfaces.LifecycleManager
import com.m3sv.plainupnp.presentation.onboarding.activity.ConfigureFolderActivity
import com.m3sv.plainupnp.presentation.onboarding.selecttheme.SelectThemeActivity
import com.m3sv.plainupnp.presentation.settings.ratehandler.RateHandler
import com.m3sv.selectcontentdirectory.SelectApplicationModeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var rateHandler: RateHandler

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var lifecycleManager: LifecycleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences by preferencesRepository.preferences.collectAsState()
            val currentTheme by themeManager.theme.collectAsState()

            AppTheme(currentTheme.isDarkTheme()) {
                Surface {
                    OnePane(viewingContent = {
                        OneTitle(stringResource(id = R.string.title_feature_settings))
                        OneToolbar(onBackClick = { finish() }) {}
                    }) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            ThemeSection(currentTheme)
                            ApplicationModeSection(preferences)
                            UpnpSection(preferences)
                            AboutSection()
                        }
                    }
                }

                val lifecycleState by lifecycleManager.lifecycleState.collectAsState()

                LifecycleIndicator(lifecycleState = lifecycleState, ::finishApp)
            }
        }
    }

    @Composable
    private fun ApplicationModeSection(preferences: Preferences) {
        Section {
            SectionRow(
                title = stringResource(id = R.string.application_mode_settings),
                currentValue = preferences
                    .applicationMode
                    .asApplicationMode()
                    .stringValue.let { stringResource(id = it) }
            ) {
                startActivity(Intent(applicationContext, SelectApplicationModeActivity::class.java))
            }
        }
    }

    @Composable
    fun AboutSection() {
        Section {
            SectionRow(
                title = stringResource(id = R.string.contact_us_title),
                currentValue = stringResource(id = R.string.contact_us_body),
                icon = painterResource(id = R.drawable.ic_baseline_email_green)
            ) {
                openActivity(::openEmail)
            }

            RowDivider()

            SectionRow(
                title = stringResource(id = R.string.rate),
                currentValue = stringResource(id = R.string.open_play_store),
                icon = painterResource(id = R.drawable.ic_play_store)
            ) {
                rateHandler.rate(this@SettingsActivity)
            }

            RowDivider()

            SectionRow(
                title = stringResource(id = R.string.github_link_title),
                currentValue = stringResource(id = R.string.source_url),
                painterResource(id = R.drawable.ic_github)
            ) {
                openActivity(::openGithub)
            }

            RowDivider()

            SectionRow(
                title = stringResource(id = R.string.privacy_policy),
                currentValue = stringResource(id = R.string.open_privacy_policy),
                icon = painterResource(id = R.drawable.ic_privacy_policy)
            ) {
                openActivity(::openPrivacyPolicy)
            }

            RowDivider()

            SectionRow(
                title = stringResource(id = R.string.version),
                currentValue = BuildConfig.APP_VERSION,
            ) {}
        }
    }

    @Composable
    private fun UpnpSection(preferences: Preferences) {
        Section {
            SectionRow(
                title = stringResource(R.string.selected_folders),
                icon = painterResource(id = R.drawable.ic_folder_24dp)
            ) {
                startActivity(Intent(applicationContext, ConfigureFolderActivity::class.java))
            }

            RowDivider()

            SwitchRow(
                title = stringResource(id = R.string.share_images),
                initialValue = preferences.enableImages,
                icon = painterResource(id = R.drawable.ic_image)
            ) { enabled ->
                lifecycleScope.launch {
                    preferencesRepository.setShareImages(enabled)
                }
            }

            RowDivider()

            SwitchRow(
                title = stringResource(id = R.string.share_videos),
                initialValue = preferences.enableVideos,
                icon = painterResource(id = R.drawable.ic_video)
            ) { enabled ->
                lifecycleScope.launch {
                    preferencesRepository.setShareVideos(enabled)
                }
            }

            RowDivider()

            SwitchRow(
                title = stringResource(id = R.string.share_music),
                initialValue = preferences.enableAudio,
                icon = painterResource(id = R.drawable.ic_music)
            ) { enabled ->
                lifecycleScope.launch {
                    preferencesRepository.setShareAudio(enabled)
                }
            }

            RowDivider()

            Row {
                Column {
                    SwitchRow(
                        title = stringResource(id = R.string.enable_thumbnails),
                        initialValue = preferences.enableThumbnails,
                        icon = painterResource(id = R.drawable.ic_thumbnail),
                    ) { enabled ->
                        lifecycleScope.launch {
                            preferencesRepository.setShowThumbnails(enabled)
                        }
                    }

                    Row(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))

                        Image(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text = stringResource(id = R.string.enable_thumbnails_note),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ThemeSection(themeOption: ThemeOption) {
        val textId = when (themeOption) {
            ThemeOption.System -> R.string.system_theme_label
            ThemeOption.Light -> R.string.light_theme_label
            ThemeOption.Dark -> R.string.dark_theme_label
            else -> error("Theme is not set")
        }

        Section {
            SectionRow(
                title = stringResource(R.string.set_theme_label),
                currentValue = stringResource(id = textId),
                icon = painterResource(id = R.drawable.ic_theme)
            ) {
                lifecycleScope.launch(Dispatchers.IO) {
                    startActivity(Intent(applicationContext, SelectThemeActivity::class.java))
                }
            }
        }
    }

    @Composable
    fun RowDivider() {
        Divider(Modifier.padding(start = 48.dp, end = 8.dp))
    }

    @Composable
    private fun Section(sectionContent: @Composable (ColumnScope.() -> Unit)) {
        Row {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(AppTheme.cornerRadius)
            ) {
                Column(content = sectionContent)
            }
        }
    }

    @Composable
    private fun SectionRow(
        title: String,
        currentValue: String? = null,
        icon: Painter? = null,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(Modifier.padding(start = if (icon != null) 16.dp else 4.dp)) {
                Text(title)
                if (currentValue != null) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            text = currentValue,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SwitchRow(
        title: String,
        initialValue: Boolean,
        icon: Painter? = null,
        onSwitch: (Boolean) -> Unit
    ) {
        val checkedState = remember { mutableStateOf(initialValue) }

        fun flipSwitch() {
            checkedState.value = !checkedState.value
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    flipSwitch()
                    onSwitch(checkedState.value)
                }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    Modifier.size(24.dp)
                )
            }

            Row(Modifier.padding(start = if (icon != null) 16.dp else 4.dp)) {
                Text(title)
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = checkedState.value, onCheckedChange = {
                    checkedState.value = it
                    onSwitch(it)
                })
            }
        }
    }

    private fun openEmail() {
        val email = getString(R.string.dev_email)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("${getString(R.string.mail_to)}$email")
        }

        startActivity(intent)
    }

    private fun openPrivacyPolicy() = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(getString(R.string.privacy_policy_link))
    ).also(::startActivity)

    private fun openGithub() =
        Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link))).also(::startActivity)

    private fun openActivity(block: () -> Unit) {
        try {
            block()
        } catch (e: ActivityNotFoundException) {
            pass
        }
    }
}
