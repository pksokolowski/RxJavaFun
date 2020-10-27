package com.github.pksokolowski.rxjavafun.api.fakes

import io.reactivex.rxjava3.core.Observable

fun <T> fakeResource(delayMillis: Long = 0, response: () -> T): Observable<T> {
    fun get(): T {
        if (delayMillis != 0L) Thread.sleep(delayMillis)
        return response.invoke()
    }

    return Observable.fromCallable { get() }
}