package com.pulselink.data.db

import com.pulselink.domain.model.ContactMessage
import com.pulselink.domain.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val dao: ContactMessageDao
) : MessageRepository {
    override fun observeForContact(contactId: Long): Flow<List<ContactMessage>> =
        dao.observeForContact(contactId)

    override suspend fun record(message: ContactMessage) {
        dao.insert(message)
    }

    override suspend fun clear(contactId: Long) {
        dao.clear(contactId)
    }
}
