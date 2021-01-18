package com.gnefedev.coroutines.vs.reactor.entities

import org.springframework.data.annotation.Id
import java.math.BigDecimal

data class Account(
        @Id
        val id: Long? = null,
        val amount: BigDecimal,
        val version: Int = 0
)
