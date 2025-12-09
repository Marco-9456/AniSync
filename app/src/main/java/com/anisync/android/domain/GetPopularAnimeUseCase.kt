package com.anisync.android.domain

import com.anisync.android.type.MediaType
import javax.inject.Inject

class GetPopularAnimeUseCase @Inject constructor(
    private val repository: DiscoverRepository
) {
    suspend operator fun invoke(): Result<List<LibraryEntry>> {
        return repository.getPopular(MediaType.ANIME)
    }
}
