package com.gnefedev.coroutines.vs.reactor;

import com.gnefedev.coroutines.vs.reactor.controllers.TransferRequest;
import com.gnefedev.coroutines.vs.reactor.entities.Account;
import com.gnefedev.coroutines.vs.reactor.repos.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;


@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@SpringBootTest
public class ApplicationTest {
    private final AccountRepository accountRepository;
    private final WebTestClient webTestClient;

    @Test
    public void successTransfer() {
        var firstAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.valueOf(100))
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        var secondAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.ZERO)
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        webTestClient.put()
                .uri("/api/ledger/transfer")
                .body(BodyInserters.fromValue(TransferRequest.builder()
                        .fromAccountId(firstAccount.getId())
                        .toAccountId(secondAccount.getId())
                        .amount(BigDecimal.valueOf(100))
                        .transactionKey(UUID.randomUUID().toString())
                        .build()
                ))
                .exchange()
                .expectStatus().is2xxSuccessful();

        assertThat(accountRepository.findById(firstAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.ZERO));
        assertThat(accountRepository.findById(secondAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.valueOf(100)));
    }

    @Test
    public void shouldNotGoBelowZero() {
        var firstAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.valueOf(100))
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        var secondAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.ZERO)
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        webTestClient.put()
                .uri("/api/ledger/transfer")
                .body(BodyInserters.fromValue(TransferRequest.builder()
                        .fromAccountId(firstAccount.getId())
                        .toAccountId(secondAccount.getId())
                        .amount(BigDecimal.valueOf(200))
                        .transactionKey(UUID.randomUUID().toString())
                        .build()
                ))
                .exchange()
                .expectStatus().is4xxClientError();

        assertThat(accountRepository.findById(firstAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.valueOf(100)));
        assertThat(accountRepository.findById(secondAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.ZERO));
    }

    @RepeatedTest(10)
    public void shouldNotGoBelowZeroWithParallelRequests() {
        var firstAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.valueOf(100))
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        var secondAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.ZERO)
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        var statuses = new CopyOnWriteArrayList<HttpStatus>();

        testConcurrently(
                () -> statuses.add(webTestClient.put()
                        .uri("/api/ledger/transfer")
                        .body(BodyInserters.fromValue(TransferRequest.builder()
                                .fromAccountId(firstAccount.getId())
                                .toAccountId(secondAccount.getId())
                                .amount(BigDecimal.valueOf(100))
                                .transactionKey(UUID.randomUUID().toString())
                                .build()
                        ))
                        .exchange().returnResult(String.class)
                        .getStatus()),
                () -> statuses.add(webTestClient.put()
                        .uri("/api/ledger/transfer")
                        .body(BodyInserters.fromValue(TransferRequest.builder()
                                .fromAccountId(firstAccount.getId())
                                .toAccountId(secondAccount.getId())
                                .amount(BigDecimal.valueOf(100))
                                .transactionKey(UUID.randomUUID().toString())
                                .build()
                        ))
                        .exchange().returnResult(String.class)
                        .getStatus())
        );

        assertThat(statuses, containsInAnyOrder(HttpStatus.OK, HttpStatus.UNPROCESSABLE_ENTITY));

        assertThat(accountRepository.findById(firstAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.ZERO));
        assertThat(accountRepository.findById(secondAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.valueOf(100)));
    }

    @Test
    public void successRetry() {
        var firstAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.valueOf(100))
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        var secondAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.ZERO)
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        var request = BodyInserters.fromValue(TransferRequest.builder()
                .fromAccountId(firstAccount.getId())
                .toAccountId(secondAccount.getId())
                .amount(BigDecimal.valueOf(100))
                .transactionKey(UUID.randomUUID().toString())
                .build()
        );
        for (int i = 0; i < 2; i++) {
            webTestClient.put()
                    .uri("/api/ledger/transfer")
                    .body(request)
                    .exchange()
                    .expectStatus().is2xxSuccessful();
        }

        assertThat(accountRepository.findById(firstAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.ZERO));
        assertThat(accountRepository.findById(secondAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.valueOf(100)));
    }

    @RepeatedTest(10)
    public void duplicatedRequest() {
        var firstAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.valueOf(100))
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        var secondAccount = accountRepository.save(Account.builder()
                .amount(BigDecimal.ZERO)
                .version(0)
                .build()
        ).blockOptional().orElseThrow();

        var request = BodyInserters.fromValue(TransferRequest.builder()
                .fromAccountId(firstAccount.getId())
                .toAccountId(secondAccount.getId())
                .amount(BigDecimal.valueOf(50))
                .transactionKey(UUID.randomUUID().toString())
                .build()
        );

        testConcurrently(
                () -> webTestClient.put()
                        .uri("/api/ledger/transfer")
                        .body(request)
                        .exchange()
                        .expectStatus().is2xxSuccessful(),
                () -> webTestClient.put()
                        .uri("/api/ledger/transfer")
                        .body(request)
                        .exchange()
                        .expectStatus().is2xxSuccessful()
        );

        assertThat(accountRepository.findById(firstAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.valueOf(50)));
        assertThat(accountRepository.findById(secondAccount.getId()).blockOptional().orElseThrow().getAmount(), comparesEqualTo(BigDecimal.valueOf(50)));
    }


    @SneakyThrows
    public static void testConcurrently(Executable... blocks) {
        AtomicReference<Throwable> result = new AtomicReference<>();
        var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        CountDownLatch done = new CountDownLatch(blocks.length);
        for (Executable block : blocks) {
            executor.submit(() -> {
                try {
                    block.execute();
                } catch (Throwable e) {
                    result.updateAndGet(r -> {
                        if (r == null) {
                            return e;
                        }
                        r.addSuppressed(e);
                        return r;
                    });
                }
                done.countDown();
            });
        }
        try {
            done.await(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                throw new TimeoutException();
            }
            if (result.get() != null) {
                throw result.get();
            }
        }
    }

}
