package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.StaffInfo
import com.anisync.android.domain.VoiceActor
import com.anisync.android.type.CharacterSort
import com.anisync.android.type.StaffSort
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.details.components.StaffItem
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.ui.theme.emphasis

/** Grid vs. list layout for the Characters / Staff tabs. */
enum class PeopleViewMode { GRID, LIST }

/** The opposite layout, for the view-toggle button. */
fun PeopleViewMode.toggled(): PeopleViewMode =
    if (this == PeopleViewMode.GRID) PeopleViewMode.LIST else PeopleViewMode.GRID

/**
 * Sort options for the Staff tab. Each maps to AniList [StaffSort] and is applied SERVER-SIDE
 * (the list refetches from page 1 on change). Unlike the cast, [RELEVANCE] maps to the
 * moderator-curated order `[RELEVANCE, ID]` — creator/director/design/music first, matching the
 * website — because the staff connection's unsorted default is unhelpful.
 */
enum class StaffSortOption(val label: String, val apiSort: List<StaffSort>?) {
    RELEVANCE("Relevance", listOf(StaffSort.RELEVANCE, StaffSort.ID)),
    MOST_FAVOURITES("Most favourites", listOf(StaffSort.FAVOURITES_DESC)),
    LEAST_FAVOURITES("Least favourites", listOf(StaffSort.FAVOURITES)),
    NEWEST("Newest", listOf(StaffSort.ID_DESC)),
    OLDEST("Oldest", listOf(StaffSort.ID))
}

/**
 * Sort options for the Characters tab. Each maps to AniList [CharacterSort] and is applied
 * SERVER-SIDE: [apiSort] is passed to the characters query and the list refetches from page 1
 * on change, so favourite/id ranking reflects the whole cast, not just the loaded pages.
 * [RELEVANCE] sends no sort, yielding AniList's default (moderator/relevance) order — the order
 * the website shows.
 */
enum class CharacterSortOption(val label: String, val apiSort: List<CharacterSort>?) {
    RELEVANCE("Relevance", null),
    ROLE("Role", listOf(CharacterSort.ROLE, CharacterSort.FAVOURITES_DESC)),
    FAVOURITES("Most favourites", listOf(CharacterSort.FAVOURITES_DESC)),
    LEAST_FAVOURITES("Least favourites", listOf(CharacterSort.FAVOURITES)),
    NEWEST("Newest", listOf(CharacterSort.ID_DESC)),
    OLDEST("Oldest", listOf(CharacterSort.ID))
}

/** Role filter for the Characters tab (maps to AniList CharacterRole). */
enum class CharacterRoleFilter(val label: String, val apiName: String?) {
    ALL("All roles", null),
    MAIN("Main", "MAIN"),
    SUPPORTING("Supporting", "SUPPORTING"),
    BACKGROUND("Background", "BACKGROUND")
}

private val PORTRAIT_ASPECT = 5f / 7f

// ---- Pure filtering / sorting helpers ----

/**
 * Apply the role filter only. Sorting is server-side (see [CharacterSortOption]); the role
 * filter stays client-side because the AniList `characters` connection has no role argument.
 */
fun List<CharacterInfo>.applyCharacterRole(role: CharacterRoleFilter): List<CharacterInfo> =
    if (role.apiName == null) this else filter { it.role.equals(role.apiName, ignoreCase = true) }

/**
 * Distinct voice-actor languages across the loaded cast, Japanese first then alphabetical —
 * drives the Characters language picker. Empty for manga / casts with no VAs (hides the chip).
 */
fun availableVoiceActorLanguages(characters: List<CharacterInfo>): List<String> {
    val languages = characters
        .flatMap { it.voiceActors }
        .mapNotNull { it.language?.takeIf { lang -> lang.isNotBlank() } }
        .distinct()
    return languages.sortedWith(
        compareByDescending<String> { it.equals("Japanese", ignoreCase = true) }
            .thenBy { it.lowercase() }
    )
}

/** The voice actor for [language], or the first VA when no language is selected/matches. */
fun CharacterInfo.voiceActorFor(language: String?): VoiceActor? {
    if (voiceActors.isEmpty()) return null
    return language?.let { lang -> voiceActors.firstOrNull { it.language.equals(lang, ignoreCase = true) } }
        ?: voiceActors.first()
}

// ---- LazyListScope emitters (the tab body lives in the shared details LazyColumn) ----

