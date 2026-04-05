package com.kawaiipet.app.llm

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.kawaiipet.app.util.PreferenceManager
import kotlinx.coroutines.flow.first

class GeminiLlmService(
    private val preferenceManager: PreferenceManager
) : LlmService {

    private var cachedApiKey: String = ""
    private var cachedModelName: String = ""
    private var generativeModel: GenerativeModel? = null

    private suspend fun getModel(): GenerativeModel {
        val apiKey = preferenceManager.apiKey.first()
        val modelName = preferenceManager.modelName.first()

        if (apiKey.isBlank()) throw IllegalStateException("Gemini API key not configured")

        if (apiKey != cachedApiKey || modelName != cachedModelName || generativeModel == null) {
            cachedApiKey = apiKey
            cachedModelName = modelName
            generativeModel = GenerativeModel(
                modelName = modelName.ifBlank { "gemini-1.5-flash" },
                apiKey = apiKey
            )
        }
        return generativeModel!!
    }

    override suspend fun chat(messages: List<ChatMessage>, systemPrompt: String): String {
        val apiKey = preferenceManager.apiKey.first()
        val modelName = preferenceManager.modelName.first()

        if (apiKey.isBlank()) throw IllegalStateException("Gemini API key not configured")

        val chatModel = GenerativeModel(
            modelName = modelName.ifBlank { "gemini-1.5-flash" },
            apiKey = apiKey,
            systemInstruction = content { text(systemPrompt) }
        )

        val history = messages.dropLast(1).map { msg ->
            content(role = if (msg.role == Role.USER) "user" else "model") {
                text(msg.text)
            }
        }

        val chat = chatModel.startChat(history = history)
        val lastMessage = messages.lastOrNull()
            ?: throw IllegalArgumentException("No messages provided")

        val response = chat.sendMessage(lastMessage.text)
        return response.text ?: ""
    }

    override suspend fun extractFacts(conversationSnippet: String): List<String> {
        val model = getModel()
        val prompt = buildString {
            appendLine("You are a fact extractor. Read the conversation below and list only concrete personal facts about the user (name, preferences, hobbies, relationships, etc.).")
            appendLine("Rules:")
            appendLine("- One fact per line, written as a short plain-English sentence.")
            appendLine("- Do NOT output any labels, tags, headers, or meta-text (no \"output:\", \"instruction:\", \"thought\", etc.).")
            appendLine("- If there are no personal facts, respond with exactly: NONE")
            appendLine()
            appendLine("Conversation:")
            appendLine(conversationSnippet)
        }

        val response = model.generateContent(prompt)
        return (response.text ?: "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank()
                    && !it.startsWith("#")
                    && !it.startsWith("```")
                    && !it.equals("NONE", ignoreCase = true) }
    }
}
