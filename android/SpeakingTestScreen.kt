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
import androidx.compose.material.icons.automirrored.rounded.Send
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
fun SpeakingTestScreen(
    viewModel: SpeakingTestViewModel,
    onBackClick: () -> Unit,
    onFinishTestClick: () -> Unit
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
                        text = "Speech Test: ${lesson?.title ?: ""}",
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
            if (viewModel.isTestCompleted) {
                // TEST COMPLETION SUMMARY CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Speech Test Passed! 🎉",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You successfully passed all ${viewModel.sentencesList.size} sentences in this topic!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onFinishTestClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Back to Lessons 🏆", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // TEST PROGRESS HEADER
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
                            text = "Test Question ${viewModel.currentSentenceIndex + 1} of ${viewModel.sentencesList.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // HINDI QUESTION CARD (ENGLISH ANSWER HIDDEN INITIALLY)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "CHALLENGE (Translate in English):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!sentence?.promptHindi.isNullOrBlank()) {
                            Text(
                                text = "Hindi: ${sentence!!.promptHindi}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // HINT / ANSWER REVEAL CARD
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (viewModel.isHintUnlocked)
                                Color(0xFFE8F5E9)
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (!viewModel.isHintUnlocked) {
                                    viewModel.revealHint()
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (viewModel.isHintUnlocked) Icons.Rounded.Lightbulb else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = if (viewModel.isHintUnlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (viewModel.isHintUnlocked)
                                        "Expected: \"${sentence?.expectedText ?: ""}\""
                                    else
                                        "💡 Tap to Reveal Hint",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewModel.isHintUnlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                )
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
                    text = if (viewModel.isRecording) "🎤 Recording... Tap Mic again to stop" else viewModel.statusMessage,
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

                    Button(
                        onClick = { viewModel.sendAndEvaluateTestAnswer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("➤ Send & Evaluate Answer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // EVALUATION FEEDBACK
                AnimatedVisibility(visible = evalResult != null) {
                    evalResult?.let { result ->
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (viewModel.isPassed)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (viewModel.isPassed) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                            contentDescription = null,
                                            tint = if (viewModel.isPassed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (viewModel.isPassed) "✅ Correct Answer!" else "❌ Incorrect Answer",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${result.matchPercentage}% Match",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // NEXT SENTENCE BUTTON (DISABLED UNTIL PASSED)
                Button(
                    onClick = { viewModel.nextSentence() },
                    enabled = viewModel.isPassed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.isPassed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = if (viewModel.isPassed) "Next Sentence ➡️" else "🔒 Speak Correctly to Unlock Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
