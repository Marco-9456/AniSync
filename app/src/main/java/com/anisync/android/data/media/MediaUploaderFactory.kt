package com.anisync.android.data.media

import com.anisync.android.data.AppSettings
import com.anisync.android.domain.media.MediaHost
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Resolves the active [MediaUploader] from the user's current [AppSettings] choice.
 * Fresh instance per call so per-host mutable knobs (Litterbox `time`, Custom config)
 * cannot leak across uploads.
 */
@Singleton
class MediaUploaderFactory @Inject constructor(
    private val settings: AppSettings,
    private val catbox: Provider<CatboxUploader>,
    private val litterbox: Provider<LitterboxUploader>,
    private val custom: Provider<CustomMultipartUploader>
) {
    fun current(): MediaUploader {
        return when (settings.mediaHost.value) {
            MediaHost.CATBOX -> catbox.get().apply {
                userhash = settings.catboxUserHash.value.trim()
            }
            MediaHost.LITTERBOX -> litterbox.get().apply {
                time = settings.litterboxDuration.value.ifBlank { "1h" }
            }
            MediaHost.CUSTOM -> custom.get().apply {
                config = CustomMultipartUploader.Config(
                    url = settings.customHostUrl.value.orEmpty(),
                    fileFieldName = settings.customHostFileField.value.ifBlank { "fileToUpload" },
                    authHeader = settings.customHostAuthHeader.value.takeIf { it.isNotBlank() },
                    responseJsonPath = settings.customHostResponseJsonPath.value.takeIf { it.isNotBlank() }
                )
            }
        }
    }
}
