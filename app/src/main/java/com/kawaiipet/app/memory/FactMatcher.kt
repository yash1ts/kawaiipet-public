package com.kawaiipet.app.memory

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FactMatcher @Inject constructor() {

    fun extractKeywords(text: String): List<String> {
        val words = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }

        return words
            .filterNot { it in STOP_WORDS }
            .distinct()
            .take(MAX_KEYWORDS)
    }

    /**
     * Produces a non-empty comma-separated keyword string for Room storage / LIKE search.
     * [extractKeywords] can be empty for short proper nouns or very short facts; we still must save the row.
     */
    fun keywordsForStorage(primaryText: String, vararg extraContext: String): String {
        val merged = buildList {
            addAll(extractKeywords(primaryText))
            extraContext.forEach { addAll(extractKeywords(it)) }
        }.distinct().take(MAX_KEYWORDS * 2)

        if (merged.isNotEmpty()) return merged.joinToString(",")

        val relaxed = buildList {
            val tokenize: (String) -> Unit = { s ->
                s.lowercase()
                    .replace(Regex("[^a-z0-9\\s]"), " ")
                    .split(Regex("\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it !in STOP_WORDS }
                    .forEach { add(it) }
            }
            tokenize(primaryText)
            extraContext.forEach(tokenize)
        }.distinct().take(16)

        if (relaxed.isNotEmpty()) return relaxed.joinToString(",")

        val minimal = primaryText.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(8)
        if (minimal.isNotEmpty()) return minimal.joinToString(",")

        return "memory"
    }

    companion object {
        private const val MAX_KEYWORDS = 8

        private val STOP_WORDS = setOf(
            "the", "and", "for", "are", "but", "not", "you", "all",
            "can", "had", "her", "was", "one", "our", "out", "has",
            "have", "been", "some", "them", "than", "its", "over",
            "said", "each", "which", "their", "will", "other", "about",
            "many", "then", "these", "would", "make", "like", "just",
            "could", "what", "there", "know", "take", "come", "made",
            "from", "that", "this", "with", "they", "been", "into",
            "your", "does", "doing", "did", "how", "why", "when",
            "where", "who", "whom", "tell", "told", "really", "very",
            "much", "also", "well", "here", "still", "thing", "things",
            "don", "yes", "yeah", "okay", "sure", "right"
        )
    }
}
