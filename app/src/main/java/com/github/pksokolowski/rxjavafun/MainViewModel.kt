package com.github.pksokolowski.rxjavafun

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.pksokolowski.rxjavafun.api.fakes.PostsFakeService
import com.github.pksokolowski.rxjavafun.api.fakes.VocabFakeService
import com.github.pksokolowski.rxjavafun.api.models.Post
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.internal.operators.observable.ObservableJust
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subscribers.DisposableSubscriber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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

    fun backpressureOneAtATime(requestAllAtStartInstead: Boolean = false) {
        output("now subscriber only requests what it can handle - 1 at a time\n")

        val executor: ExecutorService = Executors.newSingleThreadExecutor()

        Flowable.range(1, 10)
            .doOnNext { v: Int -> output("produced: $v") }
            .subscribeOn(Schedulers.io())
            .subscribe(object : DisposableSubscriber<Int>() {
                override fun onStart() {
                    if (requestAllAtStartInstead) {
                        super.onStart()
                    } else {
                        request(1)
                    }
                    samplesDisposables.add(this)
                }

                override fun onNext(item: Int?) {
                    executor.execute {
                        Thread.sleep(100)
                        output("- received item = $item")
                        request(1)
                    }
                }

                override fun onError(t: Throwable?) {
                    executor.execute {
                        output("error happened within subscriber: $t")
                    }
                }

                override fun onComplete() {
                }
            })
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

    fun handleErrors() {
        output("computing 3 / 0...")
        Observable.just(3)
            .subscribeOn(Schedulers.computation())
            .map { it / 0 }
            .doOnError { output("division didn't work") }
            .subscribe { result ->
                output("division result is $result")
            }
            .addTo(samplesDisposables)
    }

    fun handleErrorMidChainWithDefaultReplacement() {
        output("Running a chain od two operations; the first one fails and a default value is used instead for the second one\n")
        Observable.just(0)
            .subscribeOn(Schedulers.computation())
            .map { 3 / it }
            .doOnError { output("error in stage one! Falling back to defaults") }
            .onErrorReturnItem(1)
            .map { it + 1 }
            .subscribeBy { resultOfStage2 ->
                output("Result of the second stage is: $resultOfStage2")
            }
            .addTo(samplesDisposables)
    }

    fun handleErrorAndSwithToADifferentSolution(input: Int) {
        output("Tries a faster, probabilistic algorithm first, when it succeeds - great, but it not, falls back to a slower, deterministic solution\n")

        fun fastProbabilisticAlgorithm(number: Int): Int {
            Thread.sleep(100)
            if (Random.nextBoolean()) throw ArithmeticException("Failed to perform the operation")
            return number * 2
        }

        fun slowDeterministicAlgorithm(number: Int): Int {
            Thread.sleep(1000)
            return number * 2
        }

        Observable.just(input)
            .subscribeOn(Schedulers.computation())
            .map {
                output("trying with the fast, probabilistic algorithm")
                fastProbabilisticAlgorithm(it)
            }
            .onErrorResumeNext {
                output("falling back to a deterministic one")
                Observable.just(input)
                    .map { slowDeterministicAlgorithm(it) }
            }
            .subscribe {
                output("result is: $it")
            }
            .addTo(samplesDisposables)
    }

    fun handlerErrorAndRetry() {
        output("Tries an iffy connection, if it faile, retries a couple of times\n")

        fun downloadResourceOverIffyConnection(): String {
            Thread.sleep(100)
            if (Random.nextBoolean()) throw ArithmeticException("Failed to perform the operation")
            return "<some successfully retrieved content>"
        }

        Observable.just(1)
            .map { downloadResourceOverIffyConnection() }
            .subscribeOn(Schedulers.io())
            .retry(4)
            .doOnError {
                output("Tried 5 times but failed anyway")
            }
            .subscribe {
                output("Got: $it")
            }
    }

    private fun <T> emitFollowing(items: List<T>, delay: Long): Observable<T> =
        Observable.fromIterable(items)
            .map { Thread.sleep(delay); it }

    private fun <T> emitFollowingFlowable(items: List<T>, delay: Long): Flowable<T> =
        Flowable.fromIterable(items)
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