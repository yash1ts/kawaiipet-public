package com.kawaiipet.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kawaiipet.app.ui.theme.SlimeWaterDeepDark

/**
 * Live speech subtitles while the mic is open (partial STT or platform recognizer).
 */
@Composable
fun ListeningSubtitle(
    text: String,
    isPlaceholder: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = if (isPlaceholder) {
            MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFB3E5FC).copy(alpha = 0.85f)
            )
        } else {
            MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE1F5FE))
        },
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .background(SlimeWaterDeepDark.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp)
    )
}
