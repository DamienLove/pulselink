package com.pulselink.domain.model

import androidx.annotation.RawRes

const val SOUND_RES_PHONE_DEFAULT_NOTIFICATION = -1
const val SOUND_RES_PHONE_DEFAULT_RINGTONE = -2

data class SoundOption(
    val key: String,
    val label: String,
    @RawRes val resId: Int,
    val category: SoundCategory
)

enum class SoundCategory { SIREN, CHIME, CALL }
