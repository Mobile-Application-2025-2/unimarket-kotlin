package com.example.unimarket.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.model.domain.entity.User
import com.example.unimarket.model.domain.entity.Category
import com.example.unimarket.model.domain.service.AuthService
import com.example.unimarket.model.domain.service.CategoryService
import com.example.unimarket.model.domain.service.BusinessService
import com.example.unimarket.model.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AuthNavDestination {
    object None : AuthNavDestination()
    object ToCreateAccount : AuthNavDestination()
    object ToLogin : AuthNavDestination()
    object ToStudentCode : AuthNavDestination()
    object ToBuyerHome : AuthNavDestination()
    object ToBusinessProfile : AuthNavDestination()
}

data class WelcomeUiState(
    val shouldPlayIntro: Boolean = false,
    val nav: AuthNavDestination = AuthNavDestination.None
)

data class SignInUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val nav: AuthNavDestination = AuthNavDestination.None
)

data class CreateAccountUiState(
    val name: String = "",
    val nameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val acceptedPolicy: Boolean = false,
    val acceptedPolicyError: String? = null,
    val isSubmitting: Boolean = false,
    val toastMessage: String? = null,
    val nav: AuthNavDestination = AuthNavDestination.None
)

enum class OnbStep { CODE, ADDRESS, LOGO, CATEGORIES }

data class StudentCodeUiState(
    val step: OnbStep = OnbStep.CODE,
    val title: String = "Código estudiantil",
    val subtitle: String = "",
    val hint: String = "Student ID",
    val cta: String = "Continuar",

    val textValue: String = "",
    val canProceed: Boolean = false,
    val errorMessage: String? = null,
    val requestOpenCamera: Boolean = false,
    val nav: AuthNavDestination = AuthNavDestination.None,

    val role: String = "",

    val categories: List<String> = emptyList(),
    val selectedCats: List<String> = emptyList()
)

