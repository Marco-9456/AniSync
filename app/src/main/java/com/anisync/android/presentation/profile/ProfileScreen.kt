package com.anisync.android.presentation.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.UserProfile
import com.anisync.android.ui.theme.*

@Composable
fun ProfileScreen(
    onMediaClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit = {}, // Added distinct logout callback
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OliveDrab)
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.message}", color = Color.Red)
                }
            }
            is ProfileUiState.Success -> {
                ProfileContent(
                    profile = state.profile,
                    onSettingsClick = onSettingsClick,
                    onLogoutClick = onLogoutClick
                )
            }
        }
    }
}

@Composable
fun ProfileContent(
    profile: UserProfile,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Header Banner
        AsyncImage(
            model = profile.bannerUrl,
            contentDescription = "Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(BeigeYellow) // Fallback color
        )

        // 2. Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(140.dp)) // Push avatar down to overlap

            // Content Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Avatar
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = profile.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp) // Slightly smaller match screenshot
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .border(4.dp, Color.White, RoundedCornerShape(24.dp))
                        .background(Color.Gray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name & ID
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    fontSize = 28.sp
                )
                Text(
                    text = "ID: #${profile.id}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Anime Card
                    StatCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFDE047), // Yellow
                        icon = Icons.Default.Tv,
                        label = "ANIME",
                        value = String.format("%.1f", profile.daysWatched),
                        subLabel = "Days Watched"
                    )

                    // Manga Card
                    StatCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFB3E5FC), // Light Blue
                        icon = Icons.Default.Book,
                        label = "MANGA",
                        value = profile.chaptersRead.toString(),
                        subLabel = "Chapters Read"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Settings Group
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfacePinkWhite)
                ) {
                    // Notifications
                var notificationsEnabled by remember { mutableStateOf(true) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = OliveDrab,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, CircleShape)
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OliveDrab,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                }

                HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))

                // Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSettingsClick() }
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = OliveDrab, // Brown/Olive
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, CircleShape) // Using White bg for icon circle
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    // No chevron in screenshot, but usually implies navigation. 
                    // Screenshot just shows the item. Use generic click.
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Log Out Button
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = PastelPink),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = "Log Out",
                    color = BehindRed, // Darker red/brown for text
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subLabel: String
) {
    Card(
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }
            
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }
        }
    }
}
