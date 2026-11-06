package com.rojas.consumodatos.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ContactApiService {
    @GET("contacts")
    suspend fun getContacts(): List<ContactDto>

    @POST("contacts")
    suspend fun createContact(@Body contact: ContactDto): ContactDto
}