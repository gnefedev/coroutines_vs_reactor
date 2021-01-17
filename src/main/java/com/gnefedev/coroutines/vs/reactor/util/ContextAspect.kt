package com.gnefedev.coroutines.vs.reactor.util

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

@Aspect
@Component
class ContextAspect {
    @Around("@annotation(org.springframework.web.bind.annotation.PutMapping)")
    fun wrapSuspend(joinPoint: ProceedingJoinPoint) = callWrapped(joinPoint) { call, target, args ->
        withContext<Unit>(MDCContext()) {
            call(target, args)
        }
    }

    private fun callWrapped(
            joinPoint: ProceedingJoinPoint,
            wrap: suspend (call: suspend Any.(args: Array<Any?>) -> Any?, target: Any, args: Array<Any?>) -> Any?
    ): Any? {
        val continuation = joinPoint.args[joinPoint.args.size - 1] as Continuation<*>
        return ContextAspect::class.java
                .getMethod("wrap", ProceedingJoinPoint::class.java, Function4::class.java, Continuation::class.java)
                .invoke(this, joinPoint, wrap, Continuation<Any?>(continuation.context) {})
    }

    suspend fun wrap(
            joinPoint: ProceedingJoinPoint,
            wrap: suspend (call: suspend Any.(args: Array<Any?>) -> Any?, target: Any, args: Array<Any?>) -> Any?
    ) {
        val method = (joinPoint.signature as MethodSignature).method
        val call: suspend Any.(Array<Any?>) -> Any? = {
            method.kotlinFunction!!.callSuspend(this, *it)
        }
        val args = joinPoint.args.slice(0..joinPoint.args.size - 2).toTypedArray()

        @Suppress("UNCHECKED_CAST")
        val continuation = joinPoint.args[joinPoint.args.size - 1] as Continuation<Any?>
        try {
            val value = wrap(call, joinPoint.target, args)
            continuation.resume(value)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
