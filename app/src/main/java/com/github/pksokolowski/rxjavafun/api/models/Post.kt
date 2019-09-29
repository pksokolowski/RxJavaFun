package com.github.pksokolowski.rxjavafun.api.models

import com.google.gson.annotations.SerializedName

class Post(
    @SerializedName("id")
    val id: Long,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("body")
    val body: String
)