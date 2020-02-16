package com.github.pksokolowski.rxjavafun.api

import com.github.pksokolowski.rxjavafun.api.models.Post
import com.github.pksokolowski.rxjavafun.api.models.User
import io.reactivex.Observable
import retrofit2.http.GET

interface SomeService{

    @GET("users")
    fun getUsers(): Observable<List<User>>

    @GET("posts")
    fun getPosts(): Observable<List<Post>>

    @GET("kopkop")
    fun getPostsByUserId(userId: Long): Observable<List<Post>>
}