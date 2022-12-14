package com.m3sv.plainupnp.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

@Composable
fun <T> RadioGroup(
    modifier: Modifier = Modifier,
    items: List<T>,
    initial: T,
    stringProvider: @Composable (T) -> String,
    onItemSelected: (T) -> Unit,
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(initial) }

    val onClick: (T) -> Unit = { themeOption ->
        onOptionSelected(themeOption)
        onItemSelected(themeOption)
    }

    Column {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = (item == selectedOption),
                        onClick = { onClick(item) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The [clearAndSetSemantics] causes the button's redundant
                // selectable semantics to be cleared in favor of the [Row]
                // selectable's, to improve usability with screen-readers.
                Box(Modifier.clearAndSetSemantics {}) {
                    RadioButton(
                        selected = (item == selectedOption),
                        onClick = { onClick(item) },
                        modifier = modifier
                    )
                }
                Text(
                    text = stringProvider(item),
                    style = MaterialTheme.typography.body1.merge(),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
