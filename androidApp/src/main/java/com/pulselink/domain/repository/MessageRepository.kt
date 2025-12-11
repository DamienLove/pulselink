package com.pulselink.domain.repository

import com.pulselink.domain.model.ContactMessage
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeForContact(contactId: Long): Flow<List<ContactMessage>>
    suspend fun record(message: ContactMessage)
    suspend fun clear(contactId: Long)
    suspend fun clearAll()
    suspend fun getUnreadEmergencyMessageCount(contactIds: List<Long>, since: Long): Int
}
