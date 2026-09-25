package com.TeacherTinkl.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SpeakingLessonScreen(
    viewModel: SpeakingLessonViewModel,
    onBackClick: () -> Unit
) {
    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val sentence = viewModel.currentSentence
    val lesson = viewModel.lessonState
    val evalResult = viewModel.evaluationResult

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        lesson?.title ?: "Speaking Practice 🎤",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PERSON PERSPECTIVE FILTER TABS
            ScrollableTabRow(
                selectedTabIndex = when (viewModel.selectedPersonFilter) {
                    "1st Person" -> 1
                    "2nd Person" -> 2
                    "3rd Person" -> 3
                    else -> 0
                },
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("All", "1st Person (Main)", "2nd Person (Tum/Aap)", "3rd Person (Wo/Ye)").forEach { filter ->
                    Tab(
                        selected = viewModel.selectedPersonFilter.contains(filter.take(3)),
                        onClick = {
                            val tag = when {
                                filter.startsWith("1st") -> "1st Person"
                                filter.startsWith("2nd") -> "2nd Person"
                                filter.startsWith("3rd") -> "3rd Person"
                                else -> "All"
                            }
                            viewModel.setPersonFilter(tag)
                        },
                        text = { Text(filter, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LESSON PROGRESS HEADER
            if (viewModel.sentencesList.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { (viewModel.currentSentenceIndex + 1).toFloat() / viewModel.sentencesList.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sentence ${viewModel.currentSentenceIndex + 1} of ${viewModel.sentencesList.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = sentence?.personTag ?: "1st Person",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PROMPT & HINDI DISPLAY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (!sentence?.promptHindi.isNullOrBlank()) {
                        Text(
                            text = "Hindi: ${sentence!!.promptHindi}",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = sentence?.promptQuestion ?: "Prompt",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sentence?.expectedText ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.playCurrentSentenceTts() }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Listen",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // MIC BUTTON SECTION (SINGLE TAP TOGGLE)
            Surface(
                shape = CircleShape,
                color = if (viewModel.isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp),
                onClick = {
                    if (micPermissionState.status.isGranted) {
                        viewModel.stopSpeaking()
                        if (viewModel.isRecording) {
                            viewModel.stopListeningUserSpeech()
                        } else {
                            viewModel.startListeningUserSpeech()
                        }
                    } else {
                        micPermissionState.launchPermissionRequest()
                    }
                },
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        if (viewModel.isRecording) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                        contentDescription = "Record Speech",
                        tint = if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (viewModel.isRecording) "🎤 Recording... Tap Mic again to stop" else "Tap 🎤 to speak sentence",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            // LIVE SPEECH DISPLAY & SEND BUTTON
            if (viewModel.liveSpokenText.isNotBlank() || viewModel.isRecording) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Live Speech Captured:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (viewModel.liveSpokenText.isNotBlank()) "\"${viewModel.liveSpokenText}\"" else "Listening...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SEND / EVALUATE BUTTON
                Button(
                    onClick = { viewModel.sendAndEvaluateSpeech() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("➤ Send & Check Speech", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // EVALUATION RESULT CARD
            AnimatedVisibility(visible = evalResult != null) {
                evalResult?.let { result ->
                    Column(modifier = Modifier.padding(top = 20.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.isUnderstood)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (result.isUnderstood) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                        contentDescription = null,
                                        tint = if (result.isUnderstood) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Speech Feedback",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        "${result.matchPercentage}% Match",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                Text(
                                    text = "You said: \"${result.transcribedText}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // OFFLINE LANGUAGE TOOL GRAMMAR FEEDBACK
                                if (result.grammarResult.matches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "🔎 Offline LanguageTool Feedback:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )

                                    result.grammarResult.matches.forEach { match ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Row(modifier = Modifier.padding(8.dp)) {
                                                Text("❌ '${match.errorText}'", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("➡️ '${match.replacements.firstOrNull() ?: ""}'", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Better sentence: \"${result.grammarResult.correctedText}\"",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ACTION BUTTONS (RETRY & NEXT)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.retryCurrentSentence() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Try Again 🎤")
                            }

                            Button(
                                onClick = { viewModel.nextSentence() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Next ➡️")
                            }
                        }
                    }
                }
            }
        }
    }
}
