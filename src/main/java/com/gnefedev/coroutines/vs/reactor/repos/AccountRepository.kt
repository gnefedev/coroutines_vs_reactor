package com.gnefedev.coroutines.vs.reactor.repos;

import com.gnefedev.coroutines.vs.reactor.entities.Account;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AccountRepository extends Repository<Account, Long> {
    Mono<Account> save(Account account);

    Mono<Void> transferAmount(long id, int version, BigDecimal toTransfer);

    Mono<Account> findById(long id);
}
