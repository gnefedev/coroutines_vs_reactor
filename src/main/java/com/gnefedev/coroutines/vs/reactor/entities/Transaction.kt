package com.gnefedev.coroutines.vs.reactor.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Getter
@Builder
public class Transaction {
    @Id
    private final Long id;
    private final Long fromAccountId;
    private final Long toAccountId;
    private final BigDecimal amount;
    private final String uniqueKey;
}
