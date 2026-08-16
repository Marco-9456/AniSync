package com.anisync.android.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.util.launchUrl

private const val GOOGLE_AI_STUDIO_URL = "https://aistudio.google.com/app/apikey"

private val PRESET_GEMINI_MODELS = listOf(
    "gemini-2.5-flash" to "Gemini 2.5 Flash (Default & Recommended)",
    "gemini-2.5-flash-lite" to "Gemini 2.5 Flash Lite",
    "gemini-3-flash-preview" to "Gemini 3 Flash Preview",
    "gemini-3.1-flash-lite" to "Gemini 3.1 Flash Lite",
    "gemini-3.5-flash" to "Gemini 3.5 Flash",
    "gemini-3.5-flash-lite" to "Gemini 3.5 Flash Lite",
    "gemini-3.6-flash" to "Gemini 3.6 Flash",
    "gemini-3.7-flash" to "Gemini 3.7 Flash",
    "gemma-4-31b-it" to "Gemma 4 31B IT",
    "gemma-4-26b-a4b-it" to "Gemma 4 26B A4B IT"
)

@Composable
fun SettingsAiScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appSettings = LocalAppSettings.current
    val context = LocalContext.current

    val apiKey by appSettings.geminiApiKey.collectAsStateWithLifecycle()
    val currentModel by appSettings.geminiModel.collectAsStateWithLifecycle()
    val buttonEnabled by appSettings.aiChatButtonEnabled.collectAsStateWithLifecycle()
    val defaultWebSearch by appSettings.aiWebSearchEnabled.collectAsStateWithLifecycle()
    val defaultUserData by appSettings.aiUserDataEnabled.collectAsStateWithLifecycle()
    val defaultAllowSpoilers by appSettings.aiAllowSpoilersEnabled.collectAsStateWithLifecycle()

    var inputKey by remember(apiKey) { mutableStateOf(apiKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    val isCustomModel = remember(currentModel) {
        PRESET_GEMINI_MODELS.none { it.first == currentModel }
    }
    var customModelIdInput by remember(currentModel) {
        mutableStateOf(if (isCustomModel) currentModel else "")
    }

    SettingsScreenScaffold(
        title = "AI Assistant (Gemini)",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        // API Key Section
        SettingsSectionLabel("Google Gemini API")
        SettingsGroup {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Google Gemini API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "A free API key from Google AI Studio is required for AI chat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = {
                            inputKey = it
                            isSaved = false
                        },
                        label = { Text("API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = if (keyVisible) "Hide key" else "Show key"
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        TextButton(
                            onClick = { context.launchUrl(GOOGLE_AI_STUDIO_URL) }
                        ) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get Free API Key")
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                appSettings.setGeminiApiKey(inputKey)
                                isSaved = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isSaved) "Saved ✓" else "Save Key")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Model Selection
        SettingsSectionLabel("AI Model")
        SettingsGroup {
            PRESET_GEMINI_MODELS.forEach { (modelId, modelLabel) ->
                RadioSettingsItem(
                    title = modelId,
                    subtitle = modelLabel,
                    selected = currentModel == modelId,
                    onClick = { appSettings.setGeminiModel(modelId) }
                )
            }

            RadioSettingsItem(
                title = "Custom Model ID",
                subtitle = if (isCustomModel) "Active: $currentModel" else "Enter a custom model identifier",
                selected = isCustomModel,
                onClick = {
                    if (customModelIdInput.isNotBlank()) {
                        appSettings.setGeminiModel(customModelIdInput.trim())
                    }
                }
            )

            AnimatedVisibility(visible = isCustomModel || customModelIdInput.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customModelIdInput,
                            onValueChange = { customModelIdInput = it },
                            placeholder = { Text("e.g. gemini-2.0-flash") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customModelIdInput.isNotBlank()) {
                                    appSettings.setGeminiModel(customModelIdInput.trim())
                                }
                            },
                            enabled = customModelIdInput.isNotBlank()
                        ) {
                            Text("Set")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Default Preferences
        SettingsSectionLabel("Default Chat Preferences")
        SettingsGroup {
            SwitchSettingsItem(
                title = "Web Search Grounding",
                subtitle = "Enable real-time Google search grounding by default",
                checked = defaultWebSearch,
                onCheckedChange = { appSettings.setAiWebSearchEnabled(it) }
            )

            SwitchSettingsItem(
                title = "Include User Data",
                subtitle = "Allow AI to access your personal watch history, scores, notes, progress, and dates for personalized answers",
                checked = defaultUserData,
                onCheckedChange = { appSettings.setAiUserDataEnabled(it) }
            )

            SwitchSettingsItem(
                title = "Allow Spoilers",
                subtitle = "Enable open discussions of endings and plot twists by default",
                checked = defaultAllowSpoilers,
                onCheckedChange = { appSettings.setAiAllowSpoilersEnabled(it) }
            )
        }
    }
}
