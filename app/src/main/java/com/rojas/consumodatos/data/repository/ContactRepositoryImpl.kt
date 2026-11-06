package com.rojas.consumodatos.data.repository

import com.rojas.consumodatos.data.local.ContactLocalDataSource
import com.rojas.consumodatos.data.mapper.toDomain
import com.rojas.consumodatos.data.mapper.toEntity
import com.rojas.consumodatos.data.remote.ContactRemoteDataSource
import com.rojas.consumodatos.domain.model.Contact
import com.rojas.consumodatos.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val localDataSource: ContactLocalDataSource,
    private val remoteDataSource: ContactRemoteDataSource
) : ContactRepository {

    override fun getContacts(): Flow<List<Contact>> {
        return localDataSource.getContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addContact(name: String, phone: String) {
        val contact = Contact(name = name, phone = phone)
        localDataSource.saveContact(contact.toEntity())
    }

    override suspend fun syncRemoteContacts() {
        runCatching {
            val remoteDtos = remoteDataSource.fetchContacts()
            val entities = remoteDtos.map { it.toEntity() }
            localDataSource.saveContacts(entities)
        }
    }

    override suspend fun deleteContact(id: Long) {
        localDataSource.deleteContact(id)
    }
}