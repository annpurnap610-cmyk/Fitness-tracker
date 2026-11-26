package com.example.ui.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiCoachViewModel : ViewModel() {
    private val TAG = "AiCoachVM"

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun askAiCoach(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                val fullPrompt = "You are an elite, expert strength & conditioning coach and exercise physiologist. Provide a detailed, professional, structured workout recommendation or answer for the following fitness query:\n\n$prompt"
                val response = withTimeout(60_000L) {
                    model.generateContent(fullPrompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _uiState.value = AiUiState.Success(text)
                } else {
                    _uiState.value = AiUiState.Error("AI Coach returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI request timed out", e)
                _uiState.value = AiUiState.Error("Request timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI generation failed", e)
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "AI generation failed.")
            }
        }
    }
}
