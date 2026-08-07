package com.anisync.android.widget.core

import android.content.Context
import com.anisync.android.data.account.AccountStore
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.TrendingDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Single Hilt entry point for every widget.
 *
 * Widgets have no component lifecycle, so they resolve dependencies by hand. This replaces the
 * near-identical per-widget entry point interfaces that existed before.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun airingScheduleDao(): AiringScheduleDao
    fun trendingDao(): TrendingDao
    fun libraryDao(): LibraryDao
    fun accountStore(): AccountStore
}

fun Context.widgetDeps(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/** Active AniList user id, or -1 when signed out. Library and schedule rows are scoped by this. */
fun WidgetEntryPoint.activeOwnerId(): Int = accountStore().activeAccount.value?.id ?: NO_OWNER

const val NO_OWNER = -1
