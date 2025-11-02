package com.pulselink.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val phoneNumber: String,
    val escalationTier: EscalationTier = EscalationTier.EMERGENCY,
    val includeLocation: Boolean = true,
    val autoCall: Boolean = false,
    val emergencySoundKey: String? = null,
    val checkInSoundKey: String? = null,
    val cameraEnabled: Boolean = false,
    val contactOrder: Int = 0
)

@Serializable
enum class EscalationTier { EMERGENCY, CHECK_IN }
