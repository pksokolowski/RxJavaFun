package com.github.pksokolowski.rxjavafun.api.fakes

import io.reactivex.Observable

@Suppress("MemberVisibilityCanBePrivate")
open class FakeResource<T>(protected val response: T, protected val delayMillis: Long = 0) {
    fun get(): T {
        if (delayMillis != 0L) Thread.sleep(delayMillis)
        return response
    }

    fun getObservable(): Observable<T> {
        return Observable.fromCallable { get() }
    }

}