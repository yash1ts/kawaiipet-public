package com.kawaiipet.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kawaiipet_prefs")

class PreferenceManager(private val context: Context) {

    val apiKey: Flow<String> = context.dataStore.data.map { it[Keys.API_KEY] ?: "" }
    val petName: Flow<String> = context.dataStore.data.map { it[Keys.PET_NAME] ?: "Mochi" }
    val modelName: Flow<String> = context.dataStore.data.map { it[Keys.MODEL_NAME] ?: "gemini-1.5-flash" }
    val sttModelId: Flow<String> = context.dataStore.data.map { it[Keys.STT_MODEL_ID] ?: "" }
    val ttsModelId: Flow<String> = context.dataStore.data.map { it[Keys.TTS_MODEL_ID] ?: "" }
    val personalityPrompt: Flow<String> = context.dataStore.data.map {
        it[Keys.PERSONALITY] ?: DEFAULT_PERSONALITY
    }

    suspend fun getApiKey(): String = apiKey.first()
    suspend fun getPetName(): String = petName.first()
    suspend fun getModelName(): String = modelName.first()

    suspend fun getSttModelId(): String = sttModelId.first()

    suspend fun getTtsModelId(): String = ttsModelId.first()

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { it[Keys.API_KEY] = value }
    }

    suspend fun setPetName(value: String) {
        context.dataStore.edit { it[Keys.PET_NAME] = value }
    }

    suspend fun setModelName(value: String) {
        context.dataStore.edit { it[Keys.MODEL_NAME] = value }
    }

    suspend fun setSttModelId(value: String) {
        context.dataStore.edit { it[Keys.STT_MODEL_ID] = value }
    }

    suspend fun setTtsModelId(value: String) {
        context.dataStore.edit { it[Keys.TTS_MODEL_ID] = value }
    }

    suspend fun setPersonalityPrompt(value: String) {
        context.dataStore.edit { it[Keys.PERSONALITY] = value }
    }

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val PET_NAME = stringPreferencesKey("pet_name")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val STT_MODEL_ID = stringPreferencesKey("stt_model_id")
        val TTS_MODEL_ID = stringPreferencesKey("tts_model_id")
        val PERSONALITY = stringPreferencesKey("personality")
    }

    companion object {
        const val DEFAULT_PERSONALITY =
            "You are a cute, friendly virtual pet. You speak in a warm, playful tone. " +
            "You remember things the user tells you and bring them up naturally. " +
            "Keep responses concise (1-3 sentences). " +
            "End every response with an emotion tag: [happy], [sad], [thinking], or [idle]."
    }
}
