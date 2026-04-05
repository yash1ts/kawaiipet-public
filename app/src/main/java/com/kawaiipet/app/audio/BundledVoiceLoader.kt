package com.kawaiipet.app.audio

import android.content.Context
import android.util.Log
import com.kawaiipet.app.util.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BundledVoiceLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: ModelManager,
    private val preferenceManager: PreferenceManager,
    private val audioPipeline: AudioPipeline
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun startWarmup() {
        scope.launch(Dispatchers.IO) {
            val ok = modelManager.ensureBundledModelsFromAssets()
            if (!ok) {
                Log.e(TAG, "Bundled voice assets missing or copy failed")
                return@launch
            }
            preferenceManager.setSttModelId(BundledVoiceModels.STT_MODEL_ID)
            preferenceManager.setTtsModelId(BundledVoiceModels.TTS_MODEL_ID)
            withContext(Dispatchers.Default) {
                val sttOk = audioPipeline.initializeSTT(BundledVoiceModels.STT_MODEL_ID)
                val ttsOk = audioPipeline.initializeTTS(BundledVoiceModels.TTS_MODEL_ID)
                Log.i(TAG, "Warmup: STT=$sttOk TTS=$ttsOk")
            }
        }
    }

    companion object {
        private const val TAG = "BundledVoiceLoader"
    }
}
