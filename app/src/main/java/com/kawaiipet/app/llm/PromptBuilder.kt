package com.kawaiipet.app.llm

import com.kawaiipet.app.memory.db.FactEntity
import com.kawaiipet.app.util.PreferenceManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptBuilder @Inject constructor(
    private val preferenceManager: PreferenceManager
) {
    suspend fun build(relevantFacts: List<FactEntity>): String = buildString {
        val petName = preferenceManager.getPetName()
        val personality = preferenceManager.personalityPrompt.first()

        appendLine("Your name is $petName.")
        appendLine()
        appendLine(personality)

        if (relevantFacts.isNotEmpty()) {
            appendLine()
            appendLine("Things you remember about the user:")
            relevantFacts.forEach { fact ->
                appendLine("- ${fact.factText}")
            }
        }

        appendLine()
        appendLine("IMPORTANT: End every response with exactly one emotion tag: [happy], [sad], [thinking], or [idle].")
        appendLine("Place the tag at the very end of your message.")
    }
}