@OptIn(ExperimentalSharedTransitionApi::class)
fun LazyListScope.castTabContent(
    characters: List<CharacterInfo>,
    columns: Int,
    viewMode: PeopleViewMode,
    language: String?,
    isLoadingMore: Boolean,
    onCharacterClick: (Int) -> Unit,
    onVoiceActorClick: (Int) -> Unit,
    filterBar: @Composable () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    item(key = "cast_filter_bar") { filterBar() }

    if (characters.isEmpty()) {
        item(key = "cast_empty") { PeopleEmptyState(stringResource(R.string.empty_no_characters)) }
        return
    }

    when (viewMode) {
        PeopleViewMode.GRID -> {
            val rows = characters.chunked(columns)
            itemsIndexedKeyed(rows, keyPrefix = "cast_grid_row") { row ->
                PeopleGridRow {
                    row.forEach { character ->
                        Box(modifier = Modifier.weight(1f)) {
                            CastGridCard(
                                character = character,
                                voiceActor = character.voiceActorFor(language),
                                onClick = { onCharacterClick(character.id) },
                                onVoiceActorClick = onVoiceActorClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        PeopleViewMode.LIST -> {
            items(
                count = characters.size,
                key = { i -> "cast_list_${characters[i].id}_${characters[i].role}" }
            ) { i ->
                val character = characters[i]
                CastListCard(
                    character = character,
                    voiceActor = character.voiceActorFor(language),
                    onClick = { onCharacterClick(character.id) },
                    onVoiceActorClick = onVoiceActorClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (isLoadingMore) {
        item(key = "cast_loading") { PeopleLoadingFooter() }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun LazyListScope.staffTabContent(
    staff: List<StaffInfo>,
    columns: Int,
    viewMode: PeopleViewMode,
    isLoadingMore: Boolean,
    onStaffClick: (Int) -> Unit,
    filterBar: @Composable () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    item(key = "staff_filter_bar") { filterBar() }

    if (staff.isEmpty()) {
        item(key = "staff_empty") { PeopleEmptyState(stringResource(R.string.empty_no_staff)) }
        return
    }

    when (viewMode) {
        PeopleViewMode.GRID -> {
            val rows = staff.chunked(columns)
            itemsIndexedKeyed(rows, keyPrefix = "staff_grid_row") { row ->
                PeopleGridRow {
                    row.forEach { member ->
                        Box(modifier = Modifier.weight(1f)) {
                            StaffItem(
                                staff = member,
                                onClick = { onStaffClick(member.id) },
                                fillCell = true,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        PeopleViewMode.LIST -> {
            items(
                count = staff.size,
                key = { i -> "staff_list_${staff[i].id}_${staff[i].role}" }
            ) { i ->
                StaffListCard(
                    staff = staff[i],
                    onClick = { onStaffClick(staff[i].id) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (isLoadingMore) {
        item(key = "staff_loading") { PeopleLoadingFooter() }
    }
}

/** [androidx.compose.foundation.lazy.itemsIndexed] with a stable string key per row. */
private fun <T> LazyListScope.itemsIndexedKeyed(
    items: List<T>,
    keyPrefix: String,
    itemContent: @Composable (T) -> Unit
) {
    items(count = items.size, key = { i -> "${keyPrefix}_$i" }) { i -> itemContent(items[i]) }
}

@Composable
private fun PeopleGridRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    // The caller emits exactly row.size weighted cells then tops up the remaining columns with
    // weighted spacers, so a short final row keeps each cell at its column width (no stretching).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        content()
    }
}

@Composable
private fun PeopleEmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PeopleLoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AppCircularProgressIndicator()
    }
}

// ---- Filter bars ----

@Composable
fun CastFilterBar(
    viewMode: PeopleViewMode,
    sort: CharacterSortOption,
    role: CharacterRoleFilter,
    language: String?,
    languageActive: Boolean,
    onToggleView: () -> Unit,
    onSortClick: () -> Unit,
    onRoleClick: () -> Unit,
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PeopleFilterBarRow(viewMode = viewMode, onToggleView = onToggleView, modifier = modifier) {
        SheetChip(label = "Sort · ${sort.label}", active = sort != CharacterSortOption.RELEVANCE, onClick = onSortClick)
        SheetChip(
            label = if (role == CharacterRoleFilter.ALL) "Role" else "Role · ${role.label}",
            active = role != CharacterRoleFilter.ALL,
            onClick = onRoleClick
        )
        // Only when the cast carries VAs (manga / VA-less casts hide the chip). Shows the effective
        // language; "active" means the viewer explicitly changed it from the default.
        if (language != null) {
            SheetChip(
                label = "Language · $language",
                active = languageActive,
                onClick = onLanguageClick
            )
        }
    }
}

@Composable
fun StaffFilterBar(
    viewMode: PeopleViewMode,
    sort: StaffSortOption,
    onToggleView: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PeopleFilterBarRow(viewMode = viewMode, onToggleView = onToggleView, modifier = modifier) {
        SheetChip(label = "Sort · ${sort.label}", active = sort != StaffSortOption.RELEVANCE, onClick = onSortClick)
    }
}

@Composable
private fun PeopleFilterBarRow(
    viewMode: PeopleViewMode,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier,
    chips: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips()
        }
        IconButton(onClick = onToggleView) {
            Icon(
                imageVector = if (viewMode == PeopleViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                contentDescription = if (viewMode == PeopleViewMode.GRID) "List view" else "Grid view",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SheetChip(label: String, active: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

// ---- Cards ----

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CastGridCard(
    character: CharacterInfo,
    voiceActor: VoiceActor?,
    onClick: () -> Unit,
    onVoiceActorClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier
) {
    val imageShape = RoundedCornerShape(16.dp)
    Column(modifier = modifier) {
        Box {
            val imageBase = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(PORTRAIT_ASPECT)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = TransitionKeys.characterImage(character.id)
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition = OverlayClip(imageShape)
                        )
                        .clip(imageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                }
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(PORTRAIT_ASPECT)
                    .clip(imageShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            }
            AsyncImage(
                model = character.imageUrl,
                contentDescription = stringResource(R.string.a11y_character_image, character.nameUserPreferred),
                contentScale = ContentScale.Crop,
                modifier = imageBase.bouncyClickable(onClick = onClick)
            )
            if (voiceActor?.imageUrl != null) {
                AsyncImage(
                    model = voiceActor.imageUrl,
                    contentDescription = voiceActor.nameUserPreferred,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .bouncyClickable(onClick = { onVoiceActorClick(voiceActor.id) })
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = character.nameUserPreferred,
            style = MaterialTheme.typography.labelMedium.emphasis(),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = character.role.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CastListCard(
    character: CharacterInfo,
    voiceActor: VoiceActor?,
    onClick: () -> Unit,
    onVoiceActorClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(14.dp)
    val thumbShape = RoundedCornerShape(10.dp)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = cardShape,
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick, clipShape = cardShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersonThumb(
                imageUrl = character.imageUrl,
                shape = thumbShape,
                transitionKey = TransitionKeys.characterImage(character.id),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.nameUserPreferred,
                    style = MaterialTheme.typography.titleSmall.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = character.role.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (voiceActor != null) {
                Spacer(Modifier.width(12.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = voiceActor.nameUserPreferred,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    voiceActor.language?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                AsyncImage(
                    model = voiceActor.imageUrl,
                    contentDescription = voiceActor.nameUserPreferred,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(56.dp)
                        .aspectRatio(PORTRAIT_ASPECT)
                        .clip(thumbShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .bouncyClickable(onClick = { onVoiceActorClick(voiceActor.id) }, clipShape = thumbShape)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StaffListCard(
    staff: StaffInfo,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(14.dp)
    val thumbShape = RoundedCornerShape(10.dp)
    val roleText = staff.role.takeIf { it.isNotBlank() } ?: staff.primaryOccupations.firstOrNull().orEmpty()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = cardShape,
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick, clipShape = cardShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersonThumb(
                imageUrl = staff.imageUrl,
                shape = thumbShape,
                transitionKey = TransitionKeys.staffImage(staff.id),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = staff.nameUserPreferred,
                    style = MaterialTheme.typography.titleSmall.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (roleText.isNotEmpty()) {
                    Text(
                        text = roleText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PersonThumb(
    imageUrl: String?,
    shape: RoundedCornerShape,
    transitionKey: Any,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    val base = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .width(56.dp)
                .aspectRatio(PORTRAIT_ASPECT)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = transitionKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                    clipInOverlayDuringTransition = OverlayClip(shape)
                )
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        }
    } else {
        Modifier
            .width(56.dp)
            .aspectRatio(PORTRAIT_ASPECT)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    }
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = base
    )
}
