package com.kawaiipet.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.kawaiipet.app.ui.theme.SlimeBubbleSurface
import com.kawaiipet.app.ui.theme.SlimeTextOnBubble
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ChatBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    if (text.isBlank()) return

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(SlimeBubbleSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = SlimeTextOnBubble,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
    }
}
