package com.anisync.android.presentation.settings.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.ui.theme.PreviewTheme
import com.materialkolor.PaletteStyle

/**
 * A phone mockup frame containing a mini app preview.
 * 
 * Displays a realistic phone outline with the theme preview content inside,
 * including a dynamic island/notch at the top.
 */
@Composable
fun PhonePreview(
    seedColor: Color?,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Phone frame
        Box(
            modifier = Modifier
                .width(180.dp)
                .aspectRatio(0.48f)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black)
                .padding(4.dp)
        ) {
            // Screen content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                if (seedColor != null) {
                    // Use PreviewTheme with seed color
                    PreviewTheme(
                        seedColor = seedColor,
                        isDark = isDarkMode,
                        style = paletteStyle
                    ) {
                        PhoneScreenContent()
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Use dynamic color scheme for "Dynamic" option
                    val dynamicColorScheme = if (isDarkMode) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                    PreviewTheme(colorScheme = dynamicColorScheme) {
                        PhoneScreenContent()
                    }
                } else {
                    // Fallback for pre-Android 12 devices - use default purple
                    PreviewTheme(
                        seedColor = Color(0xFF6750A4),
                        isDark = isDarkMode,
                        style = paletteStyle
                    ) {
                        PhoneScreenContent()
                    }
                }
            }
        }
    }
}

/**
 * The content rendered inside the phone screen.
 * Matches the screenshot: notch, two tabs, filter chips, 2x2 grid, bottom nav.
 */
@Composable
private fun PhoneScreenContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Status bar with dynamic island/notch
        StatusBarWithNotch()
        
        // Two pill tabs
        TabRow()
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Two filter chips
        FilterChipsRow()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 2x2 card grid
        CardGrid(modifier = Modifier.weight(1f))
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Bottom navigation
        BottomNavBar()
    }
}

@Composable
private fun StatusBarWithNotch() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // "Search bar"
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    }
}

@Composable
private fun TabRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active tab
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        // Inactive tab
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
private fun FilterChipsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Two filter chips
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    }
}

@Composable
private fun CardGrid(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // First row
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MediaCard(modifier = Modifier.weight(1f))
            MediaCard(modifier = Modifier.weight(1f))
        }
        // Second row
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MediaCard(modifier = Modifier.weight(1f))
            MediaCard(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MediaCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image placeholder area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Content area with progress bar placeholders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Two text line placeholders
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active nav item (filled circle)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            // Inactive nav items (outline circles)
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun PhonePreviewDark() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        PhonePreview(
            seedColor = Color(0xFF6750A4),
            isDarkMode = true,
            paletteStyle = PaletteStyle.TonalSpot,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhonePreviewLight() {
    MaterialTheme {
        PhonePreview(
            seedColor = Color(0xFFE91E63),
            isDarkMode = false,
            paletteStyle = PaletteStyle.TonalSpot,
            modifier = Modifier.padding(16.dp)
        )
    }
}
