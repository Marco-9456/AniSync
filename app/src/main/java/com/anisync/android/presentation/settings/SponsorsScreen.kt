package com.anisync.android.presentation.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.Sponsor
import com.anisync.android.domain.SponsorTier
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.ui.theme.AppShapes
import com.anisync.android.ui.theme.ExpressiveShapes

private const val SPONSOR_URL = "https://github.com/sponsors/Marco-9456"

@Composable
fun SponsorsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SponsorsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    val isRefreshing = (state as? SponsorsViewModel.UiState.Ready)?.isRefreshing == true

    CollapsingTopBarScaffold(
        title = stringResource(R.string.sponsors_title),
        onBackClick = onBackClick,
        scrollableState = listState,
        modifier = modifier,
        actions = {
            RefreshAction(
                isRefreshing = isRefreshing,
                onClick = viewModel::refresh
            )
        }
    ) { topPadding ->
        when (val s = state) {
            SponsorsViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is SponsorsViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is SponsorsViewModel.UiState.Ready -> {
                SponsorsContent(
                    sponsors = s.sponsors,
                    updatedAt = s.updatedAt,
                    listState = listState,
                    topPadding = topPadding,
                    onSponsorClick = { uriHandler.openUri(it.url) },
                    onBecomeSponsor = { uriHandler.openUri(SPONSOR_URL) }
                )
            }
        }
    }
}

@Composable
private fun RefreshAction(
    isRefreshing: Boolean,
    onClick: () -> Unit
) {
    val rotation = if (isRefreshing) {
        val transition = rememberInfiniteTransition(label = "RefreshSpin")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
        angle
    } else 0f

    IconButton(onClick = onClick, enabled = !isRefreshing) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = stringResource(R.string.sponsors_refresh),
            modifier = Modifier.rotate(rotation)
        )
    }
}

@Composable
private fun SponsorsContent(
    sponsors: List<Sponsor>,
    updatedAt: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    topPadding: androidx.compose.ui.unit.Dp,
    onSponsorClick: (Sponsor) -> Unit,
    onBecomeSponsor: () -> Unit
) {
    val grouped = remember(sponsors) {
        sponsors
            .sortedByDescending { it.tier }
            .groupBy { SponsorTier.forAmount(it.tier) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = topPadding + 8.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item("hero") {
            HeroCard(onBecomeSponsor = onBecomeSponsor)
        }

        for (tier in SponsorTier.entries) {
            val entries = grouped[tier].orEmpty()
            item("tier_${tier.name}") {
                Spacer(modifier = Modifier.height(8.dp))
                TierBlock(
                    tier = tier,
                    sponsors = entries,
                    onSponsorClick = onSponsorClick,
                    onBecomeSponsor = onBecomeSponsor
                )
            }
        }

        item("updated") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.sponsors_updated_at, formatUpdatedAt(updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeroCard(onBecomeSponsor: () -> Unit) {
    Surface(
        shape = AppShapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.sponsors_hero_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.sponsors_hero_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onBecomeSponsor,
                    shape = ExpressiveShapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolunteerActivism,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sponsors_become_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TierBlock(
    tier: SponsorTier,
    sponsors: List<Sponsor>,
    onSponsorClick: (Sponsor) -> Unit,
    onBecomeSponsor: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = if (sponsors.isEmpty()) {
                stringResource(tier.label)
            } else {
                stringResource(R.string.sponsor_tier_count, stringResource(tier.label), sponsors.size)
            },
            level = HeaderLevel.Section,
            padding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (sponsors.isEmpty()) {
            EmptyTierCta(tier = tier, onClick = onBecomeSponsor)
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = ExpressiveShapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    sponsors.forEachIndexed { index, sponsor ->
                        SponsorRow(
                            sponsor = sponsor,
                            onClick = { onSponsorClick(sponsor) }
                        )
                        if (index < sponsors.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SponsorRow(
    sponsor: Sponsor,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(sponsor.avatarUrl) {
        ImageRequest.Builder(context)
            .data(sponsor.avatarUrl)
            .crossfade(true)
            .build()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.a11y_sponsor_avatar, sponsor.name),
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sponsor.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${sponsor.login}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = stringResource(R.string.sponsors_open_external),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyTierCta(
    tier: SponsorTier,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        shape = ExpressiveShapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.sponsors_empty_tier_cta, tier.minDollars),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatUpdatedAt(iso: String): String {
    return try {
        val instant = java.time.Instant.parse(iso)
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(java.time.ZoneOffset.UTC)
        formatter.format(instant)
    } catch (e: Exception) {
        iso
    }
}
