package com.pulselink.beacon.util

enum class MagicTone {
    PROFESSIONAL, FUN, SHAKESPEARE, COMPACT
}

object MagicComposeHelper {

    fun rewrite(text: String, tone: MagicTone): String {
        if (text.isBlank()) return text
        return when (tone) {
            MagicTone.PROFESSIONAL -> makeProfessional(text)
            MagicTone.FUN -> makeFun(text)
            MagicTone.SHAKESPEARE -> makeShakespeare(text)
            MagicTone.COMPACT -> makeCompact(text)
        }
    }

    private fun makeProfessional(text: String): String {
        var res = text
        // Expand contractions
        res = res.replace(Regex("\\b(im|i'm)\\b", RegexOption.IGNORE_CASE), "I am")
        res = res.replace(Regex("\\b(cant|can't)\\b", RegexOption.IGNORE_CASE), "cannot")
        res = res.replace(Regex("\\b(dont|don't)\\b", RegexOption.IGNORE_CASE), "do not")
        res = res.replace(Regex("\\b(wont|won't)\\b", RegexOption.IGNORE_CASE), "will not")
        res = res.replace(Regex("\\b(idk)\\b", RegexOption.IGNORE_CASE), "I am unsure")
        res = res.replace(Regex("\\b(thx|thanks)\\b", RegexOption.IGNORE_CASE), "Thank you")
        res = res.replace(Regex("\\b(u)\\b", RegexOption.IGNORE_CASE), "you")
        res = res.replace(Regex("\\b(ur)\\b", RegexOption.IGNORE_CASE), "your")
        res = res.replace(Regex("\\b(hey|hi)\\b", RegexOption.IGNORE_CASE), "Greetings")

        // Punctuation
        if (!res.endsWith(".")) res += "."

        return res
    }

    private fun makeFun(text: String): String {
        var res = text
        res = res.replace(Regex("\\b(hello|hi|hey)\\b", RegexOption.IGNORE_CASE), "Yo!")
        res = res.replace(Regex("\\b(yes|ok|okay)\\b", RegexOption.IGNORE_CASE), "Yasss!")
        res = res.replace(Regex("\\b(no)\\b", RegexOption.IGNORE_CASE), "Nope \uD83D\uDE45")
        res = res.replace(Regex("\\b(good)\\b", RegexOption.IGNORE_CASE), "Awesome \uD83C\uDF89")
        res = res.replace(Regex("\\b(bad)\\b", RegexOption.IGNORE_CASE), "Bummer \uD83D\uDE1E")
        res = res.replace(Regex("\\b(love)\\b", RegexOption.IGNORE_CASE), "Looooove \uD83D\uDE0D")

        // Add emoji
        if (!res.contains(Regex("[\\p{So}]"))) {
             res += " \uD83D\uDE0E"
        }
        return res
    }

    private fun makeShakespeare(text: String): String {
        var res = text
        res = res.replace(Regex("\\b(you)\\b", RegexOption.IGNORE_CASE), "thou")
        res = res.replace(Regex("\\b(your)\\b", RegexOption.IGNORE_CASE), "thy")
        res = res.replace(Regex("\\b(are)\\b", RegexOption.IGNORE_CASE), "art")
        res = res.replace(Regex("\\b(do)\\b", RegexOption.IGNORE_CASE), "dost")
        res = res.replace(Regex("\\b(does)\\b", RegexOption.IGNORE_CASE), "doth")
        res = res.replace(Regex("\\b(my)\\b", RegexOption.IGNORE_CASE), "mine")
        res = res.replace(Regex("\\b(hello|hi)\\b", RegexOption.IGNORE_CASE), "Hail")
        res = res.replace(Regex("\\b(friend)\\b", RegexOption.IGNORE_CASE), "kinsman")

        return "$res, verily."
    }

    private fun makeCompact(text: String): String {
        var res = text
        res = res.replace(Regex("\\b(are)\\b", RegexOption.IGNORE_CASE), "r")
        res = res.replace(Regex("\\b(you)\\b", RegexOption.IGNORE_CASE), "u")
        res = res.replace(Regex("\\b(your|you're)\\b", RegexOption.IGNORE_CASE), "ur")
        res = res.replace(Regex("\\b(because)\\b", RegexOption.IGNORE_CASE), "cuz")
        res = res.replace(Regex("\\b(be)\\b", RegexOption.IGNORE_CASE), "b")
        res = res.replace(Regex("\\b(see)\\b", RegexOption.IGNORE_CASE), "c")
        res = res.replace(Regex("\\b(at)\\b", RegexOption.IGNORE_CASE), "@")
        res = res.replace(Regex("\\b(and)\\b", RegexOption.IGNORE_CASE), "&")

        // Remove vowels from long words? Maybe too aggressive.
        // Just return shortened version.
        return res
    }
}
