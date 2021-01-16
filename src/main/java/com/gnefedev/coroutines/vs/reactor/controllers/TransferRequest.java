package com.gnefedev.coroutines.vs.reactor.controllers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.lang.NonNull
import java.math.BigDecimal
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransferRequest constructor(
        @field:NonNull
        @field:NotBlank
        val transactionKey: String,
        val fromAccountId: Long,
        val toAccountId: Long,
        @field:Min(0)
        @field:NonNull
        val amount: BigDecimal,
)
