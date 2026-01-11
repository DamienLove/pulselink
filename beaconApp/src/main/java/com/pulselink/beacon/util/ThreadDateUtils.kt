package com.pulselink.beacon.util

import android.text.format.DateUtils
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.ui.Reaction
import com.pulselink.beacon.ui.ThreadUiItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ThreadDateUtils {

    // Regex for tapbacks (English default)
    // Matches: "Liked “Hello world”"
    private val reactionRegex = Regex("^(Liked|Loved|Disliked|Laughed at|Emphasized|Questioned) [“\"](.+)[”\"]$", RegexOption.DOT_MATCHES_ALL)

    private val emojiMap = mapOf(
        "Liked" to "👍",
        "Loved" to "❤️",
        "Disliked" to "👎",
        "Laughed at" to "😂",
        "Emphasized" to "!!",
        "Questioned" to "❓"
    )

    fun mapMessagesToUi(messages: List<SmsMessageItem>): List<ThreadUiItem> {
        if (messages.isEmpty()) return emptyList()

        // Input messages are sorted NEWEST -> OLDEST (Desc).
        // UI uses reverseLayout = true (Bottom -> Top).

        val uiItems = mutableListOf<ThreadUiItem>()
        var currentDay: LocalDate? = null

        // Pending reactions mapped by target text -> list of reactions
        val pendingReactions = mutableMapOf<String, MutableList<Reaction>>()

        // Iterate Newest -> Oldest
        for (i in messages.indices) {
            val msg = messages[i]
            val body = msg.body
            val match = reactionRegex.find(body)

            if (match != null) {
                // This is a reaction message.
                val type = match.groupValues[1]
                val targetText = match.groupValues[2]
                val emoji = emojiMap[type] ?: "👍"

                // Add to pending
                val reaction = Reaction(emoji, msg.address, msg.outgoing)

                if (!pendingReactions.containsKey(targetText)) {
                    pendingReactions[targetText] = mutableListOf()
                }
                pendingReactions[targetText]!!.add(reaction)

                // Do NOT add this message to UI items (hide it)
                continue
            }

            val msgDate = Instant.ofEpochMilli(msg.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            // If it's the first item, set current day
            if (currentDay == null) {
                currentDay = msgDate
            } else if (msgDate != currentDay) {
                 // Check if day changed
                 // We moved from Day A to Day B (older).
                 // Insert header for Day A.
                 uiItems.add(ThreadUiItem.DateHeader(formatDate(currentDay!!)))
                 currentDay = msgDate
            }

            // Attach reactions if any
            val myReactions = pendingReactions[body]?.toList() ?: emptyList()
            if (pendingReactions.containsKey(body)) {
                // We consume the reactions for this message instance.
                // Since we iterate Newest->Oldest, this attaches to the most recent message with that text.
                pendingReactions.remove(body)
            }

            uiItems.add(ThreadUiItem.Message(msg, myReactions))
        }

        // Add final header for the oldest group
        if (currentDay != null) {
            uiItems.add(ThreadUiItem.DateHeader(formatDate(currentDay!!)))
        }

        return uiItems
    }

    private fun formatDate(date: LocalDate): String {
        val now = LocalDate.now()
        return when {
            date.isEqual(now) -> "Today"
            date.isEqual(now.minusDays(1)) -> "Yesterday"
            date.year == now.year -> date.format(DateTimeFormatter.ofPattern("MMM d"))
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }
}
