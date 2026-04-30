package com.anisync.android.domain

data class NotificationPage(
    val items: List<Notification>,
    val hasNextPage: Boolean,
    val currentPage: Int
)
