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

// 👇 Respeto los nombres existentes
sealed class BusinessNavDestination {
    object None : BusinessNavDestination()
    object ToWelcome : BusinessNavDestination()
}

data class BusinessUiState(
    val displayName: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val nav: BusinessNavDestination = BusinessNavDestination.None
)

class BusinessViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: AuthService = AuthService()
) : ViewModel() {

    private val _ui = MutableStateFlow(BusinessUiState())
    val ui: StateFlow<BusinessUiState> = _ui

    init {
        loadUserName()
    }

    /** Lee el name desde users/{uid}.name; si no existe, usa alias del email. */
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

    /** Logout conservando tu navegación existente. */
    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } finally {
                _ui.update { it.copy(nav = BusinessNavDestination.ToWelcome) }
            }
        }
    }

    fun clearNavAndError() {
        _ui.update { it.copy(nav = BusinessNavDestination.None, error = null) }
    }
}