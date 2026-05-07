package com.anisync.android.presentation.settings

import androidx.lifecycle.ViewModel
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.media.MediaHost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MediaUploadSettingsViewModel @Inject constructor(
    private val settings: AppSettings
) : ViewModel() {

    val mediaHost: StateFlow<MediaHost> = settings.mediaHost
    val imgurClientId: StateFlow<String> = settings.imgurClientId
    val customHostUrl: StateFlow<String> = settings.customHostUrl
    val customHostFileField: StateFlow<String> = settings.customHostFileField
    val customHostAuthHeader: StateFlow<String> = settings.customHostAuthHeader
    val customHostResponseJsonPath: StateFlow<String> = settings.customHostResponseJsonPath

    fun setMediaHost(host: MediaHost) = settings.setMediaHost(host)
    fun setImgurClientId(value: String) = settings.setImgurClientId(value)
    fun setCustomHostUrl(value: String) = settings.setCustomHostUrl(value)
    fun setCustomHostFileField(value: String) = settings.setCustomHostFileField(value)
    fun setCustomHostAuthHeader(value: String) = settings.setCustomHostAuthHeader(value)
    fun setCustomHostResponseJsonPath(value: String) = settings.setCustomHostResponseJsonPath(value)
}
