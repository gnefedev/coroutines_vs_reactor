package com.gnefedev.coroutines.vs.reactor.services

import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import com.gnefedev.coroutines.vs.reactor.OptimisticLockException
import com.gnefedev.coroutines.vs.reactor.entities.Transaction
import com.gnefedev.coroutines.vs.reactor.repos.AccountRepository
import com.gnefedev.coroutines.vs.reactor.repos.TransactionRepository
import com.gnefedev.coroutines.vs.reactor.util.filter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.*

@Component
class Ledger(
        private val accountRepository: AccountRepository,
        private val transactionRepository: TransactionRepository,
        private val transactionalOperator: TransactionalOperator
) {
    private companion object : KLogging()

    suspend fun transfer(transactionKey: String, fromAccountId: Long, toAccountId: Long, amountToTransfer: BigDecimal) {
        try {
            try {
                retry(limitAttempts(3) + filter { it is OptimisticLockException }) {
                    val foundTransaction = transactionRepository.findByUniqueKey(transactionKey)
                    if (foundTransaction != null) {
                        logger.warn("retry of transaction $transactionKey")
                        return@retry
                    }

                    val fromAccount = accountRepository.findById(fromAccountId)
                            ?: throw IllegalArgumentException("account not found")
                    val toAccount = accountRepository.findById(toAccountId)
                            ?: throw IllegalArgumentException("account not found")

                    if (fromAccount.amount - amountToTransfer < BigDecimal.ZERO) {
                        throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "can't transfer, not enough money")
                    }
                    val transactionToInsert = Transaction(
                            amount = amountToTransfer,
                            fromAccountId = fromAccountId,
                            toAccountId = toAccountId,
                            uniqueKey = transactionKey
                    )
                    transactionalOperator.executeAndAwait {
                        try {
                            transactionRepository.save(transactionToInsert)
                        } catch (e: DataIntegrityViolationException) {
                            if (e.message?.contains("TRANSACTION_UNIQUE_KEY") != true) {
                                throw e;
                            }
                        }

                        accountRepository.transferAmount(fromAccount.id!!, fromAccount.version, amountToTransfer.negate())
                        accountRepository.transferAmount(toAccount.id!!, toAccount.version, amountToTransfer)
                    }
                }
            } catch (e: OptimisticLockException) {
                throw ResponseStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, "limit of OptimisticLockException exceeded", e)
            }
        } catch (e: Exception) {
            logger.error(e) { "error on transfer" }
            throw e;
        }
    }


    suspend fun transferParallel(transactionKey: String, fromAccountId: Long, toAccountId: Long, amountToTransfer: BigDecimal) = coroutineScope {
        try {
            try {
                retry(limitAttempts(3) + filter { it is OptimisticLockException }) {
                    val foundTransactionAsync = async {
                        logger.info("async fetch of transaction $transactionKey")
                        transactionRepository.findByUniqueKey(transactionKey)
                    }
                    val fromAccountAsync = async { accountRepository.findById(fromAccountId) }
                    val toAccountAsync = async { accountRepository.findById(toAccountId) }

                    if (foundTransactionAsync.await() != null) {
                        logger.warn("retry of transaction $transactionKey")
                        return@retry
                    }

                    val fromAccount = fromAccountAsync.await() ?: throw IllegalArgumentException("account not found")
                    val toAccount = toAccountAsync.await() ?: throw IllegalArgumentException("account not found")
                    if (fromAccount.amount.subtract(amountToTransfer) < BigDecimal.ZERO || toAccount.amount.add(amountToTransfer) < BigDecimal.ZERO) {
                        throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "can't transfer, not enough money")
                    }
                    val transactionToInsert = Transaction(
                            amount = amountToTransfer,
                            fromAccountId = fromAccountId,
                            toAccountId = toAccountId,
                            uniqueKey = transactionKey
                    )
                    transactionalOperator.executeAndAwait {
                        try {
                            transactionRepository.save(transactionToInsert)
                        } catch (e: DataIntegrityViolationException) {
                            if (e.message?.contains("TRANSACTION_UNIQUE_KEY") != true) {
                                throw e;
                            }
                        }

                        accountRepository.transferAmount(fromAccount.id!!, fromAccount.version, amountToTransfer.negate())
                        accountRepository.transferAmount(toAccount.id!!, toAccount.version, amountToTransfer)
                    }
                }
            } catch (e: OptimisticLockException) {
                throw ResponseStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, "limit of OptimisticLockException exceeded", e)
            }
        } catch (e: Exception) {
            logger.error(e) { "error on transfer" }
            throw e;
        }
    }
}
