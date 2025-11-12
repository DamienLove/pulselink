package com.pulselink.data.db

import com.pulselink.domain.model.BlockedContact
import com.pulselink.domain.repository.BlockedContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedContactRepositoryImpl @Inject constructor(
    private val blockedContactDao: BlockedContactDao
) : BlockedContactRepository {

    override suspend fun block(
        phoneNumber: String?,
        linkCode: String?,
        remoteDeviceId: String?,
        displayName: String
    ) {
        if (phoneNumber.isNullOrBlank() && linkCode.isNullOrBlank() && remoteDeviceId.isNullOrBlank()) {
            return
        }
        val entry = BlockedContact(
            phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
            linkCode = linkCode?.takeIf { it.isNotBlank() },
            remoteDeviceId = remoteDeviceId?.takeIf { it.isNotBlank() },
            displayName = displayName
        )
        blockedContactDao.insert(entry)
    }

    override suspend fun isBlocked(
        phoneNumber: String?,
        linkCode: String?,
        remoteDeviceId: String?
    ): Boolean {
        val phone = phoneNumber?.takeIf { it.isNotBlank() }
        val code = linkCode?.takeIf { it.isNotBlank() }
        val deviceId = remoteDeviceId?.takeIf { it.isNotBlank() }
        return blockedContactDao.isBlocked(phone, code, deviceId)
    }

    override suspend fun unblock(id: Long) {
        blockedContactDao.delete(id)
    }

    override fun observeBlocked(): Flow<List<BlockedContact>> = blockedContactDao.observeAll()
}
