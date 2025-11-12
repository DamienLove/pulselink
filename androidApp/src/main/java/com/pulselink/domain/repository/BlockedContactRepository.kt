package com.pulselink.domain.repository

import com.pulselink.domain.model.BlockedContact
import kotlinx.coroutines.flow.Flow

interface BlockedContactRepository {
    suspend fun block(
        phoneNumber: String?,
        linkCode: String?,
        remoteDeviceId: String?,
        displayName: String
    )

    suspend fun isBlocked(
        phoneNumber: String?,
        linkCode: String?,
        remoteDeviceId: String?
    ): Boolean

    suspend fun unblock(id: Long)

    fun observeBlocked(): Flow<List<BlockedContact>>
}
