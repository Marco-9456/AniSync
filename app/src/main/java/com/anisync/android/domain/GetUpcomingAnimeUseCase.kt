package com.anisync.android.domain

import com.anisync.android.type.MediaType
import javax.inject.Inject

class GetUpcomingAnimeUseCase @Inject constructor(
    private val repository: DiscoverRepository
) {
    suspend operator fun invoke(): Result<List<LibraryEntry>> {
        return repository.getUpcoming(MediaType.ANIME)
    }
}
