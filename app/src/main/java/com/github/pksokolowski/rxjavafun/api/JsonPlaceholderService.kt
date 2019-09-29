package com.github.pksokolowski.rxjavafun.api

import com.github.pksokolowski.rxjavafun.api.models.Post
import io.reactivex.Observable
import retrofit2.http.GET

interface JsonPlaceholderService{

    @GET("posts")
    fun getPosts(): Observable<List<Post>>
}