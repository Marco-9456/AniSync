package com.anisync.android.domain

import kotlinx.serialization.Serializable

@Serializable
data class StaffDetails(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val imageUrl: String?,
    val description: String?,
    val gender: String?,
    val age: Int?,
    val bloodType: String?,
    val dateOfBirth: String?,
    val dateOfDeath: String?,
    val favourites: Int?,
    val language: String?,
    val primaryOccupations: List<String>,
    val yearsActive: List<Int>,
    val homeTown: String?,
    val voicedCharacters: List<VoicedCharacter>
)

@Serializable
data class VoicedCharacter(
    val characterId: Int,
    val characterName: String,
    val characterImageUrl: String?,
    val mediaAppearances: List<CharacterMediaAppearance>
)

@Serializable
data class CharacterMediaAppearance(
    val mediaId: Int,
    val mediaTitle: String,
    val coverUrl: String?,
    val startYear: Int?,
    val characterRole: String?,
    val popularity: Int?,
    val averageScore: Int?,
    val favourites: Int?,
    val isOnList: Boolean
)
