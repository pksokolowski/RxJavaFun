package com.github.pksokolowski.rxjavafun.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component

@PerApp
@Component(modules = [AppModule::class, NetworkModule::class, ActivitiesModule::class, FragmentsModule::class])
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }

    fun inject(app: App)
}