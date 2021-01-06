package com.gnefedev.coroutines.vs.reactor.repos;

import com.gnefedev.coroutines.vs.reactor.entities.Account;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collection;

public interface AccountRepository extends Repository<Account, Long> {
    Mono<Account> save(Account account);

    Mono<Void> transferAmount(long id, int version, BigDecimal toTransfer);

    Flux<Account> findByIdIn(Collection<Long> ids);

    Mono<Account> findById(long id);
}
