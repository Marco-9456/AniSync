package com.anisync.android.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anisync.android.R

/**
 * Predefined empty state configurations for common scenarios in the app.
 * Each configuration provides appropriate icon, title, description, and optional CTA.
 */
object EmptyStateConfigs {
    
    /**
     * Empty library list (Watching, Completed, etc.)
     */
    @Composable
    fun LibraryEmpty(
        statusLabel: String,
        onBrowseClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyStateWithAction(
            icon = Icons.Default.VideoLibrary,
            title = stringResource(R.string.empty_list_title, statusLabel),
            description = stringResource(R.string.empty_list_anime_action),
            actionLabel = stringResource(R.string.empty_list_anime_button),
            onActionClick = onBrowseClick,
            modifier = modifier
        )
    }
    
    /**
     * Empty manga library
     */
    @Composable
    fun MangaLibraryEmpty(
        statusLabel: String,
        onBrowseClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyStateWithAction(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = stringResource(R.string.empty_list_title, statusLabel),
            description = stringResource(R.string.empty_list_manga_action),
            actionLabel = stringResource(R.string.empty_list_manga_button),
            onActionClick = onBrowseClick,
            modifier = modifier
        )
    }
    
    /**
     * No search results found
     */
    @Composable
    fun SearchNoResults(
        query: String,
        onClearClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyStateWithAction(
            icon = Icons.Default.SearchOff,
            title = stringResource(R.string.empty_search_results_title, query),
            description = stringResource(R.string.empty_search_results_action),
            actionLabel = stringResource(R.string.empty_search_results_button),
            onActionClick = onClearClick,
            modifier = modifier
        )
    }
    
    /**
     * No search results - compact version without clear action
     */
    @Composable
    fun SearchNoResultsCompact(
        query: String,
        modifier: Modifier = Modifier
    ) {
        EmptyStateCompact(
            icon = Icons.Default.SearchOff,
            title = stringResource(R.string.empty_search_results_title, query),
            description = stringResource(R.string.empty_search_alt_action),
            modifier = modifier
        )
    }
    
    /**
     * No favorites added yet
     */
    @Composable
    fun NoFavorites(
        onBrowseClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyStateWithAction(
            icon = Icons.Default.FavoriteBorder,
            title = stringResource(R.string.empty_favorites_title),
            description = stringResource(R.string.empty_favorites_action),
            actionLabel = stringResource(R.string.empty_favorites_button),
            onActionClick = onBrowseClick,
            modifier = modifier
        )
    }
    
    /**
     * Network/connection error
     */
    @Composable
    fun NetworkError(
        onRetryClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyStateWithAction(
            icon = Icons.Default.CloudOff,
            title = stringResource(R.string.empty_connection_title),
            description = stringResource(R.string.empty_connection_action),
            actionLabel = stringResource(R.string.retry),
            onActionClick = onRetryClick,
            modifier = modifier
        )
    }
    
    /**
     * Generic error state
     */
    @Composable
    fun GenericError(
        message: String,
        onRetryClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyStateWithAction(
            icon = Icons.Default.CloudOff,
            title = stringResource(R.string.empty_generic_title),
            description = message,
            actionLabel = stringResource(R.string.retry),
            onActionClick = onRetryClick,
            modifier = modifier
        )
    }
    
    /**
     * User not logged in
     */
    @Composable
    fun NotLoggedIn(
        onLoginClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyStateWithAction(
            icon = Icons.Default.Person,
            title = stringResource(R.string.empty_sign_in_title),
            description = stringResource(R.string.empty_sign_in_action),
            actionLabel = stringResource(R.string.empty_sign_in_button),
            onActionClick = onLoginClick,
            modifier = modifier
        )
    }
    
    /**
     * No airing episodes today
     */
    @Composable
    fun NoAiringToday(
        modifier: Modifier = Modifier
    ) {
        EmptyStateCompact(
            icon = Icons.Default.CalendarMonth,
            title = stringResource(R.string.empty_airing_today_title),
            description = stringResource(R.string.empty_airing_today_action),
            modifier = modifier
        )
    }
    
    /**
     * No upcoming episodes in watch list
     */
    @Composable
    fun NoUpcomingEpisodes(
        onBrowseClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyStateWithAction(
            icon = Icons.Default.Schedule,
            title = stringResource(R.string.empty_upcoming_title),
            description = stringResource(R.string.empty_upcoming_action),
            actionLabel = stringResource(R.string.empty_upcoming_button),
            onActionClick = onBrowseClick,
            modifier = modifier
        )
    }
    
    /**
     * No notifications
     */
    @Composable
    fun NoNotifications(
        modifier: Modifier = Modifier
    ) {
        EmptyStateCompact(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.empty_notifications_title),
            description = stringResource(R.string.empty_notifications_action),
            modifier = modifier
        )
    }
    
    /**
     * Empty character list for a media
     */
    @Composable
    fun NoCharacters(
        modifier: Modifier = Modifier
    ) {
        EmptyStateCompact(
            icon = Icons.Default.Person,
            title = stringResource(R.string.empty_characters_title),
            description = stringResource(R.string.empty_characters_action),
            modifier = modifier
        )
    }

    /**
     * No forum threads found
     */
    @Composable
    fun ForumNoThreads(
        onCreateClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        EmptyStateWithAction(
            icon = Icons.Default.Forum,
            title = stringResource(R.string.empty_discussions_title),
            description = stringResource(R.string.empty_discussions_action),
            actionLabel = if (onCreateClick != null) stringResource(R.string.empty_discussions_button) else null,
            onActionClick = onCreateClick,
            modifier = modifier
        )
    }

    /**
     * No comments on a forum thread
     */
    @Composable
    fun ForumNoComments(
        modifier: Modifier = Modifier
    ) {
        EmptyStateCompact(
            icon = Icons.Default.ChatBubbleOutline,
            title = stringResource(R.string.empty_comments_title),
            description = stringResource(R.string.empty_comments_action),
            modifier = modifier
        )
    }

    /**
     * No forum search results
     */
    @Composable
    fun ForumSearchNoResults(
        query: String,
        modifier: Modifier = Modifier
    ) {
        EmptyStateCompact(
            icon = Icons.Default.SearchOff,
            title = stringResource(R.string.empty_forum_search_title, query),
            description = stringResource(R.string.empty_forum_search_action),
            modifier = modifier
        )
    }
}
