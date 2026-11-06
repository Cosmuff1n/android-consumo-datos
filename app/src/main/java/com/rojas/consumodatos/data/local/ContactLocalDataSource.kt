package com.rojas.consumodatos.data.local

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ContactLocalDataSource {
    fun getContacts(): Flow<List<ContactEntity>>
    suspend fun saveContact(contact: ContactEntity)
    suspend fun saveContacts(contacts: List<ContactEntity>)
    suspend fun deleteContact(id: Long)
}

class ContactLocalDataSourceImpl @Inject constructor(
    private val contactDao: ContactDao
) : ContactLocalDataSource {
    override fun getContacts(): Flow<List<ContactEntity>> = contactDao.getAllContacts()
    override suspend fun saveContact(contact: ContactEntity) = contactDao.insertContact(contact)
    override suspend fun saveContacts(contacts: List<ContactEntity>) = contactDao.insertContacts(contacts)
    override suspend fun deleteContact(id: Long) = contactDao.deleteContactById(id)
}