package com.github.pksokolowski.rxjavafun.di

import dagger.Module
import dagger.android.AndroidInjectionModule
import dagger.android.support.AndroidSupportInjectionModule

@Module(includes = [ViewModelsModule::class, AndroidInjectionModule::class, AndroidSupportInjectionModule::class])
open class AppModule {

}