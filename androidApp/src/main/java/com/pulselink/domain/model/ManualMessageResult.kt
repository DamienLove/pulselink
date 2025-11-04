package com.pulselink.domain.model
sealed class ManualMessageResult {
    data class Success(val overrideApplied: Boolean) : ManualMessageResult()

    data class Failure(val reason: Reason) : ManualMessageResult() {
        enum class Reason {
            CONTACT_MISSING,
            NOT_LINKED,
            SMS_FAILED,
            UNKNOWN
        }
    }
}

