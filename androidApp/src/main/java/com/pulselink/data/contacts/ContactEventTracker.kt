package com.pulselink.data.contacts

import com.pulselink.domain.model.Contact
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Serializable
data class ContactEvent(
    val type: EventType,
    val timestamp: Long
)

enum class EventType { CALL, SMS }

class ContactEventTracker @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    fun addEvent(
        contact: Contact,
        eventType: EventType,
        timestamp: Long = System.currentTimeMillis()
    ): Contact {
        val currentEvents = try {
            json.decodeFromString<MutableList<ContactEvent>>(contact.lastContactEvents)
        } catch (e: Exception) {
            mutableListOf()
        }

        currentEvents.add(ContactEvent(eventType, timestamp))

        val cutoff = timestamp - TimeUnit.MINUTES.toMillis(contact.bypassDndWindowMinutes.toLong())
        val prunedEvents = currentEvents.filter { it.timestamp >= cutoff }.toMutableList()

        return contact.copy(lastContactEvents = json.encodeToString(prunedEvents))
    }

    fun countEvents(contact: Contact, eventType: EventType, windowMinutes: Int): Int {
        val currentEvents = try {
            json.decodeFromString<List<ContactEvent>>(contact.lastContactEvents)
        } catch (e: Exception) {
            emptyList()
        }

        val cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(windowMinutes.toLong())
        return currentEvents.count { it.type == eventType && it.timestamp >= cutoff }
    }
}
