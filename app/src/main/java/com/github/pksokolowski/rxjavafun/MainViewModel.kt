package com.github.pksokolowski.rxjavafun

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.pksokolowski.rxjavafun.api.SomeService
import com.github.pksokolowski.rxjavafun.api.fakes.PostsFakeService
import com.github.pksokolowski.rxjavafun.api.models.Post
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val service: PostsFakeService
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val posts = MutableLiveData<List<Post>>().apply {
        value = listOf()
    }

    fun getPosts() = posts as LiveData<List<Post>>

    fun fetchPosts() =
        service.getPosts()
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


    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}