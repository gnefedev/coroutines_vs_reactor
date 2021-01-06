package com.gnefedev.coroutines.vs.reactor.services;

import com.gnefedev.coroutines.vs.reactor.OptimisticLockException;
import com.gnefedev.coroutines.vs.reactor.entities.Transaction;
import com.gnefedev.coroutines.vs.reactor.repos.AccountRepository;
import com.gnefedev.coroutines.vs.reactor.repos.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static com.gnefedev.coroutines.vs.reactor.util.LoggerHelper.withMDC;

@Component
@RequiredArgsConstructor
@Log4j2
public class Ledger {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionalOperator transactionalOperator;

    public Mono<Void> transfer(String transactionKey, long fromAccountId, long toAccountId, BigDecimal amountToTransfer) {
        return transactionRepository.findByUniqueKey(transactionKey)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(withMDC(foundTransaction -> {
                    if (foundTransaction.isPresent()) {
                        log.warn("retry of transaction " + transactionKey);
                        return Mono.empty();
                    }
                    return accountRepository.findById(fromAccountId)
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("account not found")))
                            .flatMap(fromAccount -> accountRepository.findById(toAccountId)
                                    .switchIfEmpty(Mono.error(new IllegalArgumentException("account not found")))
                                    .flatMap(toAccount -> {
                                        var transactionToInsert = Transaction.builder()
                                                .amount(amountToTransfer)
                                                .fromAccountId(fromAccountId)
                                                .toAccountId(toAccountId)
                                                .uniqueKey(transactionKey)
                                                .build();
                                        if (fromAccount.getAmount().subtract(amountToTransfer).compareTo(BigDecimal.ZERO) < 0 || toAccount.getAmount().add(amountToTransfer).compareTo(BigDecimal.ZERO) < 0) {
                                            return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "can't transfer, not enough money"));
                                        }
                                        return transactionalOperator.transactional(
                                                transactionRepository.save(transactionToInsert)
                                                        .then(accountRepository.transferAmount(fromAccount.getId(), fromAccount.getVersion(), amountToTransfer.negate()))
                                                        .then(accountRepository.transferAmount(toAccount.getId(), toAccount.getVersion(), amountToTransfer))
                                        ).onErrorResume(error -> {
                                            //transaction was inserted on parallel transaction, we may return success response
                                            if (error instanceof DataIntegrityViolationException && error.getMessage().contains("TRANSACTION_UNIQUE_KEY")) {
                                                return Mono.empty();
                                            } else {
                                                return Mono.error(error);
                                            }
                                        });
                                    }));
                }))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(1))
                        .filter(OptimisticLockException.class::isInstance)
                        .onRetryExhaustedThrow((__, retrySignal) -> retrySignal.failure())
                )
                .onErrorMap(
                        OptimisticLockException.class,
                        e -> new ResponseStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, "limit of OptimisticLockException exceeded", e)
                );
    }

    public Mono<Void> transferParallel(String transactionKey, long fromAccountId, long toAccountId, BigDecimal amountToTransfer) {
        return Mono.zip(
                transactionRepository.findByUniqueKey(transactionKey)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty()),
                accountRepository.findById(fromAccountId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("account not found"))),
                accountRepository.findById(toAccountId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("account not found")))
        ).flatMap(withMDC(fetched -> {
            var foundTransaction = fetched.getT1();
            var fromAccount = fetched.getT2();
            var toAccount = fetched.getT3();
            if (foundTransaction.isPresent()) {
                log.warn("retry of transaction " + transactionKey);
                return Mono.empty();
            }
            var transactionToInsert = Transaction.builder()
                    .amount(amountToTransfer)
                    .fromAccountId(fromAccountId)
                    .toAccountId(toAccountId)
                    .uniqueKey(transactionKey)
                    .build();
            if (fromAccount.getAmount().subtract(amountToTransfer).compareTo(BigDecimal.ZERO) < 0 || toAccount.getAmount().add(amountToTransfer).compareTo(BigDecimal.ZERO) < 0) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "can't transfer, not enough money"));
            }
            return transactionalOperator.transactional(
                    transactionRepository.save(transactionToInsert)
                            .then(accountRepository.transferAmount(fromAccount.getId(), fromAccount.getVersion(), amountToTransfer.negate()))
                            .then(accountRepository.transferAmount(toAccount.getId(), toAccount.getVersion(), amountToTransfer))
            ).onErrorResume(error -> {
                //transaction was inserted on parallel transaction, we may return success response
                if (error instanceof DataIntegrityViolationException && error.getMessage().contains("TRANSACTION_UNIQUE_KEY")) {
                    return Mono.empty();
                } else {
                    return Mono.error(error);
                }
            });
        }))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(1))
                        .filter(OptimisticLockException.class::isInstance)
                        .onRetryExhaustedThrow((__, retrySignal) -> retrySignal.failure())
                )
                .onErrorMap(
                        OptimisticLockException.class,
                        e -> new ResponseStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, "limit of OptimisticLockException exceeded", e)
                );
    }
}
