package com.anisync.android.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing media details.
 * Returns a Flow that emits whenever details change.
 */
class GetMediaDetailsUseCase @Inject constructor(
    private val repository: DetailsRepository
) {
    operator fun invoke(id: Int): Flow<MediaDetails?> {
        return repository.observeMediaDetails(id)
    }
}
