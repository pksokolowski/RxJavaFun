package com.github.pksokolowski.rxjavafun.api.models

import com.google.gson.annotations.SerializedName

class User(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("surname")
    val suranme: String
)