package com.gnefedev.coroutines.vs.reactor.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
public class Account {
    @Id
    private final Long id;
    private final BigDecimal amount;
    private final int version;
}
