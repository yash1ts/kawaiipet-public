package com.kawaiipet.app.llm

interface LlmService {
    suspend fun chat(messages: List<ChatMessage>, systemPrompt: String): String
    suspend fun extractFacts(conversationSnippet: String): List<String>
}

data class ChatMessage(
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Role { USER, ASSISTANT }
