package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExercisePR
import com.example.data.Routine
import com.example.data.Workout
import com.example.data.WorkoutRepository
import com.example.ui.ai.AiCoachScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.home.HomeScreen
import com.example.ui.prs.PersonalBestsScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.templates.TemplatesScreen
import com.example.ui.theme.ApexFitTheme
import com.example.ui.workout.ActiveWorkoutScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // MANDATORY: Configure App Check before any UI or Firebase operations
        configureAppCheck(this, intent)

        setContent {
            ApexFitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainRoot()
                }
            }
        }
    }
}

sealed interface NavTab {
    data object Home : NavTab
    data object Templates : NavTab
    data object AiCoach : NavTab
    data object PersonalBests : NavTab
    data object Profile : NavTab
}

@Composable
fun AppMainRoot() {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var currentTab by remember { mutableStateOf<NavTab>(NavTab.Home) }
    var isWorkoutActive by remember { mutableStateOf(false) }
    var activeRoutine by remember { mutableStateOf<Routine?>(null) }

    val userId = currentUser?.uid ?: ""
    val repository = remember(userId) { WorkoutRepository(context, userId) }
    val scope = rememberCoroutineScope()

    val workouts by repository.observeWorkouts().collectAsStateWithLifecycle(initialValue = emptyList())
    val exercises by repository.observeExercises().collectAsStateWithLifecycle(initialValue = emptyList())
    val routines by repository.observeRoutines().collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            repository.seedInitialDataIfNeeded()
        }
    }

    if (currentUser == null) {
        AuthScreen(onSignedIn = {
            currentUser = Firebase.auth.currentUser
        })
    } else if (isWorkoutActive) {
        ActiveWorkoutScreen(
            initialRoutine = activeRoutine,
            onSaveWorkout = { workout ->
                scope.launch {
                    repository.saveWorkout(workout)
                    isWorkoutActive = false
                    activeRoutine = null
                }
            },
            onCancel = {
                isWorkoutActive = false
                activeRoutine = null
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF13151F),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentTab is NavTab.Home,
                        onClick = { currentTab = NavTab.Home },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00E5FF),
                            selectedTextColor = Color(0xFF00E5FF),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF1E2235)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.BookmarkBorder, contentDescription = "Templates") },
                        label = { Text("Templates") },
                        selected = currentTab is NavTab.Templates,
                        onClick = { currentTab = NavTab.Templates },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00E5FF),
                            selectedTextColor = Color(0xFF00E5FF),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF1E2235)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Coach") },
                        label = { Text("AI") },
                        selected = currentTab is NavTab.AiCoach,
                        onClick = { currentTab = NavTab.AiCoach },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00E5FF),
                            selectedTextColor = Color(0xFF00E5FF),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF1E2235)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "PRs") },
                        label = { Text("PRs") },
                        selected = currentTab is NavTab.PersonalBests,
                        onClick = { currentTab = NavTab.PersonalBests },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00E5FF),
                            selectedTextColor = Color(0xFF00E5FF),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF1E2235)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentTab is NavTab.Profile,
                        onClick = { currentTab = NavTab.Profile },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00E5FF),
                            selectedTextColor = Color(0xFF00E5FF),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF1E2235)
                        )
                    )
                }
            },
            containerColor = Color(0xFF0F1016)
        ) { paddingVals ->
            Box(modifier = Modifier.padding(paddingVals)) {
                when (currentTab) {
                    is NavTab.Home -> HomeScreen(
                        workouts = workouts,
                        userName = currentUser?.displayName ?: "Athlete",
                        onStartWorkoutClick = {
                            activeRoutine = null
                            isWorkoutActive = true
                        }
                    )
                    is NavTab.Templates -> TemplatesScreen(
                        routines = routines,
                        onCreateTemplate = { routine ->
                            scope.launch {
                                repository.saveRoutine(routine)
                            }
                        },
                        onStartWorkoutFromTemplate = { routine ->
                            activeRoutine = routine
                            isWorkoutActive = true
                        }
                    )
                    is NavTab.AiCoach -> AiCoachScreen()
                    is NavTab.PersonalBests -> PersonalBestsScreen(exercises = exercises)
                    is NavTab.Profile -> ProfileScreen(onSignedOut = {
                        currentUser = null
                    })
                }
            }
        }
    }
}
