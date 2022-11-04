package com.m3sv.plainupnp.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun OneContainedButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = modifier.fillMaxWidth(0.75f),
            onClick = onClick,
            shape = RoundedCornerShape(AppTheme.cornerRadius),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.button.copy(fontSize = 16.sp)
            )
        }
    }
}
