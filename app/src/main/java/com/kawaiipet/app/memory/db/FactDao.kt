package com.kawaiipet.app.memory.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fact: FactEntity): Long

    @Delete
    suspend fun delete(fact: FactEntity)

    @Query("SELECT * FROM facts ORDER BY lastAccessed DESC")
    fun getAllFacts(): Flow<List<FactEntity>>

    @Query("SELECT * FROM facts ORDER BY lastAccessed DESC LIMIT :limit")
    suspend fun getRecentFacts(limit: Int = 20): List<FactEntity>

    @Query("SELECT * FROM facts WHERE keywords LIKE '%' || :keyword || '%'")
    suspend fun findByKeyword(keyword: String): List<FactEntity>

    @Query("UPDATE facts SET lastAccessed = :timestamp WHERE id = :factId")
    suspend fun updateLastAccessed(factId: Long, timestamp: Long)

    @Query("DELETE FROM facts WHERE id = :factId")
    suspend fun deleteById(factId: Long)

    @Query("SELECT COUNT(*) FROM facts")
    suspend fun count(): Int
}
