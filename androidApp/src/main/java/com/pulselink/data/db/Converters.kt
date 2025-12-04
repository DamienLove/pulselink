package com.pulselink.data.db

import androidx.room.TypeConverter
import com.pulselink.domain.model.MessageDirection

class Converters {
    @TypeConverter
    fun fromDirection(direction: MessageDirection): String = direction.name

    @TypeConverter
    fun toDirection(value: String): MessageDirection = MessageDirection.valueOf(value)
}
