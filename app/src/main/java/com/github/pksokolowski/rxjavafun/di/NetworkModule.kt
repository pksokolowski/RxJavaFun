package com.github.pksokolowski.rxjavafun.di

import com.github.pksokolowski.rxjavafun.api.JsonPlaceholderService
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@Module
open class NetworkModule {

    @PerApp
    @Provides
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    @PerApp
    @Provides
    fun provideJsonPlaceholderService(retrofit: Retrofit): JsonPlaceholderService =
        retrofit.create(JsonPlaceholderService::class.java)

}