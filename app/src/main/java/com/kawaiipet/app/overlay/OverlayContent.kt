package com.kawaiipet.app.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kawaiipet.app.pet.PetAnimationController
import com.kawaiipet.app.pet.PetViewModel
import com.kawaiipet.app.ui.theme.KawaiiPetTheme

/** Space reserved above the pet so bubbles/text never change total layout height (pet stays fixed). */
private val OverlayAbovePetHeight = 220.dp

@Composable
fun OverlayContent(
    petViewModel: PetViewModel,
    animationController: PetAnimationController,
    onDrag: (Float, Float) -> Unit,
    onPetDragStart: () -> Unit = {},
    onPetDragEnd: () -> Unit = {},
    onRequestFocus: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val state by petViewModel.overlayState.collectAsState()
    val responseText by petViewModel.currentResponse.collectAsState()
    val listeningSubtitle by petViewModel.listeningSubtitle.collectAsState()

    KawaiiPetTheme(dynamicColor = false) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OverlayAbovePetHeight),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(
                        visible = state is OverlayState.Listening || state is OverlayState.PreparingVoice,
                        enter = fadeIn(animationSpec = tween(120)) + slideInVertically(animationSpec = tween(180)) { -it },
                        exit = fadeOut(animationSpec = tween(100)) + slideOutVertically(animationSpec = tween(140)) { -it }
                    ) {
                        ListeningSubtitle(
                            text = if (listeningSubtitle.isBlank()) {
                                if (state is OverlayState.PreparingVoice) "Loading voice model…" else "Listening…"
                            } else listeningSubtitle,
                            isPlaceholder = listeningSubtitle.isBlank(),
                            modifier = Modifier.widthIn(max = 220.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = state is OverlayState.Speaking || responseText.isNotEmpty(),
                        enter = fadeIn(animationSpec = tween(120)) + slideInVertically(animationSpec = tween(180)) { -it },
                        exit = fadeOut(animationSpec = tween(100)) + slideOutVertically(animationSpec = tween(140)) { -it }
                    ) {
                        ChatBubble(
                            text = responseText,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = state is OverlayState.TextInput,
                        enter = fadeIn(animationSpec = tween(120)) + slideInVertically(animationSpec = tween(180)) { it },
                        exit = fadeOut(animationSpec = tween(100)) + slideOutVertically(animationSpec = tween(140)) { it }
                    ) {
                        TextInputOverlay(
                            onSubmit = { text ->
                                onRequestFocus(false)
                                petViewModel.onTextSubmitted(text)
                            },
                            onDismiss = {
                                onRequestFocus(false)
                                petViewModel.dismissTextInput()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            PetOverlay(
                animationController = animationController,
                onTap = { petViewModel.onPetTapped() },
                onLongPress = {
                    onRequestFocus(true)
                    petViewModel.onPetLongPressed()
                },
                onDrag = onDrag,
                onDragStart = onPetDragStart,
                onDragEnd = onPetDragEnd,
                onDoubleTap = onDismiss
            )
        }
    }
}
