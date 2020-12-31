package com.github.pksokolowski.rxjavafun

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.pksokolowski.rxjavafun.api.fakes.PostsFakeService
import com.github.pksokolowski.rxjavafun.api.fakes.VocabFakeService
import com.github.pksokolowski.rxjavafun.api.models.Post
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random


class MainViewModel @Inject constructor(
    private val postsService: PostsFakeService,
    private val vocabService: VocabFakeService
) : ViewModel() {

    private val _output = PublishSubject.create<String>()
    val output: Observable<String> = _output

    private val _outputUpdateLastLine = PublishSubject.create<String>()
    val outputUpdateLastLine: Observable<String> = _outputUpdateLastLine

    private fun output(message: String, updateLastLine: Boolean = false) {
        if (updateLastLine) {
            _outputUpdateLastLine.onNext(message)
        } else {
            _output.onNext(message)
        }
    }

    private val disposables = CompositeDisposable()
    private val samplesDisposables = CompositeDisposable()

    private val posts = MutableLiveData<List<Post>>().apply {
        value = listOf()
    }

    fun getPosts() = posts as LiveData<List<Post>>

    fun fetchPosts() =
        postsService.getPosts()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { posts.value = it },
                onError = {
                    it.printStackTrace()
                },
                onComplete = {}
            )
            .addTo(disposables)

    fun findVocabulary(prefix: String): Single<List<String>> = vocabService.getVocabulary()
        .filter { it.startsWith(prefix) }
        .distinct()
        .toSortedList()

    fun getTimer() = Observable.timer(1, TimeUnit.SECONDS)
        .repeat()
        .map { "$it  ${System.currentTimeMillis()}" }
        .subscribeOn(Schedulers.single())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { timeInfo ->
            output(timeInfo, true)
        }
        .addTo(samplesDisposables)

    fun fetchPostsOfAllUsers() {
        val results = mutableListOf<Post>()
        postsService.getUsers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapIterable { it }
            .flatMap { user ->
                postsService.getPostsByUserId(user.id)
            }
            .subscribeBy(
                onNext = { results.addAll(it) },
                onComplete = { posts.value = results }
            )
            .addTo(disposables)
    }

    fun maybeFun(): Single<Int> {
        val cachedResponse = Maybe.create<Int> { e ->
            if (Random.nextInt(1, 11) > 7) {
                e.onSuccess(R.string.maybe_first_prize)
            }
            e.onComplete()
        }

        val freshDownload = Maybe.create<Int> { e ->
            if (Random.nextInt(1, 11) > 4) {
                e.onSuccess(R.string.maybe_second_prize)
            }
            e.onComplete()
        }

        // turn two maybes into a single... single.
        return Maybe.concat(cachedResponse, freshDownload)
            .firstOrError()
    }

    private fun processInteger(input: Int) {
        Thread.sleep(100)
        output("processed input = $input", true)
    }

    fun backPressureUnhandled() {
        val source = PublishSubject.create<Int>()

        source.observeOn(Schedulers.computation())
            .subscribe(::processInteger) { output("Exception: $it") }
            .addTo(samplesDisposables)

        (1..1_300_000).forEach(source::onNext)
    }

    fun backPressureSample() {
        val source = PublishSubject.create<Int>()

        source
            .toFlowable(BackpressureStrategy.DROP)
            .sample(100, TimeUnit.MILLISECONDS)
            .observeOn(Schedulers.computation())
            .subscribe(::processInteger) { throwable -> output(throwable.toString()) }
            .addTo(samplesDisposables)

        (1..1_300_000).forEach(source::onNext)
    }

    fun combineLatest() {
        val observableA = emitFollowing(listOf(true, true, false, true), 300)
        val observableB = emitFollowing(listOf(false, true, false, false, true), 200)

        Observable.combineLatest(observableA, observableB) { itemA, itemB ->
            itemA && itemB
        }
            .observeOn(Schedulers.computation())
            .subscribe { bothTrue ->
                output(if (bothTrue) "Both are true!" else "at least one is false")
            }
            .addTo(samplesDisposables)

    }

    private fun <T> emitFollowing(items: List<T>, delay: Long): Observable<T> =
        Observable.fromIterable(items)
            .map { Thread.sleep(delay); it }

    /**
     * Kills streams of code samples, useful when switching between long-running samples
     */
    fun clearOngoingSampleStreams() {
        samplesDisposables.clear()
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}