package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.NiloraLanguage
import com.example.ui.components.NiloraTopBar
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.TurquoiseContainer
import com.example.ui.theme.TurquoiseOnContainer
import com.example.ui.viewmodel.AssistantMessageRole
import com.example.ui.viewmodel.TranslatorUiState

@Composable
fun AiTravelAssistantScreen(
    language: NiloraLanguage,
    state: TranslatorUiState,
    verifiedCountSummary: String,
    onAskQuestion: (String) -> Unit
) {
    var promptText by remember { mutableStateOf("") }
    val suggestions = listOf(
        "What verified destinations and events are currently in NILORA?",
        "Are there open sports or social team-ups near Cairo?",
        "Suggest a safe Real-World Mission and how to turn my 3D adventure into a real trip.",
        "Translate common taxi, stadium, and meetup phrases into Egyptian Arabic."
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NiloraTopBar(
            title = "AI Travel Assistant",
            subtitle = "Gemini Grounded • $verifiedCountSummary"
        )

        Surface(color = TurquoiseContainer, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Grounded Provenance: Answers separate [Verified Fact], [User-Provided], [AI Suggestion], and [Unverified / Unavailable]. The assistant never invents prices or confirms bookings.",
                style = MaterialTheme.typography.labelSmall,
                color = TurquoiseOnContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (state.assistantError != null) {
            Text(
                text = state.assistantError,
                color = ErrorCoral,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.assistantMessages.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Ask NILORA AI Guide", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            "Compare verified places, understand local customs, learn Egyptian Arabic phrases, or plan an itinerary using verified NILORA data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(suggestions) { item ->
                                SuggestionChip(
                                    onClick = { onAskQuestion(item) },
                                    label = { Text(item, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            } else {
                items(state.assistantMessages) { msg ->
                    val isUser = msg.role == AssistantMessageRole.User
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 330.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) DeepNavy else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = msg.text.ifBlank { "..." },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                if (msg.isStreaming) {
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(tonalElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("Ask in ${language.displayName}...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("ai_assistant_input")
                )
                FilledIconButton(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            onAskQuestion(promptText)
                            promptText = ""
                        }
                    },
                    enabled = promptText.isNotBlank() && !state.isAssistantStreaming,
                    modifier = Modifier.testTag("ai_assistant_send_btn")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send prompt")
                }
            }
        }
    }
}
