package com.github.pksokolowski.rxjavafun.api.fakes

import com.github.pksokolowski.rxjavafun.api.SomeService
import com.github.pksokolowski.rxjavafun.api.models.Post
import com.github.pksokolowski.rxjavafun.api.models.User
import javax.inject.Inject

class PostsFakeService @Inject constructor() : SomeService {

    private val users = listOf(
        User(1, "Czeslaw", "Niewomen"),
        User(2, "Steve", "Careers")
    )

    private val posts = listOf(
        Post(1, 1, "title", "some insightful dissertation..."),
        Post(2, 1, "Tru story", "And then, I saw the Yeti in its full glory"),
        Post(3, 2, "m2 seen it", "Yea, confirmed")
    )


    override fun getUsers() = fakeResource { users }

    override fun getPosts() = fakeResource { posts }

    override fun getPostsByUserId(userId: Long) = fakeResource { posts.filter { it.userId == userId } }

}