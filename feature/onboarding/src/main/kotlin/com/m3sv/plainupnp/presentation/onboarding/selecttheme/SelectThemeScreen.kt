package com.m3sv.plainupnp.presentation.onboarding.selecttheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m3sv.plainupnp.common.ThemeOption
import com.m3sv.plainupnp.compose.OneContainedButton
import com.m3sv.plainupnp.compose.OnePane
import com.m3sv.plainupnp.compose.OneSubtitle
import com.m3sv.plainupnp.compose.OneTitle
import com.m3sv.plainupnp.compose.OneToolbar
import com.m3sv.plainupnp.compose.RadioGroup

@Composable
fun SelectThemeScreen(
    titleText: String,
    buttonText: String,
    selectedTheme: ThemeOption,
    onThemeOptionSelected: (ThemeOption) -> Unit,
    onClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    OnePane(viewingContent = {
        OneTitle(text = titleText)
        OneToolbar(onBackClick = onBackClick) {}
    }) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OneSubtitle(
                text = "Start by selecting theme that you would like to use",
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            RadioGroup(
                modifier = Modifier.padding(start = 24.dp),
                items = ThemeOption.values().toList(),
                initial = selectedTheme,
                stringProvider = { stringResource(id = it.text) },
                onItemSelected = onThemeOptionSelected
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                OneContainedButton(text = buttonText, onClick = onClick)
            }
        }
    }
}

