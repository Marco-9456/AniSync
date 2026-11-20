package com.anisync.android.presentation.discover

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anisync.android.type.MediaType

/** Discover search as a page of its own, open from the start on the preset its route carries. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSearchScreen(
    mediaType: MediaType,
    navController: NavHostController,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: DiscoverSearchViewModel = hiltViewModel()
) {
    DiscoverSearch(
        viewModel = viewModel,
        mediaType = mediaType,
        searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Expanded),
        textFieldState = rememberTextFieldState(),
        searchAnchor = null,
        navController = navController,
        onMediaClick = onMediaClick,
        onCharacterClick = onCharacterClick,
        onStaffClick = onStaffClick,
        onStudioClick = onStudioClick,
        onUserClick = onUserClick,
        onBack = onBackClick
    )
}
