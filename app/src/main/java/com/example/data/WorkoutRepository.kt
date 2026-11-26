package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WorkoutRepository(context: Context, private val userId: String) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    )

    private val workoutsRef = db.collection("users").document(userId).collection("workouts")
    private val exercisesRef = db.collection("users").document(userId).collection("exercises")
    private val routinesRef = db.collection("users").document(userId).collection("routines")

    fun observeWorkouts(): Flow<List<Workout>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = workoutsRef.orderBy("dateMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("WorkoutRepo", "Error observing workouts", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Workout::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeExercises(): Flow<List<ExercisePR>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = exercisesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("WorkoutRepo", "Error observing exercises", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ExercisePR::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun observeRoutines(): Flow<List<Routine>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = routinesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("WorkoutRepo", "Error observing routines", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Routine::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveWorkout(workout: Workout): Result<Unit> {
        return try {
            val docRef = if (workout.id.isBlank()) workoutsRef.document() else workoutsRef.document(workout.id)
            val toSave = workout.copy(id = docRef.id, userId = userId)
            docRef.set(toSave).await()

            // Update personal bests if applicable
            for (ex in workout.exercises) {
                val maxWeight = ex.sets.maxOfOrNull { it.weightKg } ?: 0.0
                val maxReps = ex.sets.maxOfOrNull { it.reps } ?: 0
                if (maxWeight > 0) {
                    updateExercisePR(ex.exerciseName, maxWeight, maxReps)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Failed to save workout", e)
            Result.failure(e)
        }
    }

    private suspend fun updateExercisePR(exerciseName: String, weight: Double, reps: Int) {
        try {
            val query = exercisesRef.whereEqualTo("name", exerciseName).get().await()
            if (!query.isEmpty) {
                val doc = query.documents[0]
                val currentPR = doc.toObject(ExercisePR::class.java)
                if (currentPR != null && weight > currentPR.personalBestWeight) {
                    exercisesRef.document(doc.id).update(
                        mapOf(
                            "personalBestWeight" to weight,
                            "personalBestReps" to reps
                        )
                    ).await()
                }
            } else {
                val newDoc = exercisesRef.document()
                val pr = ExercisePR(
                    id = newDoc.id,
                    userId = userId,
                    name = exerciseName,
                    personalBestWeight = weight,
                    personalBestReps = reps
                )
                newDoc.set(pr).await()
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Failed to update exercise PR", e)
        }
    }

    suspend fun saveRoutine(routine: Routine): Result<Unit> {
        return try {
            val docRef = if (routine.id.isBlank()) routinesRef.document() else routinesRef.document(routine.id)
            val toSave = routine.copy(id = docRef.id, userId = userId)
            docRef.set(toSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Failed to save routine", e)
            Result.failure(e)
        }
    }

    suspend fun seedInitialDataIfNeeded() {
        try {
            val snapshot = exercisesRef.get().await()
            if (snapshot.isEmpty) {
                val defaultExercises = listOf(
                    ExercisePR(userId = userId, name = "Barbell Bench Press", category = "Chest", personalBestWeight = 80.0, personalBestReps = 8),
                    ExercisePR(userId = userId, name = "Back Squat", category = "Legs", personalBestWeight = 100.0, personalBestReps = 5),
                    ExercisePR(userId = userId, name = "Deadlift", category = "Back", personalBestWeight = 120.0, personalBestReps = 3),
                    ExercisePR(userId = userId, name = "Overhead Press", category = "Shoulders", personalBestWeight = 50.0, personalBestReps = 8),
                    ExercisePR(userId = userId, name = "Barbell Row", category = "Back", personalBestWeight = 70.0, personalBestReps = 10)
                )
                for (ex in defaultExercises) {
                    exercisesRef.document(ex.name.lowercase().replace(" ", "_")).set(ex).await()
                }

                val defaultRoutines = listOf(
                    Routine(userId = userId, name = "Push Day (Chest/Shoulders/Triceps)", description = "Classic hypertrophy push session", exercises = listOf("Barbell Bench Press", "Overhead Press")),
                    Routine(userId = userId, name = "Pull Day (Back/Biceps)", description = "Heavy posterior chain & width", exercises = listOf("Deadlift", "Barbell Row")),
                    Routine(userId = userId, name = "Leg Day (Quads/Hamstrings)", description = "Lower body power & endurance", exercises = listOf("Back Squat"))
                )
                for (rot in defaultRoutines) {
                    routinesRef.add(rot).await()
                }
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Failed to seed initial data", e)
        }
    }
}