class AuthViewModel(
    private val authService: AuthService = AuthService(),
    private val categoryService: CategoryService = CategoryService(),
    private val businessService: BusinessService = BusinessService()
) : ViewModel() {

    private var tmpCode: String = ""
    private var tmpAddress: String = ""
    private var tmpLogoUrl: String = ""

    private var availableCats: List<Category> = emptyList()

    private val _welcome = MutableStateFlow(WelcomeUiState())
    val welcome: StateFlow<WelcomeUiState> = _welcome

    private val _signIn = MutableStateFlow(SignInUiState())
    val signIn: StateFlow<SignInUiState> = _signIn

    private val _create = MutableStateFlow(CreateAccountUiState())
    val create: StateFlow<CreateAccountUiState> = _create

    private val _student = MutableStateFlow(StudentCodeUiState())
    val student: StateFlow<StudentCodeUiState> = _student

    /* -----------------------------
       WELCOME
    ------------------------------ */
    fun welcome_onInit() {
        viewModelScope.launch {
            SessionManager.ensureFreshIdToken(forceRefresh = false)
            val session = SessionManager.get()

            if (session != null) {
                val role = session.type.trim().lowercase()
                when (role) {
                    "buyer" -> _welcome.update { it.copy(shouldPlayIntro = false, nav = AuthNavDestination.ToBuyerHome) }
                    "business" -> _welcome.update { it.copy(shouldPlayIntro = false, nav = AuthNavDestination.ToBusinessProfile) }
                    else -> _welcome.update { it.copy(shouldPlayIntro = true, nav = AuthNavDestination.None) }
                }
            } else {
                _welcome.update { it.copy(shouldPlayIntro = true, nav = AuthNavDestination.None) }
            }
        }
    }

    fun welcome_onClickSignUp() { _welcome.update { it.copy(nav = AuthNavDestination.ToCreateAccount) } }
    fun welcome_onClickLogin()  { _welcome.update { it.copy(nav = AuthNavDestination.ToLogin) } }
    fun welcome_clearNav()      { _welcome.update { it.copy(nav = AuthNavDestination.None) } }

    /* -----------------------------
       SIGN IN
    ------------------------------ */
    fun signIn_onEmailChanged(newEmail: String) {
        _signIn.update { it.copy(email = newEmail.trim().lowercase(), emailError = null) }
    }

    fun signIn_onPasswordChanged(newPass: String) {
        _signIn.update { it.copy(password = newPass, passwordError = null) }
    }

    fun signIn_submit() {
        val email = _signIn.value.email.trim().lowercase()
        val pass  = _signIn.value.password

        var emailErr: String? = null
        var passErr: String? = null

        if (!EMAIL_REGEX.matches(email)) emailErr = "Invalid email"
        if (pass.length < 8) passErr = "Password must be at least 8 characters"

        if (emailErr != null || passErr != null) {
            _signIn.update { it.copy(emailError = emailErr, passwordError = passErr) }
            return
        }

        _signIn.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val result = authService.signIn(email, pass)
                result.onSuccess { user ->
                    SessionManager.ensureFreshIdToken(forceRefresh = false)
                    val session = SessionManager.get()
                    val role = (session?.type ?: user.type).trim().lowercase()

                    val mustOnboard = (user.onboardingCompleted != true)

                    val dest = if (mustOnboard) {
                        AuthNavDestination.ToStudentCode
                    } else {
                        when (role) {
                            "buyer" -> AuthNavDestination.ToBuyerHome
                            "business" -> AuthNavDestination.ToBusinessProfile
                            else -> {
                                _signIn.update { it.copy(errorMessage = "Tipo de usuario desconocido: $role") }
                                AuthNavDestination.None
                            }
                        }
                    }
                    _signIn.update { it.copy(nav = dest) }
                }.onFailure { e ->
                    _signIn.update { it.copy(errorMessage = e.message ?: "Error al iniciar sesión") }
                }
            } catch (t: Throwable) {
                _signIn.update { it.copy(errorMessage = t.message ?: "Error al iniciar sesión") }
            } finally {
                _signIn.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun signIn_clearNavAndErrors() {
        _signIn.update { it.copy(nav = AuthNavDestination.None, errorMessage = null) }
    }

    /* -----------------------------
       CREATE ACCOUNT (Sign Up)
    ------------------------------ */
    fun create_onNameChanged(newName: String) {
        _create.update { it.copy(name = newName.trim().lowercase(), nameError = null) }
    }

    fun create_onEmailChanged(newEmail: String) {
        _create.update { it.copy(email = newEmail.trim().lowercase(), emailError = null) }
    }

    fun create_onPasswordChanged(newPass: String) {
        _create.update { it.copy(password = newPass, passwordError = null) }
    }

    fun create_onPolicyToggled(accepted: Boolean) {
        _create.update { it.copy(acceptedPolicy = accepted, acceptedPolicyError = null) }
    }

    fun create_submit(
        idType: String = "id",
        idNumber: String = "N/A",
        type: String,
        businessName: String? = null,
        businessLogo: String? = null
    ) {
        val cur   = _create.value
        val name  = cur.name.trim().lowercase()
        val email = cur.email.trim().lowercase()
        val pass  = cur.password
        val policyOk = cur.acceptedPolicy

        val typeNorm = type.trim().lowercase()

        var nameErr: String? = null
        var emailErr: String? = null
        var passErr: String? = null
        var policyErr: String? = null

        if (name.length < 3)             nameErr = "Name too short"
        if (!EMAIL_REGEX.matches(email)) emailErr = "Invalid email"
        if (pass.length < 8)             passErr = "Password must be at least 8 characters"
        if (!policyOk)                   policyErr = "Debes aceptar la política"

        if (nameErr != null || emailErr != null || passErr != null || policyErr != null) {
            _create.update {
                it.copy(
                    nameError = nameErr,
                    emailError = emailErr,
                    passwordError = passErr,
                    acceptedPolicyError = policyErr
                )
            }
            return
        }

        _create.update { it.copy(isSubmitting = true, toastMessage = null) }

        viewModelScope.launch {
            try {
                val user = User(
                    email     = email,
                    name      = name,
                    idType    = idType,
                    idNumber  = idNumber,
                    type      = typeNorm
                )
                val result = authService.signUp(
                    user = user,
                    password = pass,
                    businessName = businessName ?: if (typeNorm == "business") name else null,
                    businessLogo = businessLogo,
                    businessAddress = null,
                    buyerAddresses = null
                )
                result.onSuccess { createdUser ->
                    SessionManager.ensureFreshIdToken(forceRefresh = false)
                    val session = SessionManager.get()
                    val role = (session?.type ?: createdUser.type).trim().lowercase()
                    _create.update {
                        it.copy(
                            toastMessage = "Cuenta creada ($role). Revisa la bandeja de tu correo o spam para verificar tu cuenta para iniciar sesión.",
                            nav = AuthNavDestination.ToLogin
                        )
                    }
                }.onFailure { e ->
                    _create.update { it.copy(toastMessage = "Error: ${e.message}") }
                }
            } catch (t: Throwable) {
                _create.update { it.copy(toastMessage = "Error: ${t.message}") }
            } finally {
                _create.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun create_clearNavAndToast() {
        _create.update { it.copy(nav = AuthNavDestination.None, toastMessage = null) }
    }

    /* -----------------------------
       STUDENT CODE
    ------------------------------ */

    fun student_init() {
        viewModelScope.launch {
            SessionManager.ensureFreshIdToken(false)
            val claimRole = SessionManager.get()?.type?.trim()?.lowercase().orEmpty()
            val role = if (claimRole.isNotBlank()) claimRole
            else (authService.currentUser().getOrNull()?.type ?: "").trim().lowercase()

            val title = if (role == "business") "Documento/NIT" else "Código estudiantil"
            val hint  = if (role == "business") "NIT o documento" else "Código"

            val subtitleText = "Si eres estudiante, ingresa tu código estudiantil. " +
                    "Si eres externo a la universidad, ingresa tu NIT o cédula."

            _student.update {
                it.copy(
                    role = role,
                    step = OnbStep.CODE,
                    title = title,
                    subtitle = subtitleText,
                    hint = hint,
                    cta = "Continuar",
                    textValue = "",
                    canProceed = false,
                    errorMessage = null
                )
            }
        }
    }

    fun student_onInputChanged(newText: String) {
        val t = newText.trim()
        val st = _student.value
        val ok = when (st.step) {
            OnbStep.CODE       -> t.length >= 3
            OnbStep.ADDRESS    -> t.isNotEmpty()
            OnbStep.LOGO       -> URL_REGEX.matches(t)
            OnbStep.CATEGORIES -> st.selectedCats.isNotEmpty() && st.selectedCats.size <= 3
        }
        _student.update { it.copy(textValue = t, canProceed = ok, errorMessage = null) }
    }
    private val URL_REGEX = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")

    fun student_next() {
        val st = _student.value

        val allow = when (st.step) {
            OnbStep.CATEGORIES -> st.selectedCats.isNotEmpty() && st.selectedCats.size <= 3
            else -> st.canProceed
        }
        if (!allow) {
            val msg = if (st.step == OnbStep.CATEGORIES)
                "Selecciona entre 1 y 3 categorías."
            else
                "Completa el campo para continuar"
            _student.update { it.copy(errorMessage = msg) }
            return
        }

        when (st.step) {
            OnbStep.CODE -> {
                tmpCode = st.textValue
                val title = "Dirección"
                val hint = "Dirección"
                val subtitleDir = "Escribe la dirección en la que recibes los pedidos " +
                        "o donde ofreces los productos o servicios."

                _student.update {
                    it.copy(
                        step = OnbStep.ADDRESS,
                        title = title,
                        subtitle = subtitleDir,
                        hint = hint,
                        cta = if (st.role == "business") "Continuar" else "Finalizar",
                        textValue = "",
                        canProceed = false,
                        errorMessage = null
                    )
                }
            }
            OnbStep.ADDRESS -> {
                tmpAddress = st.textValue
                if (st.role == "business") {
                    val subtitleLogo = "Escribe la URL del logo de tu negocio (por ejemplo, " +
                            "un enlace que empiece por http o https)."
                    _student.update {
                        it.copy(
                            step = OnbStep.LOGO,
                            title = "Logo del negocio (URL)",
                            subtitle = subtitleLogo,
                            hint = "https://…",
                            cta = "Continuar",
                            textValue = "",
                            canProceed = false,
                            errorMessage = null
                        )
                    }
                } else {
                    finalizeBuyer()
                }
            }
            OnbStep.LOGO -> {
                tmpLogoUrl = st.textValue
                viewModelScope.launch {
                    val cats = categoryService.listAll().getOrElse { emptyList() }
                    availableCats = cats
                    val names = cats.map { it.name }.sorted()

                    _student.update {
                        it.copy(
                            step = OnbStep.CATEGORIES,
                            title = "Categorías del negocio",
                            subtitle = formatCatsSubtitle(emptyList()),
                            hint = "Usa el ícono para seleccionar",
                            cta = "Finalizar",
                            textValue = "",
                            canProceed = false,
                            categories = names,
                            selectedCats = emptyList(),
                            errorMessage = null
                        )
                    }
                }
            }
            OnbStep.CATEGORIES -> {
                finalizeBusinessWithCategories(st.selectedCats)
            }
        }
    }

    fun student_onCategoriesPicked(selected: List<String>) {
        val limited = selected.take(3)
        _student.update {
            it.copy(
                selectedCats = limited,
                subtitle = formatCatsSubtitle(limited),
                canProceed = limited.isNotEmpty() && limited.size <= 3,
                errorMessage = null
            )
        }
    }

    private fun finalizeBusinessWithCategories(selectedNames: List<String>) {
        viewModelScope.launch {
            try {
                authService.completeOnboarding(tmpCode).getOrThrow()
                authService.updateBusinessAddressAndLogo(tmpAddress, tmpLogoUrl).getOrThrow()

                val uid = authService.currentUser().getOrNull()?.id
                    ?: error("No authenticated user")

                val chosen: List<Category> = selectedNames.mapNotNull { name ->
                    availableCats.firstOrNull { it.name == name }
                }.take(3)

                businessService.updateBusiness(uid, categories = chosen).getOrThrow()

                _student.update { it.copy(nav = AuthNavDestination.ToBusinessProfile) }
            } catch (e: Exception) {
                _student.update { it.copy(errorMessage = e.message ?: "No se pudo guardar") }
            }
        }
    }

    private fun formatCatsSubtitle(selected: List<String>): String =
        if (selected.isEmpty()) "Selecciona hasta 3 categorías."
        else "Seleccionadas: " + selected.joinToString(", ")

    private fun finalizeBuyer() {
        viewModelScope.launch {
            try {
                authService.completeOnboarding(tmpCode).getOrThrow()
                authService.updateBuyerAddress(tmpAddress).getOrThrow()
                _student.update { it.copy(nav = AuthNavDestination.ToBuyerHome) }
            } catch (e: Exception) {
                _student.update { it.copy(errorMessage = e.message ?: "No se pudo guardar") }
            }
        }
    }

    fun student_onCameraIconClicked() { _student.update { it.copy(requestOpenCamera = true) } }
    fun student_onCameraHandled()     { _student.update { it.copy(requestOpenCamera = false) } }
    fun student_onCameraResult(@Suppress("UNUSED_PARAMETER") bitmap: Bitmap?) { /* future use */ }

    fun student_clearNavAndErrors() {
        _student.update { it.copy(nav = AuthNavDestination.None, errorMessage = null) }
    }

    /* -----------------------------
       Utils
    ------------------------------ */
    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}