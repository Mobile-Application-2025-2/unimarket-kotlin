package com.example.unimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.session.SessionManager
import com.example.unimarket.model.domain.service.AuthService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class BuyerNavDestination {
    object None : BuyerNavDestination()
    object ToWelcome : BuyerNavDestination()
}

data class BuyerUiState(
    val displayName: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val nav: BuyerNavDestination = BuyerNavDestination.None
)

class BuyerViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: AuthService = AuthService()
) : ViewModel() {

    private val _ui = MutableStateFlow(BuyerUiState())
    val ui: StateFlow<BuyerUiState> = _ui

    init {
        loadUserName()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val sess = SessionManager.get()
                val uid = sess?.uid.orEmpty()
                val email = sess?.email.orEmpty()

                var nameFromDb = ""
                if (uid.isNotEmpty()) {
                    val snap = db.collection("users").document(uid).get().await()
                    nameFromDb = snap.getString("name").orEmpty()
                }

                val alias = email.substringBefore("@").ifBlank { "Usuario" }
                val display = if (nameFromDb.isNotBlank()) nameFromDb else alias

                _ui.update { it.copy(displayName = display, loading = false) }
            } catch (t: Throwable) {
                _ui.update { it.copy(loading = false, error = t.message ?: "Error leyendo tu perfil") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } finally {
                _ui.update { it.copy(nav = BuyerNavDestination.ToWelcome) }
            }
        }
    }

    fun clearNavAndError() {
        _ui.update { it.copy(nav = BuyerNavDestination.None, error = null) }
    }
}