package com.anisync.android.presentation.settings

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

private val GEMINI_MODELS = listOf(
    "gemini-2.5-flash" to "Gemini 2.5 Flash (Recommended - Fastest & smart)",
    "gemini-1.5-flash" to "Gemini 1.5 Flash (High speed)",
    "gemini-1.5-pro" to "Gemini 1.5 Pro (Deep reasoning)",
    "gemini-2.0-flash" to "Gemini 2.0 Flash"
)

@Composable
fun SettingsAiScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appSettings = LocalAppSettings.current
    val context = LocalContext.current

    val apiKey by appSettings.geminiApiKey.collectAsStateWithLifecycle()
    val model by appSettings.geminiModel.collectAsStateWithLifecycle()
    val buttonEnabled by appSettings.aiChatButtonEnabled.collectAsStateWithLifecycle()
    val defaultWebSearch by appSettings.aiWebSearchEnabled.collectAsStateWithLifecycle()
    val defaultIncludeNotes by appSettings.aiIncludeNotesEnabled.collectAsStateWithLifecycle()
    val defaultAllowSpoilers by appSettings.aiAllowSpoilersEnabled.collectAsStateWithLifecycle()

    var inputKey by remember(apiKey) { mutableStateOf(apiKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

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
            GEMINI_MODELS.forEach { (modelId, modelLabel) ->
                RadioSettingsItem(
                    title = modelId,
                    subtitle = modelLabel,
                    selected = model == modelId,
                    onClick = { appSettings.setGeminiModel(modelId) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Universal Button & Interface Preferences
        SettingsSectionLabel("Floating Button & Preferences")
        SettingsGroup {
            SwitchSettingsItem(
                title = "Universal AI Chat Button",
                subtitle = "Show floating AI chat button in the bottom-left corner across browsing screens",
                checked = buttonEnabled,
                onCheckedChange = { appSettings.setAiChatButtonEnabled(it) }
            )

            SwitchSettingsItem(
                title = "Web Search Grounding",
                subtitle = "Enable real-time Google search grounding by default",
                checked = defaultWebSearch,
                onCheckedChange = { appSettings.setAiWebSearchEnabled(it) }
            )

            SwitchSettingsItem(
                title = "Library Notes Context",
                subtitle = "Allow AI to read your saved anime/manga notes to personalize recommendations",
                checked = defaultIncludeNotes,
                onCheckedChange = { appSettings.setAiIncludeNotesEnabled(it) }
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
