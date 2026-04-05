package com.kawaiipet.app.llm

import com.kawaiipet.app.BuildConfig
import com.kawaiipet.app.auth.awaitReady
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseLlmService @Inject constructor(
    private val supabase: SupabaseClient,
) : LlmService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun chat(messages: List<ChatMessage>, factTexts: List<String>): String {
        require(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "Add supabase.url and supabase.anon.key to local.properties"
        }
        supabase.auth.awaitReady()
        supabase.auth.currentSessionOrNull()
            ?: error("Sign in (Settings) to chat with your pet")

        val req = ChatRequest(
            messages = messages.map { m ->
                ChatMessageDto(
                    role = if (m.role == Role.USER) "user" else "assistant",
                    text = m.text,
                )
            },
            factTexts = factTexts,
        )
        val response = supabase.functions.invoke("chat", req)
        val raw = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Chat request failed (${response.status.value}): $raw")
        }
        val parsed = try {
            json.decodeFromString(ChatResponse.serializer(), raw)
        } catch (_: Exception) {
            error("Invalid chat response: $raw")
        }
        if (!parsed.error.isNullOrBlank()) {
            error(parsed.error)
        }
        return parsed.text ?: error("Empty response from server")
    }

    override suspend fun extractFacts(conversationSnippet: String): List<String> {
        require(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "Add supabase.url and supabase.anon.key to local.properties"
        }
        supabase.auth.awaitReady()
        supabase.auth.currentSessionOrNull()
            ?: error("Sign in (Settings) to use the pet")

        val req = ExtractFactsRequest(snippet = conversationSnippet)
        val response = supabase.functions.invoke("extract-facts", req)
        val raw = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Extract-facts failed (${response.status.value}): $raw")
        }
        val parsed = try {
            json.decodeFromString(ExtractFactsResponse.serializer(), raw)
        } catch (_: Exception) {
            error("Invalid extract-facts response: $raw")
        }
        if (!parsed.error.isNullOrBlank()) {
            error(parsed.error)
        }
        return parsed.lines ?: emptyList()
    }

    @Serializable
    private data class ChatMessageDto(val role: String, val text: String)

    @Serializable
    private data class ChatRequest(val messages: List<ChatMessageDto>, val factTexts: List<String>)

    @Serializable
    private data class ChatResponse(val text: String? = null, val error: String? = null)

    @Serializable
    private data class ExtractFactsRequest(val snippet: String)

    @Serializable
    private data class ExtractFactsResponse(val lines: List<String>? = null, val error: String? = null)
}
