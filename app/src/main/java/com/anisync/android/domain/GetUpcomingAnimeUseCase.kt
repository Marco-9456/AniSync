package com.anisync.android.domain

import javax.inject.Inject

class GetUpcomingAnimeUseCase @Inject constructor(
    private val repository: DiscoverRepository
) {
    suspend operator fun invoke(): List<LibraryEntry> {
        return repository.getUpcomingAnime()
    }
}
