package com.pulselink.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_contacts")
data class BlockedContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String? = null,
    val linkCode: String? = null,
    val remoteDeviceId: String? = null,
    val displayName: String = "",
    val blockedAt: Long = System.currentTimeMillis()
) {
    init {
        require(
            !phoneNumber.isNullOrBlank() ||
                !linkCode.isNullOrBlank() ||
                !remoteDeviceId.isNullOrBlank()
        ) { "BlockedContact must include at least one identifier" }
    }
}
