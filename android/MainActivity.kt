package com.TeacherTinkl.myapplication

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.TeacherTinkl.myapplication.navigation.Navigator
import com.TeacherTinkl.myapplication.navigation.TinklDestination
import com.TeacherTinkl.myapplication.navigation.rememberNavigationState
import com.TeacherTinkl.myapplication.navigation.toEntries
import com.TeacherTinkl.myapplication.ui.screens.*
import com.TeacherTinkl.myapplication.ui.theme.TinklTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TinklTheme {
                TinklApp()
            }
        }
    }
}

@Composable
fun TinklApp() {
    val startRoute = remember { TinklDestination.Home }

    val navigationState = rememberNavigationState(
        startRoute = startRoute,
        topLevelRoutes = setOf(startRoute)
    )
    val navigator = remember { Navigator(navigationState) }

    val entryProvider: (NavKey) -> NavEntry<NavKey> = remember {
        entryProvider<NavKey> {
            entry<TinklDestination.Home> {
                HomeScreen(
                    onScanClick = { navigator.navigate(TinklDestination.Chat()) },
                    onGalleryImagesProcessed = { text -> navigator.navigate(TinklDestination.Chat(text)) },
                    onSpeakingPracticeClick = { navigator.navigate(TinklDestination.SpeakingLessonList) },
                    onSpeakingGymClick = { navigator.navigate(TinklDestination.SpeakingGym) }
                )
            }

            entry<TinklDestination.SpeakingGym> {
                val context = LocalContext.current
                val viewModel: SpeakingGymViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SpeakingGymViewModel(application = context.applicationContext as Application) as T
                        }
                    }
                )
                SpeakingGymScreen(
                    viewModel = viewModel,
                    onBackClick = { navigator.goBack() }
                )
            }

            entry<TinklDestination.Chat> { key ->
                val context = LocalContext.current
                val viewModel: ChatViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChatViewModel(application = context.applicationContext as Application, initialText = key.initialText) as T
                        }
                    }
                )
                ChatScreen(
                    viewModel = viewModel,
                    onBackClick = { navigator.goBack() }
                )
            }

            entry<TinklDestination.SpeakingLessonList> {
                SpeakingLessonListScreen(
                    onLessonClick = { lessonId -> navigator.navigate(TinklDestination.SpeakingLesson(lessonId)) },
                    onTestClick = { lessonId -> navigator.navigate(TinklDestination.SpeakingTest(lessonId)) },
                    onBackClick = { navigator.goBack() }
                )
            }

            entry<TinklDestination.SpeakingLesson> { key ->
                val context = LocalContext.current
                val viewModel: SpeakingLessonViewModel = viewModel(
                    key = "SpeakingLessonViewModel_${key.lessonId}",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SpeakingLessonViewModel(application = context.applicationContext as Application, lessonId = key.lessonId) as T
                        }
                    }
                )
                SpeakingLessonScreen(
                    viewModel = viewModel,
                    onBackClick = { navigator.goBack() }
                )
            }

            entry<TinklDestination.SpeakingTest> { key ->
                val context = LocalContext.current
                val viewModel: SpeakingTestViewModel = viewModel(
                    key = "SpeakingTestViewModel_${key.lessonId}",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SpeakingTestViewModel(application = context.applicationContext as Application, lessonId = key.lessonId) as T
                        }
                    }
                )
                SpeakingTestScreen(
                    viewModel = viewModel,
                    onBackClick = { navigator.goBack() },
                    onFinishTestClick = { navigator.navigate(TinklDestination.SpeakingLessonList) }
                )
            }
        }
    }

    Scaffold { padding ->
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() },
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TinklAppPreview() {
    TinklTheme {
        TinklApp()
    }
}
