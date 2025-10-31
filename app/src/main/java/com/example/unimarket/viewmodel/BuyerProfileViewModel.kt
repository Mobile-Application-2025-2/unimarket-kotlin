package com.example.unimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.User
import com.example.unimarket.model.session.SessionManager
import com.example.unimarket.model.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuyerProfileUiState(
    val isLoading: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null, // por ahora null, hasta que exista en sesión o BD
    val error: String? = null
)

class BuyerProfileViewModel : ViewModel() {

    private val _ui = MutableStateFlow(BuyerProfileUiState(isLoading = true))
    val ui: StateFlow<BuyerProfileUiState> = _ui

    init {
        loadProfile()
    }

    fun loadProfile() {
        _ui.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val session: UserSession? = SessionManager.get()
                val email = session?.email.orEmpty()
                val displayName = buildDisplayNameFrom(email)
                // No hay photo/url en session -> dejamos avatarUrl = null
                _ui.update {
                    it.copy(
                        isLoading = false,
                        displayName = displayName,
                        email = email,
                        avatarUrl = null
                    )
                }

                // Si luego necesitas un User de dominio:
                // val userDomain = session.toDomainUser()
                // userService.getBuyerProfile(userDomain) ...
            } catch (t: Throwable) {
                _ui.update { it.copy(isLoading = false, error = t.message ?: "Error cargando perfil") }
            }
        }
    }

    // ----------------- Helpers -----------------

    /** Deriva un nombre “bonito” desde el email si no hay nombre en sesión/BD. */
    private fun buildDisplayNameFrom(email: String): String {
        if (email.isBlank()) return "User"
        val alias = email.substringBefore('@').replaceFirstChar { c -> c.titlecase() }
        return if (alias.isNotBlank()) alias else "User"
    }

    /** Si algún servicio exige User? de dominio, mapea desde la sesión (mínimos campos). */
    private fun UserSession?.toDomainUser(): User? = this?.let {
        User(
            email = it.email.orEmpty(),
            name = buildDisplayNameFrom(it.email.orEmpty()),
            idType = "id",
            idNumber = "N/A",
            type = it.type?.lowercase()?.trim().orEmpty()
        )
    }
}