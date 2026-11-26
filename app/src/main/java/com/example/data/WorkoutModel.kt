package com.example.data

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
@IgnoreExtraProperties
data class ExerciseSet(
    val setNumber: Int = 1,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val rpe: Double = 8.0,
    val completed: Boolean = true
)

@Keep
@IgnoreExtraProperties
data class WorkoutExercise(
    val exerciseName: String = "",
    val sets: List<ExerciseSet> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class Workout(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 0,
    val notes: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null
)

@Keep
@IgnoreExtraProperties
data class ExercisePR(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Chest",
    val personalBestWeight: Double = 0.0,
    val personalBestReps: Int = 0,
    @ServerTimestamp
    val updatedAt: Date? = null
)

@Keep
@IgnoreExtraProperties
data class Routine(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val exercises: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null
)
