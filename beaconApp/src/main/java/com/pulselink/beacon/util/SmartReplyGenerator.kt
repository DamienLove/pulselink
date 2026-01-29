package com.pulselink.beacon.util

object SmartReplyGenerator {

    fun generateReplies(lastMessage: String): List<String> {
        val lower = lastMessage.trim().lowercase()
        val replies = mutableListOf<String>()

        // 1. Direct Questions
        if (lower.startsWith("where") || lower.contains("location")) {
            replies.add("I'm at home.")
            replies.add("On my way.")
            replies.add("At work.")
            replies.add("Sharing location...")
        } else if (lower.startsWith("when") || lower.contains("time")) {
            replies.add("In 5 mins.")
            replies.add("Soon.")
            replies.add("Tonight.")
            replies.add("Not sure yet.")
        } else if (lower.startsWith("how are") || lower.contains("doing?")) {
            replies.add("Good, you?")
            replies.add("Great!")
            replies.add("Busy right now.")
            replies.add("Doing well.")
        } else if (lower.startsWith("what are") || lower.contains("doing?")) {
            replies.add("Just chilling.")
            replies.add("Working.")
            replies.add("Driving.")
            replies.add("Nothing much.")
        }

        // 2. Binary Questions
        if (replies.isEmpty() && (lower.startsWith("are you") || lower.startsWith("do you") || lower.startsWith("can you") || lower.endsWith("?"))) {
            replies.add("Yes")
            replies.add("No")
            replies.add("Maybe")
            replies.add("Not yet")
            replies.add("Sure!")
        }

        // 3. Greetings / Common Phrases
        if (replies.isEmpty()) {
            if (lower.contains("hello") || lower.contains("hi ")) {
                replies.add("Hey!")
                replies.add("Hi there.")
                replies.add("What's up?")
            } else if (lower.contains("thank")) {
                replies.add("You're welcome!")
                replies.add("No problem.")
                replies.add("Anytime.")
            } else if (lower.contains("sorry")) {
                replies.add("It's okay.")
                replies.add("No worries.")
            } else if (lower.contains("bye") || lower.contains("see you")) {
                replies.add("Bye!")
                replies.add("See ya.")
                replies.add("Talk later.")
            } else if (lower.contains("lol") || lower.contains("haha")) {
                replies.add("\uD83D\uDE02") // Joy
                replies.add("Haha")
                replies.add("Right?")
            }
        }

        // 4. Default Fallbacks (if absolutely nothing matches)
        if (replies.isEmpty()) {
            replies.add("Ok")
            replies.add("Sounds good")
            replies.add("Thanks")
            replies.add("\uD83D\uDC4D") // Thumbs up
        }

        return replies.distinct().take(3)
    }
}
