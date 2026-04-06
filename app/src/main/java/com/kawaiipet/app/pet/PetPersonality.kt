package com.kawaiipet.app.pet

import com.kawaiipet.app.llm.LlmPromptDefaults

data class PetPersonality(
    val name: String = "Mochi",
    val personalityPrompt: String = LlmPromptDefaults.DEFAULT_PERSONALITY,
    val emotionDurationMs: Long = 3000L,
    val maxResponseLength: Int = 200
)
