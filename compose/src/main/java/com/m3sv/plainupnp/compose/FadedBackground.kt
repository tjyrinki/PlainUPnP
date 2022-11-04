package com.m3sv.plainupnp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FadedBackground(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.onBackground.copy(alpha = 0.5f))
            .fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(AppTheme.cornerRadius)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                content()
            }
        }
    }
}
