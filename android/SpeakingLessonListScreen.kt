package com.TeacherTinkl.myapplication.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.TeacherTinkl.myapplication.data.SpeakingLesson
import com.TeacherTinkl.myapplication.data.TinklDatabase
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakingLessonListScreen(
    onLessonClick: (Int) -> Unit,
    onTestClick: (Int) -> Unit, // <--- NEW CALLBACK ADDED
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { TinklDatabase.getDatabase(context.applicationContext as Application) }
    var lessons by remember { mutableStateOf<List<SpeakingLesson>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.tinklDao().getAllSpeakingLessons().collectLatest { list ->
            lessons = list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speaking Practice Topics 🎧", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(lessons) { lesson ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLessonClick(lesson.id) },
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when (lesson.category) {
                                            "Waking Up" -> Icons.Rounded.Alarm
                                            "Tea & Breakfast" -> Icons.Rounded.FreeBreakfast
                                            "Restaurant", "Food & Veggies" -> Icons.Rounded.Restaurant
                                            "Job Interview", "Office" -> Icons.Rounded.Work
                                            "Pattern Building" -> Icons.Rounded.AutoAwesome
                                            "School" -> Icons.Rounded.School
                                            "Travel & Commute", "Travel & Routes" -> Icons.Rounded.DirectionsCar
                                            "Back Home" -> Icons.Rounded.Home
                                            "Home Repairs" -> Icons.Rounded.Build
                                            "Shopping" -> Icons.Rounded.ShoppingBag
                                            "Health" -> Icons.Rounded.LocalHospital
                                            "Guests & Neighbors" -> Icons.Rounded.People
                                            else -> Icons.Rounded.RecordVoiceOver
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lesson.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lesson.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = lesson.level,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }

                        // TEST BUTTON AT THE BOTTOM OF EACH TOPIC CARD
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTestClick(lesson.id) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFF8F00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Take Topic Speech Test",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
