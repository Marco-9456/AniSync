package com.anisync.android.presentation.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.Sponsor
import com.anisync.android.domain.SponsorTier
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.ui.theme.AppShapes
import com.anisync.android.ui.theme.ExpressiveShapes
import com.anisync.android.ui.theme.LocalAvatarShape
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val SPONSOR_URL = "https://github.com/sponsors/Marco-9456"
private const val KOFI_URL = "https://ko-fi.com/marco_9456"

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
        enableEnterAnimation = true,
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
                    AppCircularProgressIndicator()
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
                    onGitHubSponsorsClick = { uriHandler.openUri(SPONSOR_URL) },
                    onKofiClick = { uriHandler.openUri(KOFI_URL) }
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
    listState: LazyListState,
    topPadding: Dp,
    onSponsorClick: (Sponsor) -> Unit,
    onGitHubSponsorsClick: () -> Unit,
    onKofiClick: () -> Unit
) {
    val grouped = remember(sponsors) {
        sponsors
            .sortedByDescending { it.tier }
            .mapNotNull { s -> SponsorTier.forAmount(s.tier)?.let { it to s } }
            .groupBy({ it.first }, { it.second })
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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item("hero") {
            HeroCard(
                onGitHubSponsorsClick = onGitHubSponsorsClick,
                onKofiClick = onKofiClick
            )
        }

        for (tier in SponsorTier.entries) {
            val entries = grouped[tier].orEmpty()
            item("tier_${tier.name}") {
                TierBlock(
                    tier = tier,
                    sponsors = entries,
                    onSponsorClick = onSponsorClick,
                    onBecomeSponsor = onGitHubSponsorsClick
                )
            }
        }

        item("updated") {
            Text(
                text = stringResource(R.string.sponsors_updated_at, formatUpdatedAt(updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeroCard(
    onGitHubSponsorsClick: () -> Unit,
    onKofiClick: () -> Unit
) {
    Surface(
        shape = AppShapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.sponsors_hero_headline),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.sponsors_hero_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onGitHubSponsorsClick,
                    shape = ExpressiveShapes.pill,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sponsors_github_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                FilledTonalButton(
                    onClick = onKofiClick,
                    shape = ExpressiveShapes.pill,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_kofi),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sponsors_kofi_button),
                        style = MaterialTheme.typography.labelLarge
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
        Text(
            text = stringResource(tier.label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        if (sponsors.isEmpty()) {
            EmptyTierCta(onClick = onBecomeSponsor)
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = ExpressiveShapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    sponsors.forEach { sponsor ->
                        SponsorRow(
                            sponsor = sponsor,
                            onClick = { onSponsorClick(sponsor) }
                        )
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
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val cloverShape = LocalAvatarShape.current
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.a11y_sponsor_avatar, sponsor.name),
            modifier = Modifier
                .size(48.dp)
                .clip(cloverShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = cloverShape
                )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sponsor.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${sponsor.login}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyTierCta(
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = ExpressiveShapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = stringResource(R.string.sponsors_empty_tier_cta),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
        )
    }
}

private fun formatUpdatedAt(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC)
        formatter.format(instant)
    } catch (e: Exception) {
        iso
    }
}