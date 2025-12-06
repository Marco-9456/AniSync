package com.anisync.android.domain

import com.anisync.android.type.MediaType
import javax.inject.Inject

class GetTrendingAnimeUseCase @Inject constructor(
    private val repository: DiscoverRepository
) {
    suspend operator fun invoke(): List<LibraryEntry> {
        return repository.getTrending(MediaType.ANIME)
    }
}
