package com.pulselink.domain.repository

import com.pulselink.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun observeContacts(): Flow<List<Contact>>
    suspend fun upsert(contact: Contact)
    suspend fun delete(contactId: Long)
    suspend fun getContact(contactId: Long): Contact?
    suspend fun getEmergencyContacts(): List<Contact>
    suspend fun getCheckInContacts(): List<Contact>
    suspend fun getByLinkCode(code: String): Contact?
    suspend fun getByPhone(phone: String): Contact?
    suspend fun updateOrder(contactIds: List<Long>)
}
