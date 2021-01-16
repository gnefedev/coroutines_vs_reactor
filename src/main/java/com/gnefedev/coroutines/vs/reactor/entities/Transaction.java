package com.gnefedev.coroutines.vs.reactor.entities

import org.springframework.data.annotation.Id
import java.math.BigDecimal

data class Transaction(
        @Id
        val id: Long? = null,
        val fromAccountId: Long,
        val toAccountId: Long,
        val amount: BigDecimal,
        val uniqueKey: String
)
