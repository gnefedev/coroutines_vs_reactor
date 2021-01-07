package com.gnefedev.coroutines.vs.reactor.util

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.StopRetrying
import com.github.michaelbull.retry.policy.RetryPolicy

fun <E : Throwable> filter(test: (Throwable) -> Boolean): RetryPolicy<E> = {
    if (test(this.reason)) {
        ContinueRetrying
    } else {
        StopRetrying
    }
}