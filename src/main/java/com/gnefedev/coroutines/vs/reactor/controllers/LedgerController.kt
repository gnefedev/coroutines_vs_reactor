package com.gnefedev.coroutines.vs.reactor.controllers;

import com.gnefedev.coroutines.vs.reactor.services.Ledger;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
    private final Ledger ledger;

    @PutMapping("/transfer")
    public Mono<Void> transfer(@RequestBody TransferRequest request) {
        return ledger.transfer(request.getTransactionKey(), request.getFromAccountId(), request.getToAccountId(), request.getAmount());
    }
}
