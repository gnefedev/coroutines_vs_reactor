package com.gnefedev.coroutines.vs.reactor.repos;

import com.gnefedev.coroutines.vs.reactor.entities.Transaction;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Mono;

public interface TransactionRepository extends Repository<Transaction, Long> {
    Mono<Transaction> save(Transaction transaction);

    Mono<Transaction> findByUniqueKey(String uniqueKey);
}
