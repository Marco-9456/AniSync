package com.anisync.android.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.ai.GeminiApiService
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.ai.AiUserNoteContext
import com.anisync.android.domain.ai.ChatMessage
import com.anisync.android.type.MediaType
import com.anisync.android.util.getTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val webSearchEnabled: Boolean = true,
    val includeNotesEnabled: Boolean = true,
    val allowSpoilersEnabled: Boolean = false,
    val hasApiKey: Boolean = false,
    val availableNotesCount: Int = 0
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val appSettings: AppSettings,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AiChatUiState(
            webSearchEnabled = appSettings.aiWebSearchEnabled.value,
            includeNotesEnabled = appSettings.aiIncludeNotesEnabled.value,
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

        // Preload notes count
        viewModelScope.launch {
            combine(
                libraryRepository.observeLibrary("", MediaType.ANIME),
                libraryRepository.observeLibrary("", MediaType.MANGA)
            ) { anime, manga ->
                (anime + manga).count { !it.notes.isNullOrBlank() }
            }.collect { count ->
                _uiState.update { it.copy(availableNotesCount = count) }
            }
        }
    }

    fun toggleWebSearch(enabled: Boolean) {
        appSettings.setAiWebSearchEnabled(enabled)
        _uiState.update { it.copy(webSearchEnabled = enabled) }
    }

    fun toggleIncludeNotes(enabled: Boolean) {
        appSettings.setAiIncludeNotesEnabled(enabled)
        _uiState.update { it.copy(includeNotesEnabled = enabled) }
    }

    fun toggleAllowSpoilers(enabled: Boolean) {
        appSettings.setAiAllowSpoilersEnabled(enabled)
        _uiState.update { it.copy(allowSpoilersEnabled = enabled) }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), isLoading = false) }
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

                val userNotes = if (_uiState.value.includeNotesEnabled) {
                    getUserNotesContext()
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
                    userNotes = userNotes
                )

                val aiMessage = ChatMessage(
                    text = result.text,
                    isUser = false,
                    sources = result.sources
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + aiMessage,
                        isLoading = false
                    )
                }
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

    private suspend fun getUserNotesContext(): List<AiUserNoteContext> {
        return try {
            val titleLang = appSettings.titleLanguage.value
            val anime = libraryRepository.observeLibrary("", MediaType.ANIME).first()
            val manga = libraryRepository.observeLibrary("", MediaType.MANGA).first()

            val animeNotes = anime.filter { !it.notes.isNullOrBlank() }.map { entry ->
                AiUserNoteContext(
                    title = entry.getTitle(titleLang),
                    mediaType = "Anime",
                    status = entry.status.name,
                    score = entry.score,
                    progress = entry.progress,
                    note = entry.notes.orEmpty()
                )
            }

            val mangaNotes = manga.filter { !it.notes.isNullOrBlank() }.map { entry ->
                AiUserNoteContext(
                    title = entry.getTitle(titleLang),
                    mediaType = "Manga",
                    status = entry.status.name,
                    score = entry.score,
                    progress = entry.progress,
                    note = entry.notes.orEmpty()
                )
            }

            (animeNotes + mangaNotes)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
