package com.github.pksokolowski.rxjavafun.api.fakes

import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

/**
 * Provides some vocabulary sample, a list of all words in existence, curtailed just a bit
 * for practical purposes.
 */
class VocabFakeService @Inject constructor() {

    fun getVocabulary() = Observable.fromIterable(vocabulary)

    private val vocabulary = """
        Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut 
        labore et dolore magna aliqua Ut enim ad minim veniam quis nostrud exercitation ullamco 
        laboris nisi ut aliquip ex ea commodo consequat Duis aute irure dolor in reprehenderit in 
        voluptate velit esse cillum dolore eu fugiat nulla pariatur Excepteur sint occaecat cupidatat 
        non proident sunt in culpa qui officia deserunt mollit anim id est laborum
    """.trimIndent()
        .split(" ")
        .map { it.toLowerCase().trim() }
        .takeIf { it.isNotEmpty() }
}