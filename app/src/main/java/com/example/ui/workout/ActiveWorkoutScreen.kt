package com.example.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExerciseSet
import com.example.data.Workout
import com.example.data.WorkoutExercise
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    initialTitle: String = "Upper Body Workout",
    onSaveWorkout: (Workout) -> Unit,
    onCancel: () -> Unit
) {
    var workoutTitle by remember { mutableStateOf(initialTitle) }
    var notes by remember { mutableStateOf("") }
    var exercises by remember { mutableStateOf(
        mutableListOf(
            WorkoutExercise(
                exerciseName = "Barbell Bench Press",
                mutableListOf(ExerciseSet(1, 70.0, 10, 8.0), ExerciseSet(2, 75.0, 8, 8.5))
            )
        )
    ) }

    var secondsElapsed by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            secondsElapsed++
        }
    }

    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(workoutTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Duration: $timeFormatted", color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val workout = Workout(
                                title = workoutTitle,
                                dateMillis = System.currentTimeMillis(),
                                durationMinutes = (secondsElapsed / 60).coerceAtLeast(1),
                                notes = notes,
                                exercises = exercises
                            )
                            onSaveWorkout(workout)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Finish", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1016))
            )
        },
        containerColor = Color(0xFF0F1016)
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = workoutTitle,
                    onValueChange = { workoutTitle = it },
                    label = { Text("Workout Name", color = Color(0xFF94A3B8)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF2A2D3D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            itemsIndexed(exercises) { exIndex, exercise ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161824)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise.exerciseName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            IconButton(onClick = {
                                exercises = exercises.filterIndexed { index, _ -> index != exIndex }.toMutableList()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Exercise", tint = Color(0xFFFF8A8A))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SET", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.weight(0.8f))
                            Text("KG", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.weight(1.2f))
                            Text("REPS", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.weight(1.2f))
                            Spacer(modifier = Modifier.width(48.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        exercise.sets.forEachIndexed { setIndex, setItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = Color(0xFF222536),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${setIndex + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedTextField(
                                    value = if (setItem.weightKg > 0.0) setItem.weightKg.toString() else "",
                                    onValueChange = { newVal ->
                                        val w = newVal.toDoubleOrNull() ?: 0.0
                                        val updatedSets = exercise.sets.toMutableList()
                                        updatedSets[setIndex] = setItem.copy(weightKg = w)
                                        exercises = exercises.mapIndexed { idx, e ->
                                            if (idx == exIndex) e.copy(sets = updatedSets) else e
                                        }.toMutableList()
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF2A2D3D),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1.2f).height(50.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedTextField(
                                    value = if (setItem.reps > 0) setItem.reps.toString() else "",
                                    onValueChange = { newVal ->
                                        val r = newVal.toIntOrNull() ?: 0
                                        val updatedSets = exercise.sets.toMutableList()
                                        updatedSets[setIndex] = setItem.copy(reps = r)
                                        exercises = exercises.mapIndexed { idx, e ->
                                            if (idx == exIndex) e.copy(sets = updatedSets) else e
                                        }.toMutableList()
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF2A2D3D),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1.2f).height(50.dp)
                                )

                                IconButton(onClick = {
                                    val updatedSets = exercise.sets.filterIndexed { sIdx, _ -> sIdx != setIndex }
                                    exercises = exercises.mapIndexed { idx, e ->
                                        if (idx == exIndex) e.copy(sets = updatedSets) else e
                                    }.toMutableList()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete Set", tint = Color(0xFF64748B))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                val lastSet = exercise.sets.lastOrNull()
                                val newSet = ExerciseSet(
                                    setNumber = exercise.sets.size + 1,
                                    weightKg = lastSet?.weightKg ?: 60.0,
                                    reps = lastSet?.reps ?: 10
                                )
                                val updatedSets = exercise.sets + newSet
                                exercises = exercises.mapIndexed { idx, e ->
                                    if (idx == exIndex) e.copy(sets = updatedSets) else e
                                }.toMutableList()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Set")
                        }
                    }
                }
            }

            item {
                var newExerciseName by remember { mutableStateOf("") }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13151F)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add Another Exercise",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newExerciseName,
                                onValueChange = { newExerciseName = it },
                                placeholder = { Text("e.g. Back Squat, Pull-ups", color = Color(0xFF64748B)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF2A2D3D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).height(56.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    if (newExerciseName.isNotBlank()) {
                                        exercises = (exercises + WorkoutExercise(
                                            exerciseName = newExerciseName.trim(),
                                            sets = listOf(ExerciseSet(1, 50.0, 10))
                                        )).toMutableList()
                                        newExerciseName = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
