package com.github.pksokolowski.rxjavafun.operators

import io.reactivex.rxjava3.core.ObservableTransformer

fun <T> filterDoubleTap(periodMillis: Long): ObservableTransformer<T, T> {
    var lastStamp = -1L
    return ObservableTransformer<T, T> { observable ->
        observable
            .filter {
                val now = System.currentTimeMillis()
                val sinceLast = now - lastStamp
                lastStamp = now
                val accepted = sinceLast <= periodMillis
                if (accepted) lastStamp = -1
                accepted
            }
    }
}