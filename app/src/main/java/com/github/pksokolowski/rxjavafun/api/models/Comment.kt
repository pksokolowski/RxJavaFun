package com.github.pksokolowski.rxjavafun.api.models


class Comment(
    val postId: Long,
    val id: Long,
    val name: String,
    val email: String,
    val body: String
)