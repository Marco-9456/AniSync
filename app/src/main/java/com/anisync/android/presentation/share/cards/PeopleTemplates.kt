package com.anisync.android.presentation.share.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.data.CardSize
import com.anisync.android.domain.formatExactCount
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalExpressiveTypography
import java.text.DateFormat
import java.util.Date

object CharacterTemplate : ShareTemplate<ShareSubject.Character> {
    override val label: Int = R.string.share_template_portrait
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Character, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl) { CharacterCard(subject, spec) }
    }
}

object StaffTemplate : ShareTemplate<ShareSubject.Staff> {
    override val label: Int = R.string.share_template_portrait
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Staff, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl) { StaffCard(subject, spec) }
    }
}

/** Where the portrait sits and how the name beside or under it is set, per size. */
private data class PersonLayout(
    val picture: Pair<Dp, Dp>,
    val pictureTop: Dp,
    val centred: Boolean,
    val nameSize: Int,
    val nameLines: Int,
    val textTop: Dp,
    val facts: Boolean,
    val sectionTop: Dp,
    val bottom: Dp,
)

/** The layout with its picture shortened by [reserve], its width kept in proportion. */
private fun PersonLayout.shrunkBy(reserve: Dp): PersonLayout {
    if (reserve == 0.dp) return this
    val height = picture.second - reserve
    return copy(picture = picture.first * (height / picture.second) to height)
}

private fun characterLayout(size: CardSize) = when (size) {
    CardSize.CHAT -> PersonLayout(88.dp to 126.dp, 20.dp, false, 22, 1, 2.dp, false, 16.dp, 20.dp)
    CardSize.SQUARE -> PersonLayout(112.dp to 160.dp, 20.dp, false, 24, 1, 4.dp, false, 16.dp, 20.dp)
    CardSize.PORTRAIT -> PersonLayout(150.dp to 214.dp, 20.dp, false, 28, 2, 8.dp, true, 16.dp, 14.dp)
    CardSize.STORY -> PersonLayout(180.dp to 257.dp, 28.dp, true, 30, 1, 16.dp, true, 12.dp, 16.dp)
}

private fun staffLayout(size: CardSize) = when (size) {
    CardSize.CHAT -> PersonLayout(88.dp to 88.dp, 20.dp, false, 22, 1, 2.dp, false, 16.dp, 20.dp)
    CardSize.SQUARE -> PersonLayout(112.dp to 112.dp, 20.dp, false, 24, 1, 6.dp, false, 10.dp, 16.dp)
    CardSize.PORTRAIT -> PersonLayout(149.dp to 149.dp, 24.dp, false, 28, 2, 12.dp, true, 12.dp, 14.dp)
    CardSize.STORY -> PersonLayout(160.dp to 160.dp, 28.dp, true, 30, 1, 16.dp, true, 20.dp, 16.dp)
}

@Composable
private fun CharacterCard(subject: ShareSubject.Character, spec: CardSpec) {
    val layout = characterLayout(spec.size).shrunkBy(captionReserve(spec))
    val fixed = spec.size != CardSize.CHAT
    val shape = RoundedCornerShape(
        topStart = minOf(75.dp, layout.picture.first / 2),
        topEnd = minOf(75.dp, layout.picture.first / 2),
        bottomEnd = 24.dp,
        bottomStart = 24.dp,
    )
    Column(Modifier.fillMaxWidth().then(if (fixed) Modifier.fillMaxSize() else Modifier).padding(horizontal = 20.dp)) {
        PersonHeader(
            layout = layout,
            picture = { modifier -> CardPicture(subject.imageUrl, shape, modifier) },
            eyebrow = stringResource(R.string.share_person_character),
            name = subject.name,
            nativeName = subject.nativeName,
            favourites = subject.favourites?.takeIf { spec.shows(CardDetail.FAVOURITES) },
        )
        if (layout.facts) {
            val facts = buildList {
                if (spec.shows(CardDetail.BIRTHDAY)) subject.birthday?.let { add(it to R.string.share_detail_birthday) }
                if (spec.shows(CardDetail.GENDER)) subject.gender?.let { add(it to R.string.share_detail_gender) }
                if (spec.shows(CardDetail.AGE)) subject.age?.let { add(it to R.string.share_detail_age) }
            }
            if (facts.isNotEmpty()) {
                Spacer(Modifier.height(layout.sectionTop))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    facts.forEach { (value, label) -> PersonFact(value, stringResource(label)) }
                }
            }
        }
        val knownFor = subject.knownFor
        if (knownFor != null && spec.shows(CardDetail.KNOWN_FOR)) {
            Spacer(Modifier.height(layout.sectionTop))
            KnownForTile(knownFor)
        }
        CardCaption(spec, cardText(15, 20, FontWeight.SemiBold), MaterialTheme.colorScheme.onSurface, Modifier.padding(top = 8.dp))
        if (fixed) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(16.dp))
        CardBrand(subject.handle, spec, MaterialTheme.colorScheme.surfaceContainerHigh)
        Spacer(Modifier.height(layout.bottom))
    }
}

