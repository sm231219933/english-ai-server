package com.TeacherTinkl.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SpeakingGymScreen(
    viewModel: SpeakingGymViewModel,
    onBackClick: () -> Unit = {}
) {
    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val gymSentence = viewModel.currentGymSentence
    val evalResult = viewModel.evaluationResult

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("English Speaking Gym 🏋️", fontWeight = FontWeight.Bold) },
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
            // LEVEL SELECTOR TABS (LEVEL 1 TO 5)
            ScrollableTabRow(
                selectedTabIndex = viewModel.currentLevel - 1,
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "L1: 1 Sentence",
                    "L2: Q&A",
                    "L3: Follow-up",
                    "L4: Think English",
                    "L5: 1-Min Challenge"
                ).forEachIndexed { index, levelName ->
                    Tab(
                        selected = viewModel.currentLevel == index + 1,
                        onClick = { viewModel.selectLevel(index + 1) },
                        text = { Text(levelName, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // LEVEL 5 TIMER DISPLAY
            if (viewModel.currentLevel == 5) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Timer: ${viewModel.timerSeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // PROMPT CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (!gymSentence?.promptHindi.isNullOrBlank()) {
                        Text(
                            text = gymSentence!!.promptHindi!!,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = gymSentence?.promptQuestion ?: "Prompt",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.playTtsPrompt() }) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Listen", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (!gymSentence?.hintPattern.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Pattern Hint: ${gymSentence!!.hintPattern}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // BIG MIC BUTTON
            Surface(
                shape = CircleShape,
                color = if (viewModel.isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = {
                        if (micPermissionState.status.isGranted) {
                            if (viewModel.isRecording) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.startSpeaking()
                            }
                        } else {
                            micPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        if (viewModel.isRecording) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                        contentDescription = "Record",
                        tint = if (viewModel.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = viewModel.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            // EVALUATION FEEDBACK CARD
            AnimatedVisibility(visible = evalResult != null) {
                evalResult?.let { result ->
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.isUnderstood)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (result.isUnderstood) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                        contentDescription = null,
                                        tint = if (result.isUnderstood) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (result.isUnderstood) "Great Speaking! 🎉" else "Needs Improvement 💡",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        "${result.matchPercentage}% Score",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                Text("You Said: \"${result.transcribedText}\"", fontWeight = FontWeight.SemiBold)

                                if (result.grammarResult.matches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("🔎 Grammar Fixes:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)

                                    result.grammarResult.matches.forEach { match ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Row(modifier = Modifier.padding(8.dp)) {
                                                Text("❌ ${match.errorText}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("➡️ ${match.replacements.firstOrNull() ?: ""}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Better Sentence: \"${result.grammarResult.correctedText}\"", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.retryGymSentence() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Try Again 🎤")
                            }

                            Button(
                                onClick = { viewModel.nextGymSentence() },
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
