package com.example.diywallpaper.ui.components.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.domain.model.design.EditorFontOption
import com.example.diywallpaper.ui.feature.editor.editorFontFamily

@Composable
fun EditorFontSampleRow(
    fonts: List<EditorFontOption>,
    selectedFontId: String,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        fonts.forEach { font ->
            val selected = font.id == selectedFontId
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onFontSelected(font.id) }
            ) {
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = editorFontFamily(font.id),
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                    }
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(if (selected) 4.dp else 0.dp)
                        .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                )
            }
        }
    }
}
