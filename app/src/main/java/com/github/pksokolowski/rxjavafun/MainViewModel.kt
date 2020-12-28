package com.github.pksokolowski.rxjavafun

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.pksokolowski.rxjavafun.api.fakes.PostsFakeService
import com.github.pksokolowski.rxjavafun.api.fakes.VocabFakeService
import com.github.pksokolowski.rxjavafun.api.models.Post
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
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

    private fun output(message: String) {
        _output.onNext(message)
    }

    private val disposables = CompositeDisposable()

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
            output(timeInfo)
        }
        .addTo(disposables)

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

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}