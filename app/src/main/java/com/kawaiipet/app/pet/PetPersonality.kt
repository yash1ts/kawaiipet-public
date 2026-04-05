package com.kawaiipet.app.pet

data class PetPersonality(
    val name: String = "Mochi",
    val personalityPrompt: String = DEFAULT_PROMPT,
    val emotionDurationMs: Long = 3000L,
    val maxResponseLength: Int = 200
) {
    companion object {
        const val DEFAULT_PROMPT =
            "You are a cute, friendly virtual pet. You speak in a warm, playful tone. " +
            "You remember things the user tells you and bring them up naturally. " +
            "Keep responses concise (1-3 sentences). " +
            "End every response with an emotion tag: [happy], [sad], [thinking], or [idle]."
    }
}
