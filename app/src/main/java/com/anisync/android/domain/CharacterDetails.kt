package com.anisync.android.domain

import com.anisync.android.type.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class CharacterDetails(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String> = emptyList(),
    val imageUrl: String?,
    val description: String?,
    val gender: String?,
    val age: String?,
    val bloodType: String?,
    val dateOfBirth: String?,
    val favourites: Int?,
    val isFavourite: Boolean = false,
    val media: List<CharacterMedia>,
    val hasNextPage: Boolean = false
)

@Serializable
data class CharacterMedia(
    val id: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val bannerUrl: String?,
    val type: MediaType?,
    val characterRole: String?,
    val startYear: Int?,
    val popularity: Int?,
    val averageScore: Int?,
    val favourites: Int?,
    val isOnList: Boolean,
    val voiceActors: List<VoiceActor>
)

@Serializable
data class VoiceActor(
    val id: Int,
    val nameFull: String,
    val nameNative: String?,
    val nameUserPreferred: String,
    val imageUrl: String?,
    val language: String?
)