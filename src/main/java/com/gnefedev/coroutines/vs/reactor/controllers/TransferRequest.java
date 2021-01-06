package com.gnefedev.coroutines.vs.reactor.controllers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Jacksonized
@AllArgsConstructor
@Builder
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferRequest {
    private final String transactionKey;
    private final long fromAccountId;
    private final long toAccountId;
    private final BigDecimal amount;
}
