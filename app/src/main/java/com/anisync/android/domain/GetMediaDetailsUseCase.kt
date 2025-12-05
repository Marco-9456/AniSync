package com.anisync.android.domain

import javax.inject.Inject

class GetMediaDetailsUseCase @Inject constructor(
    private val repository: DetailsRepository
) {
    suspend operator fun invoke(id: Int): MediaDetails? {
        return repository.getMediaDetails(id)
    }
}
