package com.github.windsekirun.gakumasscorehelper.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TitleAndDesc(title: String, desc: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Text(
            desc,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Preview
@Composable
fun TitleAndDescPreview() {
    TitleAndDesc(title = "Title", desc = "Description")
}