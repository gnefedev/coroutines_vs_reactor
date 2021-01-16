package com.gnefedev.coroutines.vs.reactor.controllers

import com.gnefedev.coroutines.vs.reactor.services.Ledger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/ledger")
class LedgerController(
        private val ledger: Ledger
) {
    @PutMapping("/transfer")
    suspend fun transfer(@Valid @RequestBody request: TransferRequest) = coroutineScope {
        withContext(MDCContext()) {
            ledger.transfer(request.transactionKey, request.fromAccountId, request.toAccountId, request.amount)
        }
    }
}
