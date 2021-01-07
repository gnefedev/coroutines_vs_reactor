package com.gnefedev.coroutines.vs.reactor.repos

import com.gnefedev.coroutines.vs.reactor.entities.Account
import org.springframework.data.repository.Repository
import reactor.core.publisher.Flux
import java.math.BigDecimal

interface AccountRepository : Repository<Account, Long> {
    fun findAll(): Flux<Account>

    suspend fun save(account: Account): Account
    suspend fun transferAmount(id: Long, version: Int, toTransfer: BigDecimal)
    suspend fun findById(id: Long): Account?
}