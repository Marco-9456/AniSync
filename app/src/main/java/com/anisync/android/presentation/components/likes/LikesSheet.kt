package com.anisync.android.presentation.components.likes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.UserSummary
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.ui.theme.LocalAvatarShape
import com.anisync.android.ui.theme.emphasis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesSheet(
    target: LikesTarget,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    viewModel: LikesViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(target) { viewModel.load(target) }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LikesSheetContent(
            users = users,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onUserClick = onUserClick
        )
    }
}

@Composable
fun LikesSheetContent(
    users: List<UserSummary>,
    isLoading: Boolean,
    errorMessage: String?,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
    ) {
        Text(
            text = stringResource(R.string.activity_likes_title),
            style = MaterialTheme.typography.titleLarge.emphasis(),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        when {
            isLoading && users.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppCircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMessage != null && users.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            users.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.activity_likes_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(users, key = { it.id }) { user ->
                        LikerRow(user = user, onClick = { onUserClick(user.name) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LikerRow(user: UserSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    val avatarRequest = remember(user.avatarUrl) {
        ImageRequest.Builder(context)
            .data(user.avatarUrl)
            .allowHardware(false)
            .crossfade(true)
            .build()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            contentDescription = null,
            size = 48.dp,
            model = avatarRequest
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = user.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun LikesSheetContentPreview_Loading() {
    MaterialTheme {
        LikesSheetContent(
            users = emptyList(),
            isLoading = true,
            errorMessage = null,
            onUserClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
private fun LikesSheetContentPreview_Empty() {
    MaterialTheme {
        LikesSheetContent(
            users = emptyList(),
            isLoading = false,
            errorMessage = null,
            onUserClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
private fun LikesSheetContentPreview_Error() {
    MaterialTheme {
        LikesSheetContent(
            users = emptyList(),
            isLoading = false,
            errorMessage = "Failed to load likes. Please try again.",
            onUserClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Populated State")
@Composable
private fun LikesSheetContentPreview_Populated() {
    val dummyUsers = listOf(
        UserSummary(id = 1, name = "Spike Spiegel", avatarUrl = ""),
        UserSummary(id = 2, name = "Faye Valentine", avatarUrl = ""),
        UserSummary(id = 3, name = "Jet Black", avatarUrl = ""),
        UserSummary(id = 4, name = "Edward", avatarUrl = "")
    )
    MaterialTheme {
        LikesSheetContent(
            users = dummyUsers,
            isLoading = false,
            errorMessage = null,
            onUserClick = {}
        )
    }
}
