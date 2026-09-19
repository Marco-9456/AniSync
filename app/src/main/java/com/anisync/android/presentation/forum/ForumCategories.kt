package com.anisync.android.presentation.forum

import androidx.annotation.StringRes
import com.anisync.android.R

/**
 * One chip in the category strip. [id] is the AniList forum category id, or null for "All".
 *
 * The shipped screen carried these as hard-coded English literals, so none of them reached the
 * translated locales even though the Discover and Feed rails beside them did.
 */
data class ForumCategoryTab(val id: Int?, @param:StringRes val labelRes: Int)

/**
 * AniList's default forum categories, in the order the site lists them. Ids 6 and 14 do not exist.
 */
val forumCategoryTabs: List<ForumCategoryTab> = listOf(
    ForumCategoryTab(null, R.string.forum_category_all),
    ForumCategoryTab(1, R.string.forum_category_anime),
    ForumCategoryTab(2, R.string.forum_category_manga),
    ForumCategoryTab(3, R.string.forum_category_light_novels),
    ForumCategoryTab(4, R.string.forum_category_visual_novels),
    ForumCategoryTab(5, R.string.forum_category_release_discussion),
    ForumCategoryTab(7, R.string.forum_category_general),
    ForumCategoryTab(8, R.string.forum_category_news),
    ForumCategoryTab(9, R.string.forum_category_music),
    ForumCategoryTab(10, R.string.forum_category_gaming),
    ForumCategoryTab(11, R.string.forum_category_site_feedback),
    ForumCategoryTab(12, R.string.forum_category_bug_reports),
    ForumCategoryTab(13, R.string.forum_category_announcements),
    ForumCategoryTab(15, R.string.forum_category_recommendations),
    ForumCategoryTab(16, R.string.forum_category_forum_games),
    ForumCategoryTab(17, R.string.forum_category_misc),
    ForumCategoryTab(18, R.string.forum_category_anilist_apps),
)
