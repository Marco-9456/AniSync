package com.anisync.android.data.update

import java.io.File

/**
 * Represents a GitHub release with the information needed for an in-app update.
 */
data class Release(
    val tagName: String,
    val prerelease: Boolean,
    val body: String,
    val downloadUrl: String
)

/**
 * Represents the current state of the update flow.
 *
 * State machine:
 * ```
 * Idle -> Checking -> UpdateAvailable -> Downloading -> ReadyToInstall
 *                  -> Idle (up to date)
 *                  -> Idle (error)
 * ```
 */
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpdateAvailable(val release: Release) : UpdateState()
    data class Downloading(val release: Release, val progress: Int) : UpdateState()
    data class ReadyToInstall(val release: Release, val file: File) : UpdateState()
}

/**
 * One-shot result returned from [UpdateManager.checkForUpdate].
 * Allows callers to distinguish between "up to date" and "error"
 * for appropriate user feedback (e.g., toast only on manual check).
 */
sealed class UpdateCheckResult {
    data class Available(val release: Release) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val exception: Exception) : UpdateCheckResult()
}
