package com.kawaiipet.app.memory.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "facts")
data class FactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val factText: String,
    val category: String = "general",
    val keywords: String,
    val createdAt: Long,
    val lastAccessed: Long,
    val importanceScore: Float = 1.0f
)
