package com.rojas.consumodatos.domain.repository

import com.rojas.consumodatos.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContacts(): Flow<List<Contact>>
    suspend fun addContact(name: String, phone: String)
    suspend fun syncRemoteContacts()
    suspend fun deleteContact(id: Long)
}