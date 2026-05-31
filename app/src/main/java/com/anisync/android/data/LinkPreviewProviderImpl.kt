package com.anisync.android.data

import com.anisync.android.GetLinkPreviewsQuery
import com.anisync.android.GetUserPreviewQuery
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.domain.LinkPreview
import com.anisync.android.domain.LinkPreviewProvider
import com.anisync.android.domain.parser.LinkPreviewKey
import com.anisync.android.domain.parser.RichTextBlock
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkPreviewProviderImpl @Inject constructor(
    private val mediaDetailsDao: MediaDetailsDao,
    private val apolloClient: ApolloClient
) : LinkPreviewProvider {

    override suspend fun getPreviews(
        links: List<RichTextBlock.AnilistLink>
    ): Map<LinkPreviewKey, LinkPreview> {
        if (links.isEmpty()) return emptyMap()

        val uniqueLinks = links.distinctBy { it.previewKey }
        val result = mutableMapOf<LinkPreviewKey, LinkPreview>()

        // Phase 1: Check Room cache for media (anime/manga)
        val mediaLinks = uniqueLinks.filter { it.type == "anime" || it.type == "manga" }
        val uncachedMediaIds = mutableListOf<Int>()

        for (link in mediaLinks) {
            val entity = mediaDetailsDao.getById(link.id)
            // Only serve from cache when the cover color is also present. Rows cached before the
            // coverColor column existed (v16) have a null color; falling through to the network
            // fetch lets those cards pick up their accent tint instead of rendering grey.
            if (entity != null && entity.coverColor != null) {
                result[link.previewKey] = LinkPreview(
                    title = entity.titleUserPreferred,
                    imageUrl = entity.coverUrl,
                    coverColor = entity.coverColor,
                    subtitle = buildMediaSubtitle(entity.format, entity.seasonYear ?: entity.year)
                )
            } else {
                uncachedMediaIds.add(link.id)
            }
        }

        // Phase 2: Collect character/staff/user IDs that need network fetch
        val characterIds = uniqueLinks
            .filter { it.type == "character" }
            .map { it.id }
            .distinct()
        val staffIds = uniqueLinks
            .filter { it.type == "staff" }
            .map { it.id }
            .distinct()
        val userNames = uniqueLinks
            .filter { it.type == "user" }
            .mapNotNull { it.slug }
            .distinct()

        // Phase 3: Batch fetch uncached media + characters + staff from API
        if (uncachedMediaIds.isNotEmpty() || characterIds.isNotEmpty() || staffIds.isNotEmpty()) {
            try {
                val response = apolloClient.query(
                    GetLinkPreviewsQuery(
                        mediaIds = Optional.presentIfNotNull(uncachedMediaIds.takeIf { it.isNotEmpty() }),
                        characterIds = Optional.presentIfNotNull(characterIds.takeIf { it.isNotEmpty() }),
                        staffIds = Optional.presentIfNotNull(staffIds.takeIf { it.isNotEmpty() })
                    )
                ).execute()

                response.data?.mediaPage?.media?.filterNotNull()?.forEach { media ->
                    val id = media.id
                    val type = media.type?.name?.lowercase() ?: "anime"
                    result[LinkPreviewKey(type, id)] = LinkPreview(
                        title = media.title?.userPreferred ?: return@forEach,
                        imageUrl = media.coverImage?.large,
                        coverColor = media.coverImage?.color,
                        subtitle = buildMediaSubtitle(media.format?.rawValue, media.seasonYear)
                    )
                }

                response.data?.characterPage?.characters?.filterNotNull()?.forEach { character ->
                    val id = character.id
                    result[LinkPreviewKey("character", id)] = LinkPreview(
                        title = character.name?.userPreferred ?: return@forEach,
                        imageUrl = character.image?.large
                    )
                }

                response.data?.staffPage?.staff?.filterNotNull()?.forEach { staff ->
                    val id = staff.id
                    result[LinkPreviewKey("staff", id)] = LinkPreview(
                        title = staff.name?.userPreferred ?: return@forEach,
                        imageUrl = staff.image?.large
                    )
                }

            } catch (_: Exception) {
                // Network failure — callers will fall back to slug-derived titles
            }
        }

        // Users can't be batched and are addressed by username, so fetch each individually by name.
        for (name in userNames) {
            try {
                val user = apolloClient.query(GetUserPreviewQuery(Optional.present(name)))
                    .execute().data?.user ?: continue
                // Key by the URL username (matches AnilistLink.previewKey), not the canonical name.
                result[LinkPreviewKey("user", 0, name)] = LinkPreview(
                    title = user.name,
                    imageUrl = user.avatar?.large
                )
            } catch (_: Exception) {
                // Skip on failure — caller falls back to slug-derived title
            }
        }

        return result
    }

    /** Media secondary line, e.g. "TV · 2024". Either part may be missing. */
    private fun buildMediaSubtitle(format: String?, year: Int?): String? {
        val parts = listOfNotNull(format?.let(::prettyFormat), year?.toString())
        return parts.joinToString(" · ").takeIf { it.isNotBlank() }
    }

    private fun prettyFormat(raw: String): String = when (raw.uppercase()) {
        "TV" -> "TV"
        "TV_SHORT" -> "TV Short"
        "OVA" -> "OVA"
        "ONA" -> "ONA"
        "MOVIE" -> "Movie"
        "SPECIAL" -> "Special"
        "MUSIC" -> "Music"
        "MANGA" -> "Manga"
        "NOVEL" -> "Light Novel"
        "ONE_SHOT" -> "One-shot"
        else -> raw.lowercase().replaceFirstChar { it.uppercase() }
    }
}
