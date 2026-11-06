package com.rojas.consumodatos.data.mapper

import com.rojas.consumodatos.data.local.ContactEntity
import com.rojas.consumodatos.data.remote.ContactDto
import com.rojas.consumodatos.domain.model.Contact

fun ContactEntity.toDomain() = Contact(id = id, name = name, phone = phone)
fun Contact.toEntity() = ContactEntity(id = id, name = name, phone = phone)
fun ContactDto.toEntity() = ContactEntity(id = id ?: 0, name = name, phone = phone)
fun Contact.toDto() = ContactDto(id = if (id == 0L) null else id, name = name, phone = phone)