package com.anisync.android.domain

import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(username: String): UserProfile? {
        return repository.getProfile(username)
    }
}
