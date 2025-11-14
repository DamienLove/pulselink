package com.pulselink.domain.repository

interface BetaAgreementRepository {
    suspend fun recordAgreement(name: String, agreementVersion: String)
}
