package com.gnefedev.coroutines.vs.reactor

import com.gnefedev.coroutines.vs.reactor.controllers.TransferRequest
import com.gnefedev.coroutines.vs.reactor.entities.Account
import com.gnefedev.coroutines.vs.reactor.repos.AccountRepository
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest
class ApplicationTest @Autowired constructor(
        private val accountRepository: AccountRepository,
        private val webTestClient: WebTestClient
) {

    @Test
    fun successTransfer() = runBlocking {
        val firstAccount = accountRepository.save(Account(amount = BigDecimal.valueOf(100)))
        val secondAccount = accountRepository.save(Account(amount = BigDecimal.ZERO))
        webTestClient.put()
                .uri("/api/ledger/transfer")
                .body(BodyInserters.fromValue(TransferRequest(
                        fromAccountId = firstAccount.id!!,
                        toAccountId = secondAccount.id!!,
                        amount = BigDecimal.valueOf(100),
                        transactionKey = UUID.randomUUID().toString()
                )))
                .exchange()
                .expectStatus().is2xxSuccessful
        assertThat(accountRepository.findById(firstAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.ZERO))
        assertThat(accountRepository.findById(secondAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.valueOf(100)))
    }

    @Test
    fun shouldNotGoBelowZero() = runBlocking {
        val firstAccount = accountRepository.save(Account(amount = BigDecimal.valueOf(100)))
        val secondAccount = accountRepository.save(Account(amount = BigDecimal.ZERO))
        webTestClient.put()
                .uri("/api/ledger/transfer")
                .body(BodyInserters.fromValue(TransferRequest(
                        fromAccountId = firstAccount.id!!,
                        toAccountId = secondAccount.id!!,
                        amount = BigDecimal.valueOf(200),
                        transactionKey = UUID.randomUUID().toString()
                )))
                .exchange()
                .expectStatus().is4xxClientError
        assertThat(accountRepository.findById(firstAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.valueOf(100)))
        assertThat(accountRepository.findById(secondAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.ZERO))
    }

    @RepeatedTest(10)
    fun shouldNotGoBelowZeroWithParallelRequests() = runBlocking {
        val firstAccount = accountRepository.save(Account(amount = BigDecimal.valueOf(100)))
        val secondAccount = accountRepository.save(Account(amount = BigDecimal.ZERO))
        val statuses = CopyOnWriteArrayList<HttpStatus>()
        testConcurrently(
                Executable {
                    statuses.add(webTestClient.put()
                            .uri("/api/ledger/transfer")
                            .body(BodyInserters.fromValue(TransferRequest(
                                    fromAccountId = firstAccount.id!!,
                                    toAccountId = secondAccount.id!!,
                                    amount = BigDecimal.valueOf(100),
                                    transactionKey = UUID.randomUUID().toString()
                            )))
                            .exchange().returnResult(String::class.java)
                            .status)
                },
                Executable {
                    statuses.add(webTestClient.put()
                            .uri("/api/ledger/transfer")
                            .body(BodyInserters.fromValue(TransferRequest(
                                    fromAccountId = firstAccount.id!!,
                                    toAccountId = secondAccount.id!!,
                                    amount = BigDecimal.valueOf(100),
                                    transactionKey = UUID.randomUUID().toString()
                            )))
                            .exchange().returnResult(String::class.java)
                            .status)
                }
        )
        assertThat(statuses, containsInAnyOrder(HttpStatus.OK, HttpStatus.UNPROCESSABLE_ENTITY))
        assertThat(accountRepository.findById(firstAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.ZERO))
        assertThat(accountRepository.findById(secondAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.valueOf(100)))
    }

    @Test
    fun successRetry() = runBlocking {
        val firstAccount = accountRepository.save(Account(amount = BigDecimal.valueOf(100)))
        val secondAccount = accountRepository.save(Account(amount = BigDecimal.ZERO))
        val request = BodyInserters.fromValue(TransferRequest(
                fromAccountId = firstAccount.id!!,
                toAccountId = secondAccount.id!!,
                amount = BigDecimal.valueOf(100),
                transactionKey = UUID.randomUUID().toString()
        ))
        for (i in 0..1) {
            webTestClient.put()
                    .uri("/api/ledger/transfer")
                    .body(request)
                    .exchange()
                    .expectStatus().is2xxSuccessful
        }
        assertThat(accountRepository.findById(firstAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.ZERO))
        assertThat(accountRepository.findById(secondAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.valueOf(100)))
    }

    @RepeatedTest(10)
    fun duplicatedRequest() = runBlocking {
        val firstAccount = accountRepository.save(Account(amount = BigDecimal.valueOf(100)))
        val secondAccount = accountRepository.save(Account(amount = BigDecimal.ZERO))
        val request = BodyInserters.fromValue(TransferRequest(
                fromAccountId = firstAccount.id!!,
                toAccountId = secondAccount.id!!,
                amount = BigDecimal.valueOf(50),
                transactionKey = UUID.randomUUID().toString()
        ))
        testConcurrently(
                Executable {
                    webTestClient.put()
                            .uri("/api/ledger/transfer")
                            .body(request)
                            .exchange()
                            .expectStatus().is2xxSuccessful
                },
                Executable {
                    webTestClient.put()
                            .uri("/api/ledger/transfer")
                            .body(request)
                            .exchange()
                            .expectStatus().is2xxSuccessful
                }
        )
        assertThat(accountRepository.findById(firstAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.valueOf(50)))
        assertThat(accountRepository.findById(secondAccount.id!!)!!.amount, comparesEqualTo(BigDecimal.valueOf(50)))
    }
}

fun testConcurrently(vararg blocks: Executable) {
    val result = AtomicReference<Throwable?>()
    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    val done = CountDownLatch(blocks.size)
    for (block in blocks) {
        executor.submit {
            try {
                block.execute()
            } catch (e: Throwable) {
                result.updateAndGet { r: Throwable? ->
                    if (r == null) {
                        return@updateAndGet e
                    }
                    r.addSuppressed(e)
                    r
                }
            }
            done.countDown()
        }
    }
    try {
        done.await(5, TimeUnit.SECONDS)
    } finally {
        executor.shutdown()
        if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
            executor.shutdownNow()
            throw TimeoutException()
        }
        if (result.get() != null) {
            throw result.get()!!
        }
    }
}
