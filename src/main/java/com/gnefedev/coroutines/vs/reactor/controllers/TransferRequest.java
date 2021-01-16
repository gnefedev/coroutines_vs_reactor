package com.gnefedev.coroutines.vs.reactor.controllers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.NonNull;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Jacksonized
@AllArgsConstructor
@Builder
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferRequest {
    @NonNull
    @NotBlank
    private final String transactionKey;
    private final long fromAccountId;
    private final long toAccountId;
    @Min(0)
    @NonNull
    private final BigDecimal amount;
}
