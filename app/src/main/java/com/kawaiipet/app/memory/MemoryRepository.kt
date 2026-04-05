package com.kawaiipet.app.memory

import com.kawaiipet.app.memory.db.FactDao
import com.kawaiipet.app.memory.db.FactEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val factDao: FactDao
) {
    val allFacts: Flow<List<FactEntity>> = factDao.getAllFacts()

    suspend fun insertFact(fact: FactEntity): Long = factDao.insert(fact)

    suspend fun deleteFact(fact: FactEntity) = factDao.delete(fact)

    suspend fun touchFact(factId: Long) =
        factDao.updateLastAccessed(factId, System.currentTimeMillis())

    suspend fun getRecentFacts(limit: Int = 20): List<FactEntity> =
        factDao.getRecentFacts(limit)

    suspend fun findFactsByKeywords(keywords: List<String>): List<FactEntity> {
        if (keywords.isEmpty()) return getRecentFacts(5)

        val results = mutableSetOf<FactEntity>()
        keywords.forEach { keyword ->
            results.addAll(factDao.findByKeyword(keyword))
        }
        return results
            .sortedByDescending { it.importanceScore }
            .take(10)
    }
}
