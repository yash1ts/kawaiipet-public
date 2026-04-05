package com.kawaiipet.app.llm

interface LlmService {
    /** [factTexts] are local-memory snippets only; the server merges them into the system prompt. */
    suspend fun chat(messages: List<ChatMessage>, factTexts: List<String>): String
    suspend fun extractFacts(conversationSnippet: String): List<String>
}

data class ChatMessage(
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Role { USER, ASSISTANT }
