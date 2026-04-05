package com.kawaiipet.app.llm

import android.util.Log
import com.kawaiipet.app.memory.FactMatcher
import com.kawaiipet.app.memory.MemoryRepository
import com.kawaiipet.app.memory.db.FactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FactExtractor @Inject constructor(
    private val llmService: LlmService,
    private val memoryRepository: MemoryRepository,
    private val factMatcher: FactMatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun extractAndStoreAsync(userText: String, assistantText: String) {
        scope.launch {
            try {
                val snippet = "User: $userText\nAssistant: $assistantText"
                val facts = llmService.extractFacts(snippet)

                facts.forEach { rawLine ->
                    val factText = sanitizeFactLine(rawLine)
                    if (factText.isBlank()) return@forEach

                    val keywords = factMatcher.keywordsForStorage(factText, userText)
                    val entity = FactEntity(
                        factText = factText,
                        keywords = keywords,
                        createdAt = System.currentTimeMillis(),
                        lastAccessed = System.currentTimeMillis()
                    )
                    memoryRepository.insertFact(entity)
                    Log.d(TAG, "Stored fact: $factText (keywords=$keywords)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fact extraction failed", e)
            }
        }
    }

    private fun sanitizeFactLine(line: String): String {
        var s = line.trim()
        s = s.removePrefix("•").trim()
        s = s.removePrefix("-").trim()
        s = s.removePrefix("*").trim()
        s = Regex("^\\d+\\.\\s*").replace(s, "")
        s = s.trim()

        if (isJunkLine(s)) return ""
        return s
    }

    private fun isJunkLine(line: String): Boolean {
        val lower = line.lowercase()
        if (lower.length < 4) return true

        val junkPatterns = listOf(
            "output", "empty_output", "instruction", "thought",
            "<thought>", "</thought>", "<output>", "</output>",
            "```", "none", "no facts", "no personal facts",
            "n/a", "not applicable"
        )
        if (junkPatterns.any { lower == it || lower == "$it:" }) return true

        if (Regex("^</?\\w+>$").matches(line)) return true
        if (Regex("^\\w+:\\s*$").matches(line)) return true

        return false
    }

    companion object {
        private const val TAG = "FactExtractor"
    }
}
