package com.gnefedev.coroutines.vs.reactor.repos

import com.gnefedev.coroutines.vs.reactor.OptimisticLockException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import java.math.BigDecimal

class AccountRepositoryImpl(
        private val databaseClient: DatabaseClient
) {
    suspend fun transferAmount(id: Long, version: Int, toTransfer: BigDecimal) {
        databaseClient.sql("" +
                "UPDATE account " +
                "   SET amount = amount + :to_transfer, version = version + 1" +
                "   WHERE id = :id AND version = :version"
        )
                .bind("id", id)
                .bind("version", version)
                .bind("to_transfer", toTransfer)
                .fetch()
                .rowsUpdated()
                .flatMap { i: Int ->
                    if (i == 0) {
                        return@flatMap Mono.error<Void>(OptimisticLockException())
                    } else {
                        return@flatMap Mono.empty<Void>()
                    }
                }
                .awaitFirstOrNull()
    }
}