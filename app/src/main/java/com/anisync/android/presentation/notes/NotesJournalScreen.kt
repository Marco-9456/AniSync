package com.anisync.android.presentation.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.EmptyStateWithAction
import com.anisync.android.presentation.forum.components.SearchField
import com.anisync.android.presentation.profile.util.formatProfileRelativeTime
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.util.getTitle

/**
 * The Notes journal — a personal, searchable diary of every entry the viewer has annotated. Reached
 * from the Library top bar. Surfaces notes that were previously buried inside the edit sheet (#75).
 *
 * Uses the app's native vocabulary throughout: the flat page-toned chrome of the Calendar screen,
 * the shared [SearchField] pill, and [LibraryListCard]-style cards so it reads as part of the same app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesJournalScreen(
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pageColor = MaterialTheme.colorScheme.surfaceContainer

    Scaffold(
        modifier = modifier,
        containerColor = pageColor,
        topBar = {
            Column(modifier = Modifier.background(pageColor)) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.notes_journal_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = pageColor)
                )
                if (uiState.totalCount > 0) {
                    SearchField(
                        query = uiState.query,
                        onQueryChange = viewModel::onQueryChange,
                        placeholder = stringResource(R.string.notes_journal_search_hint),
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.spacing_medium),
                            vertical = dimensionResource(R.dimen.spacing_small)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.totalCount == 0 -> {
                    EmptyStateWithAction(
                        icon = ImageVector.vectorResource(R.drawable.ic_note_stack_24px),
                        title = stringResource(R.string.notes_journal_empty_title),
                        description = stringResource(R.string.notes_journal_empty_subtitle),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.entries.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.notes_journal_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(dimensionResource(R.dimen.spacing_large))
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(dimensionResource(R.dimen.spacing_medium)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_normal))
                    ) {
                        items(uiState.entries, key = { it.mediaId }) { entry ->
                            NoteJournalCard(
                                entry = entry,
                                titleLanguage = uiState.titleLanguage,
                                onClick = { onMediaClick(entry.mediaId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteJournalCard(
    entry: LibraryEntry,
    titleLanguage: TitleLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = entry.getTitle(titleLanguage)
    val snippet = entry.notes.orEmpty().replace("\n", " ").trim()
    val edited = entry.updatedAt?.takeIf { it > 0L }?.let { formatProfileRelativeTime(it) }
    val coverShape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                clipShape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .aspectRatio(0.7f)
                    .clip(coverShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(entry.cover.url() ?: entry.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = entry.status.toLabel(entry.type),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (edited != null) {
                        Text(
                            text = edited,
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
