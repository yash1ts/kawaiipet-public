package com.kawaiipet.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.kawaiipet.app.pet.PetAnimationController

private const val PET_SIZE = 88

@Composable
fun PetOverlay(
    animationController: PetAnimationController,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDoubleTap: () -> Unit
) {
    val expression by animationController.expression.collectAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PET_SIZE.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        val ovalShape = GenericShape { size, _ ->
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
        Box(
            modifier = Modifier
                .size(width = (PET_SIZE * 0.7f).dp, height = (PET_SIZE * 0.18f).dp)
                .offset(y = (PET_SIZE * 0.46f).dp)
                .blur(6.dp)
                .background(Color.Black.copy(alpha = 0.18f), ovalShape)
        )

        val composition by rememberLottieComposition(
            LottieCompositionSpec.Asset(expression.lottieAsset)
        )

        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(PET_SIZE.dp),
            alignment = Alignment.BottomCenter,
            contentScale = ContentScale.Fit,
            clipToCompositionBounds = false
        )
    }
}
