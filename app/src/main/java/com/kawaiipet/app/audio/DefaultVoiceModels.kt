package com.kawaiipet.app.audio

/**
 * Default STT/TTS model IDs when the user has not chosen others. Weights are **not** shipped in the APK;
 * users download compatible Sherpa-ONNX packs into app storage (same IDs as k2-fsa release archives).
 */
object DefaultVoiceModels {
    const val STT_MODEL_ID = "moonshine-tiny-en-quantized"
    const val TTS_MODEL_ID = "kitten-nano-en-v0_2-fp16"
}
