package com.gnefedev.coroutines.vs.reactor.repos

import com.gnefedev.coroutines.vs.reactor.entities.Transaction
import org.springframework.data.repository.Repository
import reactor.core.publisher.Flux

interface TransactionRepository : Repository<Transaction, Long> {
    fun findAll(): Flux<Transaction>
    suspend fun save(transaction: Transaction): Transaction
    suspend fun findByUniqueKey(uniqueKey: String): Transaction?
}