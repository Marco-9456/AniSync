package com.anisync.android.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
            title = "Your $statusLabel list is empty",
            description = "Start tracking anime by adding shows from the Discover tab",
            actionLabel = "Browse Anime",
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
            title = "Your $statusLabel list is empty",
            description = "Start tracking manga by adding titles from the Discover tab",
            actionLabel = "Browse Manga",
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
            title = "No results for \"$query\"",
            description = "Try adjusting your search or filters",
            actionLabel = "Clear Search",
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
            title = "No results for \"$query\"",
            description = "Try different keywords",
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
            title = "No favorites yet",
            description = "Add anime or manga to your favorites from their detail pages",
            actionLabel = "Discover New Shows",
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
            title = "Connection problem",
            description = "Check your internet connection and try again",
            actionLabel = "Retry",
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
            title = "Something went wrong",
            description = message,
            actionLabel = "Retry",
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
            title = "Sign in to AniList",
            description = "Connect your AniList account to sync your library across devices",
            actionLabel = "Sign In",
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
            title = "No episodes airing today",
            description = "Check back later or add more shows to your list",
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
            title = "No upcoming episodes",
            description = "Add currently airing anime to your watching list to see upcoming episodes",
            actionLabel = "Browse Airing",
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
            title = "No notifications",
            description = "You're all caught up!",
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
            title = "No characters listed",
            description = "Character information is not available for this title",
            modifier = modifier
        )
    }
}
