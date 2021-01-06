package com.gnefedev.coroutines.vs.reactor.repos;

import com.gnefedev.coroutines.vs.reactor.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class AccountRepositoryImpl {
    private final DatabaseClient databaseClient;

    public Mono<Void> transferAmount(long id, int version, BigDecimal toTransfer) {
        return databaseClient.execute("" +
                "UPDATE account " +
                "   SET amount = amount + :to_transfer, version = version + 1" +
                "   WHERE id = :id AND version = :version"
        )
                .bind("id", id)
                .bind("version", version)
                .bind("to_transfer", toTransfer)
                .fetch()
                .rowsUpdated()
                .flatMap(i -> {
                    if (i == 0) {
                        return Mono.error(new OptimisticLockException());
                    } else {
                        return Mono.empty();
                    }
                });
    }
}