@Composable
private fun StaffCard(subject: ShareSubject.Staff, spec: CardSpec) {
    val layout = staffLayout(spec.size)
    val fixed = spec.size != CardSize.CHAT
    Column(Modifier.fillMaxWidth().then(if (fixed) Modifier.fillMaxSize() else Modifier).padding(horizontal = 20.dp)) {
        PersonHeader(
            layout = layout,
            picture = { modifier ->
                UserAvatar(contentDescription = null, size = layout.picture.first, url = subject.imageUrl, showFrame = false, modifier = modifier)
            },
            eyebrow = subject.occupation,
            name = subject.name,
            nativeName = subject.nativeName,
            favourites = subject.favourites?.takeIf { spec.shows(CardDetail.FAVOURITES) },
        )
        if (spec.shows(CardDetail.ROLES) && subject.roles.isNotEmpty()) {
            Spacer(Modifier.height(layout.sectionTop))
            Text(
                text = stringResource(if (subject.voicesRoles) R.string.share_staff_voice_of else R.string.share_known_for).cardUpper(),
                style = cardEyebrow(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                subject.roles.take(3).forEach { role -> RoleTile(role, imageSize = 84.dp - captionReserve(spec)) }
            }
        }
        if (layout.facts) {
            val chips = buildList {
                if (spec.shows(CardDetail.ACTIVE_SINCE)) {
                    subject.activeSince?.let { add(stringResource(R.string.share_staff_active_since, it.toString())) }
                }
                if (spec.shows(CardDetail.HOME_TOWN)) subject.homeTown?.let { add(it) }
            }
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(if (spec.size == CardSize.STORY) 16.dp else 2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { chip ->
                        Text(
                            text = chip,
                            style = cardText(13, 18, FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
        CardCaption(spec, cardText(15, 20, FontWeight.SemiBold), MaterialTheme.colorScheme.onSurface, Modifier.padding(top = 8.dp))
        if (fixed) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(16.dp))
        CardBrand(subject.handle, spec, MaterialTheme.colorScheme.surfaceContainerHigh)
        Spacer(Modifier.height(layout.bottom))
    }
}

/** The portrait with the eyebrow, name, native name and favourites beside it, or under it on Story. */
@Composable
private fun PersonHeader(
    layout: PersonLayout,
    picture: @Composable (Modifier) -> Unit,
    eyebrow: String?,
    name: String,
    nativeName: String?,
    favourites: Int?,
) {
    val text: @Composable (Modifier) -> Unit = { modifier ->
        PersonText(layout, eyebrow, name, nativeName, favourites, modifier)
    }
    if (layout.centred) {
        Column(Modifier.fillMaxWidth().padding(top = layout.pictureTop)) {
            picture(Modifier.align(Alignment.CenterHorizontally).size(layout.picture.first, layout.picture.second))
            text(Modifier.padding(start = 4.dp, top = layout.textTop))
        }
    } else {
        Row(Modifier.padding(top = layout.pictureTop)) {
            picture(Modifier.size(layout.picture.first, layout.picture.second))
            Spacer(Modifier.width(14.dp))
            text(Modifier.weight(1f).padding(top = layout.textTop))
        }
    }
}

@Composable
private fun PersonText(
    layout: PersonLayout,
    eyebrow: String?,
    name: String,
    nativeName: String?,
    favourites: Int?,
    modifier: Modifier,
) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier) {
        if (eyebrow != null) {
            Text(eyebrow.cardUpper(), style = cardEyebrow(), color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Spacer(Modifier.height(2.dp))
        }
        Text(
            text = name,
            style = titleWide.style(layout.nameSize.sp, 32.sp, (-0.5).sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = layout.nameLines,
            overflow = TextOverflow.Ellipsis,
        )
        if (!nativeName.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = nativeName,
                style = cardText(14, 17, FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (favourites != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(start = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = pluralStringResourceOrDefault(R.plurals.share_favourites_count, favourites, formatExactCount(favourites, locale)),
                    style = cardText(13, 18, FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PersonFact(value: String, label: String) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, style = cardText(14, 20, FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(
            text = label.cardUpper(),
            style = cardEyebrow(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun KnownForTile(knownFor: KnownFor) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardPicture(knownFor.coverUrl, RoundedCornerShape(10.dp), Modifier.size(38.dp, 54.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    when (knownFor.role) {
                        KnownForRole.MAIN -> R.string.share_known_for_main
                        KnownForRole.SUPPORTING -> R.string.share_known_for_supporting
                        KnownForRole.BACKGROUND -> R.string.share_known_for_title
                    },
                    knownFor.title,
                ),
                style = cardText(14, 20, FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (knownFor.more > 0) {
                Text(
                    text = pluralStringResourceOrDefault(R.plurals.share_known_for_more, knownFor.more, formatExactCount(knownFor.more, locale)),
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun RoleTile(role: CardImage, imageSize: Dp) {
    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CardPicture(role.url, RoundedCornerShape(16.dp), Modifier.size(imageSize))
        Spacer(Modifier.height(6.dp))
        Text(
            text = role.title,
            style = cardText(12, 16, FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

object ReviewQuoteTemplate : ShareTemplate<ShareSubject.Review> {
    override val label: Int = R.string.share_template_quote
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Review, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl) { ReviewQuote(subject, spec) }
    }
}

private data class QuoteLayout(val banner: Dp, val quoteSize: Int, val quoteLines: Int, val bottom: Dp)

private fun quoteLayout(size: CardSize) = when (size) {
    CardSize.CHAT -> QuoteLayout(88.dp, 18, 3, 20.dp)
    CardSize.SQUARE -> QuoteLayout(108.dp, 21, 3, 12.dp)
    CardSize.PORTRAIT -> QuoteLayout(150.dp, 22, 3, 24.dp)
    CardSize.STORY -> QuoteLayout(240.dp, 26, 4, 32.dp)
}

@Composable
private fun ReviewQuote(subject: ShareSubject.Review, spec: CardSpec) {
    val layout = quoteLayout(spec.size)
    val banner = layout.banner - captionReserve(spec)
    val heroWide = LocalExpressiveTypography.current.heroWide
    val locale = LocalConfiguration.current.locales[0]
    val fixed = spec.size != CardSize.CHAT
    Box(Modifier.fillMaxWidth().then(if (fixed) Modifier.fillMaxSize() else Modifier)) {
        CardPicture(
            url = subject.bannerUrl ?: subject.coverUrl,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth().height(banner + 2.dp).fadeToBottom(),
        )
        if (spec.shows(CardDetail.SCORE)) {
            ReviewScore(subject.score, Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 22.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (fixed) Modifier.fillMaxSize() else Modifier)
                .padding(start = 24.dp, end = 24.dp, top = banner - 12.dp)
        ) {
            Text(
                text = stringResource(R.string.share_review_eyebrow, subject.mediaTitle).cardUpper(),
                style = cardEyebrow(),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.share_review_quote_mark),
                style = heroWide.style(64.sp, 52.sp),
                color = MaterialTheme.colorScheme.primaryFixedDim,
                modifier = Modifier.padding(top = 4.dp).height(32.dp),
            )
            Text(
                text = subject.summary,
                style = cardText(layout.quoteSize, 28, FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = layout.quoteLines,
                overflow = TextOverflow.Ellipsis,
            )
            CardCaption(spec, cardText(15, 20, FontWeight.SemiBold), MaterialTheme.colorScheme.onSurface, Modifier.padding(top = 8.dp))
            if (spec.shows(CardDetail.AUTHOR)) {
                Spacer(Modifier.height(26.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UserAvatar(contentDescription = null, size = 32.dp, url = subject.authorAvatarUrl, showFrame = false)
                    Column {
                        Text(subject.authorName, style = cardText(14, 20, FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        Text(
                            text = pluralStringResourceOrDefault(
                                R.plurals.share_review_helpful,
                                subject.helpful,
                                reviewDate(subject.createdAt),
                                formatExactCount(subject.helpful, locale),
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (fixed) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(16.dp))
            CardBrand(subject.handle, spec, MaterialTheme.colorScheme.surfaceContainerHigh)
            Spacer(Modifier.height(layout.bottom))
        }
    }
}

/** The review score on its tier's container, green, amber or red-orange as every review surface reads it. */
@Composable
private fun ReviewScore(score: Int, modifier: Modifier = Modifier) {
    val tier = reviewTierColor(score, cardIsDark())
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tier.container)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = tier.content,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.share_review_score, score),
            style = cardText(13, 18, FontWeight.ExtraBold),
            color = tier.content,
        )
    }
}

@Composable
private fun reviewDate(createdAt: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(createdAt * 1000))
}
