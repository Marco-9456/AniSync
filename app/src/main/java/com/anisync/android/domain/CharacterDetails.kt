package com.anisync.android.domain

import com.anisync.android.type.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class CharacterDetails(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val imageUrl: String?,
    val description: String?,
    val gender: String?,
    val age: String?,
    val bloodType: String?,
    val dateOfBirth: String?,
    val favourites: Int?,
    val media: List<CharacterMedia>
)

@Serializable
data class CharacterMedia(
    val id: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val type: MediaType?,
    val voiceActor: VoiceActor?
)

@Serializable
data class VoiceActor(
    val id: Int,
    val nameFull: String,
    val nameNative: String?,
    val nameUserPreferred: String,
    val imageUrl: String?
)
