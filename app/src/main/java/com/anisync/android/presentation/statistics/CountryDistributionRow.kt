package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.CountryStat
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.ui.theme.LocalExpressiveTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountryDistributionRow(countries: List<CountryStat>) {
    if (countries.isEmpty()) return
    val expressive = LocalExpressiveTypography.current
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_country_distribution),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                countries.forEach { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = c.countryCode,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = c.count.toString(),
                            style = expressive.numericMono,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "Country — typical (5 chips)", widthDp = 360)
@Composable
private fun CountryTypicalPreview() {
    StatPreviewSurface(isDark = false) {
        CountryDistributionRow(countries = previewCountries)
    }
}

@Preview(showBackground = true, name = "Country — many (FlowRow wrap)", widthDp = 360)
@Composable
private fun CountryManyPreview() {
    StatPreviewSurface(isDark = false) {
        CountryDistributionRow(countries = previewCountriesMany)
    }
}

@Preview(showBackground = true, name = "Country — empty (early-return)", widthDp = 360, heightDp = 80)
@Composable
private fun CountryEmptyPreview() {
    StatPreviewSurface(isDark = false) {
        Column(Modifier.padding(16.dp)) {
            CountryDistributionRow(countries = emptyList())
            Text(
                text = "(intentionally renders nothing — early return on empty list)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "Country — dark", widthDp = 360)
@Composable
private fun CountryDarkPreview() {
    StatPreviewSurface(isDark = true) {
        CountryDistributionRow(countries = previewCountries)
    }
}

// endregion
