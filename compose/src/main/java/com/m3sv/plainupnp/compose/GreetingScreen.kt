package com.m3sv.plainupnp.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GreetingScreen(onClick: () -> Unit) {
    OnePane(viewingContent = {
        OneTitle("Welcome to PlainUPnP")
    }) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            OneSubtitle("Let's walk you through the onboarding process")
            Spacer(Modifier.weight(1f))
            OneContainedButton(text = "Sure", onClick = onClick)
        }
    }
}
