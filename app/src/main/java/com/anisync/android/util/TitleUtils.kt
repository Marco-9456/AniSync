package com.anisync.android.util

import com.anisync.android.data.TitleLanguage
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.data.local.entity.MediaDetailsEntity
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.domain.CharacterMediaAppearance
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.VoiceActor
import com.anisync.android.domain.VoicedCharacter

object TitleUtils {

    fun getTitle(
        language: TitleLanguage,
        romaji: String?,
        english: String?,
        native: String?,
        userPreferred: String
    ): String {
        return when (language) {
            TitleLanguage.ROMAJI -> romaji ?: english ?: native ?: userPreferred
            TitleLanguage.ENGLISH -> english ?: romaji ?: native ?: userPreferred
            TitleLanguage.NATIVE -> native ?: romaji ?: english ?: userPreferred
        }
    }
}

fun MediaDetails.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun LibraryEntry.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun RelatedMedia.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun LibraryEntryEntity.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun MediaDetailsEntity.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun CharacterDetails.getName(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, name, name, nativeName, nameUserPreferred)
}

fun StaffDetails.getName(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, name, name, nativeName, nameUserPreferred)
}

fun VoiceActor.getName(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, nameFull, nameFull, nameNative, nameUserPreferred)
}

fun VoicedCharacter.getName(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, characterName, characterName, characterNameNative, characterNameUserPreferred)
}

fun CharacterMedia.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun CharacterMediaAppearance.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, mediaTitleRomaji, mediaTitleEnglish, mediaTitleNative, mediaTitle)
}

fun com.anisync.android.domain.StaffProductionMedia.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}
