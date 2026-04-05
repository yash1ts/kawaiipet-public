package com.kawaiipet.app.llm

import com.kawaiipet.app.memory.FactMatcher
import com.kawaiipet.app.memory.MemoryRepository
import com.kawaiipet.app.memory.ShortTermMemory
import com.kawaiipet.app.pet.PetExpression
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationManager @Inject constructor(
    private val llmService: LlmService,
    private val shortTermMemory: ShortTermMemory,
    private val memoryRepository: MemoryRepository,
    private val factExtractor: FactExtractor,
    private val factMatcher: FactMatcher,
    private val promptBuilder: PromptBuilder
) {
    data class LlmResponse(val text: String, val expression: PetExpression)

    suspend fun processUserInput(text: String): LlmResponse {
        shortTermMemory.addMessage(ChatMessage(Role.USER, text))

        val keywords = factMatcher.extractKeywords(text)
        val relevantFacts = memoryRepository.findFactsByKeywords(keywords)

        relevantFacts.forEach { fact ->
            memoryRepository.touchFact(fact.id)
        }

        val systemPrompt = promptBuilder.build(relevantFacts)
        val messages = shortTermMemory.getMessages()

        val rawResponse = llmService.chat(messages, systemPrompt)
        val (cleanText, expression) = parseEmotionTag(rawResponse)

        shortTermMemory.addMessage(ChatMessage(Role.ASSISTANT, cleanText))

        factExtractor.extractAndStoreAsync(text, cleanText)

        return LlmResponse(cleanText, expression)
    }

    fun clearConversation() {
        shortTermMemory.clear()
    }

    companion object {
        private val EMOTION_TAG_REGEX = "\\[(happy|sad|angry|thinking|idle|listening|talking|sleeping)\\]"
            .toRegex(RegexOption.IGNORE_CASE)

        fun parseEmotionTag(response: String): Pair<String, PetExpression> {
            val match = EMOTION_TAG_REGEX.findAll(response).lastOrNull()
            val expression = match?.groupValues?.get(1)?.let { PetExpression.fromTag(it) }
                ?: PetExpression.HAPPY
            val cleanText = response.replace(EMOTION_TAG_REGEX, "").trim()
            return cleanText to expression
        }
    }
}
