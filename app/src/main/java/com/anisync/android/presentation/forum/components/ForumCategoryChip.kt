package com.anisync.android.presentation.forum.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anisync.android.domain.ForumCategory

@Composable
fun ForumCategoryChip(
    category: ForumCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(category.name, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier
    )
}
