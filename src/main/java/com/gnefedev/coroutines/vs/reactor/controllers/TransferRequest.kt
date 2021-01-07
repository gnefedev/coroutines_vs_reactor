package com.gnefedev.coroutines.vs.reactor.controllers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransferRequest constructor(
        val transactionKey: String,
        val fromAccountId: Long,
        val toAccountId: Long,
        val amount: BigDecimal,
)
