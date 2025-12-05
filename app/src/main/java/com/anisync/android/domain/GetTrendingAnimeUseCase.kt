package com.anisync.android.domain

import javax.inject.Inject

class GetTrendingAnimeUseCase @Inject constructor(
    private val repository: DiscoverRepository
) {
    suspend operator fun invoke(): List<LibraryEntry> {
        return repository.getTrendingAnime()
    }
}
