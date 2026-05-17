package com.anisync.android.domain

import com.anisync.android.type.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class StaffDetails(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val nameUserPreferred: String,
    val alternativeNames: List<String> = emptyList(),
    val imageUrl: String?,
    val description: String?,
    val gender: String?,
    val age: Int?,
    val bloodType: String?,
    val dateOfBirth: String?,
    val dateOfDeath: String?,
    val favourites: Int?,
    val isFavourite: Boolean = false,
    val language: String?,
    val primaryOccupations: List<String>,
    val yearsActive: List<Int>,
    val homeTown: String?,
    val voicedCharacters: List<VoicedCharacter>,
    val hasNextPage: Boolean = false,
    val productionMedia: List<StaffProductionMedia> = emptyList(),
    val productionMediaHasNextPage: Boolean = false
)

@Serializable
data class StaffProductionMedia(
    val mediaId: Int,
    val titleUserPreferred: String,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val coverUrl: String?,
    val cover: CoverImage? = null,
    val type: MediaType?,
    val startYear: Int?,
    val staffRole: String?,
    val popularity: Int?,
    val averageScore: Int?,
    val favourites: Int?,
    val isOnList: Boolean
)

@Serializable
data class VoicedCharacter(
    val characterId: Int,
    val characterName: String,
    val characterNameNative: String?,
    val characterNameUserPreferred: String,
    val characterImageUrl: String?,
    val mediaAppearances: List<CharacterMediaAppearance>
)

@Serializable
data class CharacterMediaAppearance(
    val mediaId: Int,
    val mediaTitle: String,
    val mediaTitleRomaji: String?,
    val mediaTitleEnglish: String?,
    val mediaTitleNative: String?,
    val coverUrl: String?,
    val cover: CoverImage? = null,
    val startYear: Int?,
    val characterRole: String?,
    val popularity: Int?,
    val averageScore: Int?,
    val favourites: Int?,
    val isOnList: Boolean
)