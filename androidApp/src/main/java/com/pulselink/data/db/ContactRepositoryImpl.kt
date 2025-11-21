package com.pulselink.data.db

import com.pulselink.domain.model.Contact
import com.pulselink.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao
) : ContactRepository {
    override fun observeContacts(): Flow<List<Contact>> = contactDao.observeContacts()

    override suspend fun upsert(contact: Contact) {
        contactDao.upsert(contact)
    }

    override suspend fun delete(contactId: Long) {
        contactDao.deleteById(contactId)
    }

    override suspend fun getContact(contactId: Long): Contact? = contactDao.getById(contactId)

    override suspend fun getEmergencyContacts(): List<Contact> {
        return contactDao.getByTier("EMERGENCY")
    }

    override suspend fun getCheckInContacts(): List<Contact> {
        return contactDao.getByTier("CHECK_IN")
    }

    override suspend fun getByLinkCode(code: String): Contact? = contactDao.getByLinkCode(code)

    override suspend fun getByPhone(phone: String): Contact? = contactDao.getByPhone(phone)

    override suspend fun getByRemoteDeviceId(deviceId: String): Contact? =
        contactDao.getByRemoteDeviceId(deviceId)

    override suspend fun getByRemoteUid(remoteUid: String): Contact? =
        contactDao.getByRemoteUid(remoteUid)

    override suspend fun updateOrder(contactIds: List<Long>) {
        contactIds.forEachIndexed { index, id ->
            contactDao.updateOrder(id, index)
        }
    }

    override suspend fun getAll(): List<Contact> = contactDao.getAll()

    override suspend fun clear() {
        contactDao.clear()
    }
}
