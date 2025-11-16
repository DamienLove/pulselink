# PulseLink CHROME OS DO NOT MERGE

Alpha APK is:

[![Latest APK](https://github.com/DamienLove/pulselink/actions/workflows/verify-main.yml/badge.svg)](https://github.com/DamienLove/pulselink/actions/workflows/verify-main.yml)

## [Download PulseLink Alpha](androidApp/build/outputs/apk/free/debug/androidApp-free-debug.apk)

PulseLink is a personal safety app that listens for discreet trigger phrases and instantly escalates alerts to trusted contacts with location context.

## Modules
- androidApp: Android client (Compose + background services).

## Quickstart
1. Install JDK 17 (Temurin recommended).
2. Generate Gradle wrapper jars by running `gradle wrapper --gradle-version 8.6` from this directory (one-time step, Gradle CLI required).
3. `./gradlew androidApp:assembleDebug`

## Status
Fresh rebuild created on November 2, 2025. Messaging pathways remain fully enabled.
