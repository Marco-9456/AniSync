package com.anisync.android.presentation.details.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.CoverImage
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.ListIndicator
import com.anisync.android.presentation.components.ListIndicatorStyle
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.LocalLibraryStatuses
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.ui.theme.emphasis
import java.text.NumberFormat
import java.util.Locale

/**
 * Shared building blocks for the character and staff detail screens. Both screens are the same
 * shell — hero, facts, names, biography, tabbed list — so everything that is not screen-specific
 * lives here rather than being written twice.
 */

/**
 * Every row in every tab is this tall. A list whose cards breathe differently because one title
 * wrapped and the next did not reads as broken, so the title takes one line and ellipsises.
 */
private val PersonRowHeight = 96.dp

private val CardShape = RoundedCornerShape(16.dp)
internal val RowShape = RoundedCornerShape(12.dp)
private val ThumbShape = RoundedCornerShape(8.dp)

/** One key/value fact rendered as an icon tile inside [PersonFactsCard]. */
@Immutable
data class PersonFact(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color
)

/** A tab in [PersonTabs]; [count] is shown as a badge when known. */
@Immutable
data class PersonTab(val label: String, val count: Int?)

/**
 * Banner, portrait and identity in one block. Replaces the old split of a blurred hero plus a
 * separate name card, which pushed the first real content below the fold.
 *
 * The backdrop is the banner of a related title when there is one; falling back to the portrait
 * itself is deliberately *not* done here — the caller passes null and gets a flat surface instead
 * of the same picture twice.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PersonHero(
    imageUrl: String?,
    backdropUrl: String?,
    name: String,
    nativeName: String?,
    metaLine: String?,
    favourites: Int?,
    contentDescription: String,
    transitionKey: String,
    onImageClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backdropCredit: String? = null,
    /** Drawn in the portrait slot instead of [imageUrl]. A studio hands in its cover mark here. */
    imageContent: (@Composable () -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val bannerHeight = 220.dp
    val portraitWidth = 120.dp
    val portraitTop = 150.dp
    val portraitStart = 24.dp
    val textStart = portraitStart + portraitWidth + 16.dp
    val portraitShape = RoundedCornerShape(12.dp)
    val portraitHeight = portraitWidth * 7 / 5

    val portraitModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        val spatialSpec = AppMotion.rememberSpatialSpec()
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = transitionKey),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                clipInOverlayDuringTransition = OverlayClip(portraitShape)
            )
        }
    } else {
        Modifier
    }

    Box(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        if (backdropUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        }

        // Top scrim keeps the app bar icons legible on a bright banner; the bottom one dissolves
        // the banner into the page instead of ending on a hard edge.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.28f to Color.Transparent,
                        0.5f to MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                        0.78f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )

        if (backdropCredit != null) {
            Text(
                text = backdropCredit,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .widthIn(max = 200.dp)
                    .padding(end = 24.dp, top = bannerHeight - 30.dp)
            )
        }

        val portraitSlot = Modifier
            .padding(start = portraitStart, top = portraitTop)
            .width(portraitWidth)
            .aspectRatio(5f / 7f)
            .then(portraitModifier)
            .clip(portraitShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onImageClick != null) Modifier.clickable(onClick = onImageClick) else Modifier)

        if (imageContent != null) {
            Box(
                modifier = portraitSlot.semantics { this.contentDescription = contentDescription }
            ) {
                imageContent()
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = portraitSlot
            )
        }

        Column(
            modifier = Modifier
                .padding(
                    start = textStart,
                    end = 16.dp,
                    top = portraitTop + portraitHeight - 100.dp
                )
                .fillMaxWidth()
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (nativeName != null) {
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = nativeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!metaLine.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.labelMedium.emphasis(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (favourites != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.getDefault()).format(favourites),
                        style = MaterialTheme.typography.titleSmall.emphasis(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.person_favourites_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * The old attributes list was up to eight label/value rows with dividers, which on a staff member
 * pushed every list below the fold. The same facts fit in half the height as a two-column tile
 * grid, and it is the pattern the media details screen already uses.
 */
@Composable
fun PersonFactsCard(
    facts: List<PersonFact>,
    modifier: Modifier = Modifier
) {
    if (facts.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                facts.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { fact ->
                            PersonFactTile(fact = fact, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonFactTile(fact: PersonFact, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(fact.tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fact.icon,
                contentDescription = null,
                tint = fact.tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = fact.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = fact.value,
                style = MaterialTheme.typography.labelLarge.emphasis(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Entry point to [PersonNamesSheet]. Alternative names used to be a comma-joined grey paragraph
 * inside the name card — visible but never scannable and impossible to copy one of.
 */
@Composable
fun PersonNamesRow(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.person_names_row),
                style = MaterialTheme.typography.labelLarge.emphasis(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** One label/value line in the names sheet, with its own copy action. */
@Composable
private fun PersonNameRow(
    label: String,
    value: String,
    onCopy: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy(value) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.emphasis(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = stringResource(R.string.a11y_action_copy),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Every name AniList holds for this person: the preferred/romaji/native trio, then each alternative
 * as its own copyable chip. Spoiler aliases stay behind a shutter — AniList marks them separately
 * and they give away plot points (a character's true identity, for instance).
 */
@Composable
fun PersonNamesSheetContent(
    subject: String,
    preferredName: String,
    fullName: String,
    nativeName: String?,
    alternativeNames: List<String>,
    spoilerNames: List<String>,
    onCopy: (String) -> Unit,
    onCopyAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var spoilersRevealed by rememberSaveable { mutableStateOf(false) }
    val total = listOfNotNull(preferredName, fullName.takeIf { it != preferredName }, nativeName)
        .size + alternativeNames.size + spoilerNames.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.person_names_row),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.person_names_subtitle, subject, total),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        PersonSectionLabel(stringResource(R.string.person_names_primary))
        Spacer(Modifier.height(8.dp))
        Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                PersonNameRow(
                    label = stringResource(R.string.person_names_preferred),
                    value = preferredName,
                    onCopy = onCopy
                )
                if (fullName != preferredName) {
                    PersonNameRow(
                        label = stringResource(R.string.person_names_romaji),
                        value = fullName,
                        onCopy = onCopy
                    )
                }
                nativeName?.let {
                    PersonNameRow(
                        label = stringResource(R.string.person_names_native),
                        value = it,
                        onCopy = onCopy
                    )
                }
            }
        }

        if (alternativeNames.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            PersonSectionLabel(
                stringResource(R.string.person_names_alternative) + " · ${alternativeNames.size}"
            )
            Spacer(Modifier.height(8.dp))
            PersonNameChips(names = alternativeNames, onCopy = onCopy)
        }

        if (spoilerNames.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            if (spoilersRevealed) {
                PersonSectionLabel(
                    stringResource(R.string.person_names_spoilers) + " · ${spoilerNames.size}"
                )
                Spacer(Modifier.height(8.dp))
                PersonNameChips(names = spoilerNames, onCopy = onCopy)
            } else {
                Surface(
                    shape = ThumbShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    onClick = { spoilersRevealed = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(
                                R.string.person_names_spoilers_hidden,
                                spoilerNames.size
                            ),
                            style = MaterialTheme.typography.labelMedium.emphasis(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            onClick = onCopyAll,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.person_names_copy_all),
                    style = MaterialTheme.typography.labelLarge.emphasis(),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonNameChips(names: List<String>, onCopy: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        names.forEach { alias ->
            Surface(
                shape = ThumbShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                onClick = { onCopy(alias) }
            ) {
                Text(
                    text = alias,
                    style = MaterialTheme.typography.labelLarge.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun PersonSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.emphasis(),
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

/**
 * The screen's tab bar. Both screens carry two tabs and the count belongs on the tab — it is the
 * answer to "is this list worth opening" and the old screens never showed it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonTabs(
    tabs: List<PersonTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        tabs.forEachIndexed { index, tab ->
            val checked = index == selectedIndex
            ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(index) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (tab.count != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (checked) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    ) {
                        Text(
                            text = compactCount(tab.count),
                            style = MaterialTheme.typography.labelSmall.emphasis(),
                            color = if (checked) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/** A chip that opens a menu of [options]; reads as applied whenever it is not on its default. */
@Composable
fun PersonDropdownChip(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    appliedWhenNotDefault: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val applied = appliedWhenNotDefault && selectedIndex != 0

    Box(modifier = modifier) {
        FilterChip(
            selected = applied,
            onClick = { expanded = true },
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.emphasis(),
                    maxLines = 1
                )
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        Menu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                item(
                    text = option,
                    selected = index == selectedIndex,
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** A chip that toggles a single filter on and off, with the Material check on when applied. */
@Composable
fun PersonToggleChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.emphasis(),
                maxLines = 1
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}

/**
 * One title a character appears in. The voice cast for *this* title rides along on the row, which
 * is the whole point: the old screen flattened every actor into a separate tab where it was
 * impossible to tell who voiced the character in which show.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppearanceRow(
    mediaId: Int,
    coverUrl: String?,
    cover: CoverImage?,
    title: String,
    meta: String,
    role: String?,
    roleHighlighted: Boolean = role.equals("MAIN", ignoreCase = true),
    score: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transitionPrefix: String = TransitionKeys.CHARACTER,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val coverModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        val spatialSpec = AppMotion.rememberSpatialSpec()
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = TransitionKeys.cover(transitionPrefix, mediaId)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                clipInOverlayDuringTransition = OverlayClip(ThumbShape)
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(PersonRowHeight),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cover.url() ?: coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(56.dp)
                    .height(80.dp)
                    .then(coverModifier)
                    .clip(ThumbShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!role.isNullOrBlank()) {
                        RolePill(role = role, highlighted = roleHighlighted)
                    }
                    if (score != null && score > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StarGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = score.toString(),
                                style = MaterialTheme.typography.labelSmall.emphasis(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    LocalLibraryStatuses.current[mediaId]?.let { status ->
                        ListIndicator(
                            status = status,
                            type = null,
                            style = ListIndicatorStyle.Chip
                        )
                    }
                }
            }
        }
    }
}

/** MAIN / SUPPORTING, tinted so the leading role reads first. */
@Composable
fun RolePill(
    role: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = role.equals("MAIN", ignoreCase = true)
) {
    val isMain = highlighted
    Surface(
        shape = CircleShape,
        color = if (isMain) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        modifier = modifier
    ) {
        Text(
            text = role.replace('_', ' ').uppercase(),
            style = MaterialTheme.typography.labelSmall.emphasis(),
            color = if (isMain) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * One character a staff member voices. The title they are best known for is on the row and the rest
 * are a count — the old card expanded inline into every appearance, which for a long-running role
 * meant dozens of rows unfolding inside a five-item preview.
 */
@Composable
fun PersonCharacterRow(
    name: String,
    nativeName: String?,
    imageUrl: String?,
    role: String?,
    primaryTitle: String?,
    otherTitles: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(PersonRowHeight),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(56.dp)
                    .height(80.dp)
                    .clip(ThumbShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (nativeName != null) {
                    Text(
                        text = nativeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!role.isNullOrBlank()) RolePill(role = role)
                    if (primaryTitle != null) {
                        Text(
                            text = primaryTitle,
                            style = MaterialTheme.typography.labelSmall.emphasis(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (otherTitles > 0) {
                        Text(
                            text = stringResource(R.string.person_media_and_more, otherTitles),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** One production credit: the role is the headline, the title carries it. */
@Composable
fun CreditRow(
    title: String,
    role: String?,
    meta: String,
    coverUrl: String?,
    cover: CoverImage?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(PersonRowHeight),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cover.url() ?: coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(56.dp)
                    .height(80.dp)
                    .clip(ThumbShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!role.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = role,
                        style = MaterialTheme.typography.labelSmall.emphasis(),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * One voice actor, shaped like an appearance row so the two tabs read as one list in two moods:
 * portrait, name, the language they voice in, and how many of this character's titles they carry.
 */
@Composable
fun VoiceActorRow(
    name: String,
    nativeName: String?,
    language: String?,
    titleCount: Int,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(PersonRowHeight),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(56.dp)
                    .height(80.dp)
                    .clip(ThumbShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = language.orEmpty().uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.person_titles_count,
                                titleCount,
                                titleCount
                            ),
                            style = MaterialTheme.typography.labelSmall.emphasis(),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (!nativeName.isNullOrBlank()) {
                        Text(
                            text = nativeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * End of what has been loaded. Composing this is what asks for the next page — a button to reach
 * row seven was a tap the reader should never have had to make.
 */
@Composable
fun PersonListFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // The app's own spinner rather than a bare CircularProgressIndicator: every other screen
        // loads with the wavy one, and so does the design.
        AppCircularProgressIndicator()
    }
}

/** Small note strip used to explain a section that looks emptier than the user expects. */
@Composable
fun PersonNoteStrip(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PersonEmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    )
}

/**
 * Lays [items] out in [columns] columns inside a lazy list. One column is the phone case and
 * behaves exactly like `items`; a tablet pairs rows up instead of stretching one row across a
 * 700dp pane.
 */
fun <T> LazyListScope.personGridItems(
    items: List<T>,
    columns: Int,
    key: (T) -> Any,
    spacing: Dp = 12.dp,
    rowModifier: Modifier = Modifier,
    itemContent: @Composable RowScope.(T) -> Unit
) {
    val rows = items.chunked(columns)
    items(
        items = rows,
        key = { row: List<T> -> key(row.first()) }
    ) { row: List<T> ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = rowModifier.fillMaxWidth()
        ) {
            row.forEach { item -> itemContent(item) }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Birthday facts arrive as `M/D` or `M/D/Y`; render them the way a person would say them. */
fun formatPersonBirthday(raw: String): String {
    val parts = raw.split("/")
    val month = parts.getOrNull(0)?.toIntOrNull() ?: return raw
    val day = parts.getOrNull(1)?.toIntOrNull() ?: return raw
    if (month !in 1..12) return raw
    val monthName = MONTHS[month - 1]
    val year = parts.getOrNull(2)
    return if (year != null) "$monthName $day, $year" else "$monthName $day"
}

private val MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

/** 1,204 stays 1,204; 12,400 becomes 12.4k so a tab badge never wraps. */
private fun compactCount(value: Int): String = when {
    value < 10_000 -> NumberFormat.getNumberInstance(Locale.getDefault()).format(value)
    value < 1_000_000 -> "${value / 1000}k"
    else -> "${value / 1_000_000}m"
}

/** Icon + tint pairs for the fact tiles, kept here so both screens agree. */
object PersonFactIcons {
    val gender: ImageVector = Icons.Default.Person
    val birthday: ImageVector = Icons.Default.Cake
}
