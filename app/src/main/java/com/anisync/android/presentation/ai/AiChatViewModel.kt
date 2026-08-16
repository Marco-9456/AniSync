package com.anisync.android.presentation.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anisync.android.data.AppSettings
import com.anisync.android.data.ai.GeminiApiService
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.ai.AiMediaFocusContext
import com.anisync.android.domain.ai.AiUserDataEntry
import com.anisync.android.domain.ai.ChatMessage
import com.anisync.android.presentation.navigation.AiChat
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

import com.anisync.android.domain.ai.AiChatSession

data class AiChatUiState(
    val currentSessionId: String = java.util.UUID.randomUUID().toString(),
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val webSearchEnabled: Boolean = true,
    val userDataEnabled: Boolean = true,
    val allowSpoilersEnabled: Boolean = false,
    val hasApiKey: Boolean = false,
    val focusedMedia: AiMediaFocusContext? = null
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val geminiApiService: GeminiApiService,
    private val appSettings: AppSettings,
    private val libraryRepository: LibraryRepository,
    private val detailsRepository: DetailsRepository
) : ViewModel() {

    val sessions: StateFlow<List<AiChatSession>> = appSettings.aiChatSessions

    private val navRoute = runCatching { savedStateHandle.toRoute<AiChat>() }.getOrNull()
    private val focusedMediaId = navRoute?.mediaId

    private val _uiState = MutableStateFlow(
        AiChatUiState(
            webSearchEnabled = appSettings.aiWebSearchEnabled.value,
            userDataEnabled = appSettings.aiUserDataEnabled.value,
            allowSpoilersEnabled = appSettings.aiAllowSpoilersEnabled.value,
            hasApiKey = appSettings.geminiApiKey.value.isNotBlank()
        )
    )
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.geminiApiKey.collect { key ->
                _uiState.update { it.copy(hasApiKey = key.isNotBlank()) }
            }
        }

        if (focusedMediaId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                loadFocusedMediaDetails(focusedMediaId)
            }
        }
    }

    private suspend fun loadFocusedMediaDetails(mediaId: Int) {
        try {
            val details = detailsRepository.observeMediaDetails(mediaId).first()
            if (details != null) {
                val focusContext = AiMediaFocusContext(
                    mediaId = details.id,
                    title = details.titleUserPreferred,
                    description = details.description,
                    genres = details.genres,
                    format = details.format,
                    status = details.status,
                    averageScore = details.score,
                    episodes = details.episodes,
                    studio = details.studios.firstOrNull()?.name ?: details.studio?.name
                )
                _uiState.update { it.copy(focusedMedia = focusContext) }
            }
        } catch (_: Exception) {}
    }

    fun toggleWebSearch(enabled: Boolean) {
        appSettings.setAiWebSearchEnabled(enabled)
        _uiState.update { it.copy(webSearchEnabled = enabled) }
    }

    fun toggleUserData(enabled: Boolean) {
        appSettings.setAiUserDataEnabled(enabled)
        _uiState.update { it.copy(userDataEnabled = enabled) }
    }

    fun toggleAllowSpoilers(enabled: Boolean) {
        appSettings.setAiAllowSpoilersEnabled(enabled)
        _uiState.update { it.copy(allowSpoilersEnabled = enabled) }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), isLoading = false) }
    }

    fun startNewChat() {
        _uiState.update {
            it.copy(
                currentSessionId = java.util.UUID.randomUUID().toString(),
                messages = emptyList(),
                isLoading = false
            )
        }
    }

    fun loadSession(session: AiChatSession) {
        _uiState.update {
            it.copy(
                currentSessionId = session.id,
                messages = session.messages,
                isLoading = false
            )
        }
    }

    fun deleteSession(sessionId: String) {
        appSettings.deleteAiChatSession(sessionId)
        if (_uiState.value.currentSessionId == sessionId) {
            startNewChat()
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(text = trimmed, isUser = true)
        val currentMessages = _uiState.value.messages + userMessage

        _uiState.update {
            it.copy(
                messages = currentMessages,
                isLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val apiKey = appSettings.geminiApiKey.value
                val model = appSettings.geminiModel.value

                val userData = if (_uiState.value.userDataEnabled) {
                    getUserDataEntries()
                } else {
                    emptyList()
                }

                val result = geminiApiService.generateChatResponse(
                    apiKey = apiKey,
                    modelName = model,
                    conversationHistory = currentMessages.dropLast(1),
                    latestUserMessage = trimmed,
                    useWebSearch = _uiState.value.webSearchEnabled,
                    allowSpoilers = _uiState.value.allowSpoilersEnabled,
                    userData = userData,
                    mediaFocus = _uiState.value.focusedMedia
                )

                val aiMessage = ChatMessage(
                    text = result.text,
                    isUser = false,
                    sources = result.sources
                )

                val updatedList = currentMessages + aiMessage
                _uiState.update {
                    it.copy(
                        messages = updatedList,
                        isLoading = false
                    )
                }

                // Persist session in history
                val firstUserMsg = updatedList.firstOrNull { it.isUser }?.text?.take(40) ?: "New Chat"
                val session = AiChatSession(
                    id = _uiState.value.currentSessionId,
                    title = firstUserMsg,
                    mediaId = _uiState.value.focusedMedia?.mediaId,
                    mediaTitle = _uiState.value.focusedMedia?.title,
                    messages = updatedList
                )
                appSettings.saveAiChatSession(session)
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    text = e.message ?: "Failed to get response from Gemini AI. Please check your API key and connection.",
                    isUser = false,
                    isError = true
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Loads user's anime and manga library entries to provide full personal context.
     */
    private suspend fun getUserDataEntries(): List<AiUserDataEntry> = withContext(Dispatchers.IO) {
        try {
            val anime = libraryRepository.observeLibrary("", MediaType.ANIME).first()
            val manga = libraryRepository.observeLibrary("", MediaType.MANGA).first()

            val allEntries = (anime + manga)

            allEntries.map { entry ->
                AiUserDataEntry(
                    titleUserPreferred = entry.titleUserPreferred,
                    titleRomaji = entry.titleRomaji,
                    titleEnglish = entry.titleEnglish,
                    titleNative = entry.titleNative,
                    mediaType = entry.type?.name ?: "ANIME",
                    status = entry.status.name,
                    progress = entry.progress,
                    totalEpisodesOrChapters = entry.totalEpisodes ?: entry.totalChapters,
                    score = entry.score,
                    notes = entry.notes,
                    startedAt = entry.startedAt,
                    completedAt = entry.completedAt
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
