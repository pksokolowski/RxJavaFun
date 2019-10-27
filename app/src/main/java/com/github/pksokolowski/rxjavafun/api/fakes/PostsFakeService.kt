package com.github.pksokolowski.rxjavafun.api.fakes

import com.github.pksokolowski.rxjavafun.api.JsonPlaceholderService
import com.github.pksokolowski.rxjavafun.api.models.Post

class PostsFakeService : JsonPlaceholderService {

    private val posts = FakeResource(
        listOf(
            Post(1, 1, "title", "some insightful dissertation...")
        ),
        1000
    )

    override fun getPosts() = posts.getObservable()
}