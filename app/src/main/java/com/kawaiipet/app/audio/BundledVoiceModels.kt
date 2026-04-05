package com.kawaiipet.app.audio

/**
 * Voice weights are copied from APK assets into app storage on first launch (or after a revision bump).
 *
 * Sherpa-ONNX requires its own ONNX export (merged Moonshine decoder, Kitten metadata + raw [voices.bin]).
 * The Transformers.js Moonshine int8 files on Hugging Face
 * ([onnx-community/moonshine-tiny-ONNX](https://huggingface.co/onnx-community/moonshine-tiny-ONNX/tree/main/onnx))
 * and the KittenML 0.8 int8 ONNX2 graph
 * ([KittenML/kitten-tts-nano-0.8-int8](https://huggingface.co/KittenML/kitten-tts-nano-0.8-int8))
 * are not loadable by this app’s Sherpa runtime, so we bundle the **smallest Sherpa-compatible** builds:
 * Moonshine Tiny EN **quantized** (dynamic int8-style weights) and Kitten Nano EN **v0.2** (smaller than v0.1).
 */
object BundledVoiceModels {
    const val STT_MODEL_ID = "moonshine-tiny-en-quantized"
    const val TTS_MODEL_ID = "kitten-nano-en-v0_2-fp16"

    const val ASSET_REVISION_ASSET_PATH = "voice_models/REVISION"
    const val ASSET_STT_DIR = "voice_models/stt"
    const val ASSET_TTS_DIR = "voice_models/tts/kitten-nano-en-v0_2-fp16"

    /** Bump when replacing files under [ASSET_STT_DIR] / [ASSET_TTS_DIR]. */
    const val EXPECTED_ASSET_REVISION = "1"
}
