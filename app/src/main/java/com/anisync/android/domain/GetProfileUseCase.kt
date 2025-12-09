package com.anisync.android.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing user profile.
 * Returns a Flow that emits whenever profile data changes.
 */
class GetProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke(): Flow<UserProfile?> {
        return repository.observeProfile()
    }
}
