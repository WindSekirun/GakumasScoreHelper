package com.github.windsekirun.gakumasscorehelper.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TextFieldTableItem(
    index: Int,
    row: Pair<String, String>,
    onValueChange: (index: Int, value: String) -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = Color.Unspecified
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = row.first, color = labelColor)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            TextField(
                value = row.second,
                onValueChange = {
                    if (row.second != it) {
                        onValueChange(index, it)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                singleLine = true,
            )
        }
    }
}

@Preview
@Composable
fun TextFieldTablePreview() {
    Surface(modifier = Modifier.fillMaxWidth()) {
        TextFieldTableItem(
            index = 0,
            row = "Label 1" to "10000",
            onValueChange = { _, _ -> }
        )
    }
}