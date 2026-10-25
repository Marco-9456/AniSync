package com.anisync.android.presentation.forum

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Announcement
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.anisync.android.R

/**
 * One chip in the category strip. [id] is the AniList forum category id, or null for "All".
 *
 * The labels were hard-coded English literals on the shipped screen, so none of them reached the
 * translated locales even though the Discover and Feed rails beside them did.
 */
data class ForumCategoryTab(
    val id: Int?,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
)

/**
 * AniList's forum categories by id. Id 6 never existed, 14 is retired, and Misc (17) was merged into
 * General in October 2026.
 */
val forumCategoryTabs: List<ForumCategoryTab> = listOf(
    ForumCategoryTab(null, R.string.forum_category_all, Icons.Default.Forum),
    ForumCategoryTab(1, R.string.forum_category_anime, Icons.Default.PlayArrow),
    ForumCategoryTab(2, R.string.forum_category_manga, Icons.AutoMirrored.Filled.MenuBook),
    ForumCategoryTab(3, R.string.forum_category_light_novels, Icons.AutoMirrored.Filled.LibraryBooks),
    ForumCategoryTab(4, R.string.forum_category_visual_novels, Icons.Default.VisibilityOff),
    ForumCategoryTab(5, R.string.forum_category_release_discussion, Icons.Default.RateReview),
    ForumCategoryTab(7, R.string.forum_category_general, Icons.Default.Public),
    ForumCategoryTab(8, R.string.forum_category_news, Icons.Default.Newspaper),
    ForumCategoryTab(9, R.string.forum_category_music, Icons.Default.MusicNote),
    ForumCategoryTab(10, R.string.forum_category_gaming, Icons.Default.Gamepad),
    ForumCategoryTab(11, R.string.forum_category_site_feedback, Icons.Default.Feedback),
    ForumCategoryTab(12, R.string.forum_category_bug_reports, Icons.Default.BugReport),
    ForumCategoryTab(13, R.string.forum_category_announcements, Icons.AutoMirrored.Filled.Announcement),
    ForumCategoryTab(15, R.string.forum_category_recommendations, Icons.Default.ThumbUp),
    ForumCategoryTab(16, R.string.forum_category_forum_games, Icons.Default.Casino),
    ForumCategoryTab(18, R.string.forum_category_anilist_apps, Icons.Default.Apps),
    ForumCategoryTab(19, R.string.forum_category_events, Icons.Default.Celebration),
)

/** The AniList category id the Overview's third section previews. */
const val RELEASE_DISCUSSION_CATEGORY_ID = 5
