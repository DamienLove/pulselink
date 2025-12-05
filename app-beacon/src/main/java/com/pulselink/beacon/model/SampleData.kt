package com.pulselink.beacon.model

import androidx.compose.ui.graphics.Color
import com.pulselink.beacon.ui.Sender
import com.pulselink.beacon.ui.ThreadPreview
import com.pulselink.beacon.ui.message.Message
import com.pulselink.beacon.ui.message.MessageType

data class ContactDetails(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val trusted: Boolean
)

val sampleThreads = listOf(
    ThreadPreview(
        id = "aaron",
        title = "Aaron Loeb",
        subtitle = "Hello, how are you?",
        isVip = false,
        unread = false,
        sender = Sender("AL", Color(0xFF1E88E5))
    ),
    ThreadPreview(
        id = "adeline",
        title = "Adeline Palmerston",
        subtitle = "Thank you 😍",
        isVip = false,
        unread = false,
        sender = Sender("AP", Color(0xFF8E24AA))
    ),
    ThreadPreview(
        id = "daniel",
        title = "Daniel Gallego",
        subtitle = "Wow, that's amazing",
        isVip = true,
        unread = true,
        sender = Sender("DG", Color(0xFF1565C0))
    ),
    ThreadPreview(
        id = "juliana",
        title = "Juliana Silva",
        subtitle = "Nice works 👍🏻",
        isVip = false,
        unread = false,
        sender = Sender("JS", Color(0xFFAD1457))
    ),
    ThreadPreview(
        id = "pedro",
        title = "Pedro Fernandes",
        subtitle = "OK, see you tomorrow",
        isVip = false,
        unread = false,
        sender = Sender("PF", Color(0xFF00897B))
    ),
    ThreadPreview(
        id = "korina",
        title = "Korina Villanueva",
        subtitle = "I am fine, how about you?",
        isVip = true,
        unread = true,
        sender = Sender("KV", Color(0xFF5E35B1))
    ),
    ThreadPreview(
        id = "reese",
        title = "Reese Miller",
        subtitle = "Hello, how are you?",
        isVip = true,
        unread = false,
        sender = Sender("RM", Color(0xFF6D4C41))
    ),
    ThreadPreview(
        id = "group",
        title = "Marketing Team",
        subtitle = "7 Members",
        isVip = false,
        unread = false,
        sender = Sender("GT", Color(0xFF3F51B5))
    )
)

val sampleConversation = mapOf(
    "adeline" to listOf(
        Message(
            id = "m1",
            senderName = "Adeline",
            fromMe = false,
            type = MessageType.Text("Hi there, are you available around 4PM today for meeting with a new client?"),
            timestamp = "10.45 AM"
        ),
        Message(
            id = "m2",
            senderName = "Me",
            fromMe = true,
            type = MessageType.Text("Hey, Adeline"),
            timestamp = "10.46 AM"
        ),
        Message(
            id = "m3",
            senderName = "Me",
            fromMe = true,
            type = MessageType.Text("Sure, just give me a call!"),
            timestamp = "10.46 AM"
        ),
        Message(
            id = "m4",
            senderName = "Adeline",
            fromMe = false,
            type = MessageType.Text("Thank you 😍"),
            timestamp = "10.47 AM"
        )
    ),
    "group" to listOf(
        Message(
            id = "g1",
            senderName = "Ivy",
            fromMe = false,
            type = MessageType.Text("Hi, folks!"),
            timestamp = "10:10"
        ),
        Message(
            id = "g2",
            senderName = "Leo",
            fromMe = false,
            type = MessageType.Text("How are you today?"),
            timestamp = "10:12"
        ),
        Message(
            id = "g3",
            senderName = "Mia",
            fromMe = false,
            type = MessageType.Text("Im working on new design"),
            timestamp = "10:14"
        ),
        Message(
            id = "g4",
            senderName = "Me",
            fromMe = true,
            type = MessageType.Image,
            timestamp = "10:15"
        ),
        Message(
            id = "g5",
            senderName = "Me",
            fromMe = true,
            type = MessageType.Text("Wow, amazing 😍"),
            timestamp = "10:17"
        )
    )
)

val sampleContacts = mapOf(
    "aaron" to ContactDetails("aaron", "Aaron Loeb", "1(234)567-7890", "aaron@example.com", "123 Main St, Springfield, CA 90210", trusted = false),
    "adeline" to ContactDetails("adeline", "Adeline Palmerston", "1(234)567-7890", "adeline@contact.com", "12134 contacts address, city, ca 123456", trusted = false),
    "daniel" to ContactDetails("daniel", "Daniel Gallego", "1(234)567-7890", "daniel@example.com", "42 Baker St, London", trusted = true),
    "juliana" to ContactDetails("juliana", "Juliana Silva", "1(234)567-7890", "juliana@example.com", "99 Ocean Ave, Miami", trusted = false),
    "pedro" to ContactDetails("pedro", "Pedro Fernandes", "1(234)567-7890", "pedro@example.com", "77 Sunset Blvd, LA", trusted = false),
    "korina" to ContactDetails("korina", "Korina Villanueva", "1(234)567-7890", "korina@example.com", "88 Maple Rd, Austin", trusted = true),
    "reese" to ContactDetails("reese", "Reese Miller", "1(234)567-7890", "reese@example.com", "55 Pine St, Denver", trusted = true)
)
