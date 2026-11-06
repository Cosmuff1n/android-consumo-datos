package com.rojas.consumodatos.data.remote

import javax.inject.Inject

interface ContactRemoteDataSource {
    suspend fun fetchContacts(): List<ContactDto>
    suspend fun addContact(contact: ContactDto): ContactDto
}

class ContactRemoteDataSourceImpl @Inject constructor(
    private val apiService: ContactApiService
) : ContactRemoteDataSource {
    override suspend fun fetchContacts(): List<ContactDto> = apiService.getContacts()
    override suspend fun addContact(contact: ContactDto): ContactDto = apiService.createContact(contact)
}