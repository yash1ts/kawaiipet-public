package com.kawaiipet.app.memory

import com.kawaiipet.app.llm.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortTermMemory @Inject constructor() {

    private val messages = mutableListOf<ChatMessage>()

    @Synchronized
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        if (messages.size > MAX_MESSAGES) {
            messages.removeAt(0)
        }
    }

    @Synchronized
    fun getMessages(): List<ChatMessage> = messages.toList()

    @Synchronized
    fun clear() {
        messages.clear()
    }

    @Synchronized
    fun size(): Int = messages.size

    companion object {
        private const val MAX_MESSAGES = 30
    }
}
