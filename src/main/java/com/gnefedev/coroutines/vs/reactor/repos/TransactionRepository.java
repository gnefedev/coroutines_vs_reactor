package com.gnefedev.coroutines.vs.reactor.repos;

import com.gnefedev.coroutines.vs.reactor.entities.Transaction;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TransactionRepository extends R2dbcRepository<Transaction, Long> {
    Mono<Transaction> findByUniqueKey(String uniqueKey);
}
