package com.kawaiipet.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kawaiipet.app.R
import com.kawaiipet.app.ui.theme.KawaiiPetTheme
import com.kawaiipet.app.ui.theme.SlimeWater

@Composable
fun OverlayCloseDragHint() {
    KawaiiPetTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlimeWater.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.overlay_release_to_close),
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
