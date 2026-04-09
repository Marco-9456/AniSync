package com.anisync.android.data

import com.anisync.android.GetLinkPreviewsQuery
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
            if (entity != null) {
                result[link.previewKey] = LinkPreview(
                    title = entity.titleUserPreferred,
                    imageUrl = entity.coverUrl
                )
            } else {
                uncachedMediaIds.add(link.id)
            }
        }

        // Phase 2: Collect character/staff IDs that need network fetch
        val characterIds = uniqueLinks
            .filter { it.type == "character" }
            .map { it.id }
            .distinct()
        val staffIds = uniqueLinks
            .filter { it.type == "staff" }
            .map { it.id }
            .distinct()

        // Phase 3: Batch fetch uncached media + characters from API
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
                        imageUrl = media.coverImage?.large
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

        return result
    }
}
