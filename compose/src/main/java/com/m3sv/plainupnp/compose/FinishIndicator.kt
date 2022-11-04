package com.m3sv.plainupnp.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FinishIndicator() {
    Text(
        text = "The application is finishing",
        textAlign = TextAlign.Center,
        style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        modifier = Modifier.padding(16.dp)
    )

    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
}

@Composable
fun CloseIndicator(onCloseClick: () -> Unit) {
    Text(
        text = "The application has been finished",
        textAlign = TextAlign.Center,
        style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        modifier = Modifier.padding(16.dp)
    )

    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.10f))
    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        onClick = onCloseClick
    ) {
        Text("Close", fontSize = 16.sp)
    }
}
