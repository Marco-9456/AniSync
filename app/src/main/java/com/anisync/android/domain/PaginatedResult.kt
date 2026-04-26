package com.anisync.android.domain

/**
 * Wrapper for paginated API results.
 * Used by grid screens for infinite scroll pagination.
 */
data class PaginatedResult<T>(
    val items: List<T>,
    val hasNextPage: Boolean,
    val currentPage: Int,
    val totalPages: Int,
    val lastPage: Int = 0,
    val total: Int = 0,
)
