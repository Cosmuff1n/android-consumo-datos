package com.rojas.consumodatos.data.remote

import com.google.gson.annotations.SerializedName

data class ContactDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String
)