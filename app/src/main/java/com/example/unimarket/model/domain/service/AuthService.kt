package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.serviceAdapter.AuthServiceAdapter
import com.example.unimarket.model.data.serviceAdapter.BuyersServiceAdapter
import com.example.unimarket.model.data.serviceAdapter.BusinessesServiceAdapter
import com.example.unimarket.model.data.serviceAdapter.UsersServiceAdapter
import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.domain.entity.Address
import com.example.unimarket.model.domain.entity.Buyer
import com.example.unimarket.model.domain.entity.Business
import com.example.unimarket.model.domain.entity.Cart
import com.example.unimarket.model.domain.entity.User
import com.example.unimarket.model.domain.validation.Validators.requireEmail
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank
import com.example.unimarket.model.session.SessionManager
import com.example.unimarket.model.session.UserSession
import kotlinx.coroutines.tasks.await

class AuthService(
    private val authServiceAdapter: AuthServiceAdapter = AuthServiceAdapter(),
    private val buyersServiceAdapter: BuyersServiceAdapter = BuyersServiceAdapter(),
    private val businessesServiceAdapter: BusinessesServiceAdapter = BusinessesServiceAdapter(),
    private val usersServiceAdapter: UsersServiceAdapter = UsersServiceAdapter()
) {

    suspend fun signUp(
        user: User,
        password: String,

        businessName: String? = null,
        businessLogo: String? = null,
        businessAddress: Address? = null,

        buyerAddresses: List<Address>? = null
    ): Result<User> = runCatching {
        requireEmail(user.email)
        requireNotBlank(user.name, "name")
        requireNotBlank(user.idType, "idType")
        requireNotBlank(user.idNumber, "idNumber")
        require(user.type in listOf("buyer", "business")) { "type must be buyer|business" }
        require(password.length >= 6) { "password must be >= 6" }

        val created = authServiceAdapter.signUp(user, password).getOrThrow()

        when (created.type) {
            "buyer" -> {
                val buyer = Buyer(
                    address = buyerAddresses ?: emptyList(),
                    cart = Cart(products = emptyMap(), price = 0.0)
                )
                buyersServiceAdapter.create(created.id, buyer)
            }
            "business" -> {
                val name = businessName ?: error("businessName is required for business sign up")
                val logo = businessLogo ?: ""
                val addr = businessAddress ?: Address()

                val biz = Business(
                    name = name,
                    address = addr,
                    rating = 0.0,
                    products = emptyList(),
                    logo = logo
                )
                businessesServiceAdapter.create(created.id, biz)
            }
        }

        authServiceAdapter.signIn(user.email, password).getOrThrow()

        val fresh = SessionManager.ensureFreshIdToken(forceRefresh = true)

        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: created.id
        val email = FirebaseAuthProvider.auth.currentUser?.email ?: created.email
        val claimType = fresh?.type.orEmpty()
        val profileType = if (claimType.isNotBlank()) claimType else {
            usersServiceAdapter.getById(uid)?.type ?: created.type
        }

        if (fresh == null) {
            val token = FirebaseAuthProvider.auth.currentUser
                ?.getIdToken(false)?.await()?.token ?: ""
            SessionManager.setSession(
                UserSession(
                    uid = uid,
                    email = email,
                    type = profileType,
                    idToken = token
                )
            )
        } else if (fresh.type.isBlank() && profileType.isNotBlank()) {
            SessionManager.setSession(fresh.copy(type = profileType))
        }

        created
    }

    suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val user = authServiceAdapter.signIn(email, password).getOrThrow()

        val current = FirebaseAuthProvider.auth.currentUser ?: error("No authenticated user")
        if (!current.isEmailVerified) {
            FirebaseAuthProvider.auth.signOut()
            error("Debes verificar tu correo antes de iniciar sesión. Revisa tu bandeja y spam.")
        }

        val fresh = SessionManager.ensureFreshIdToken(forceRefresh = true)
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: user.id
        val claimType = fresh?.type.orEmpty()
        val profileType = if (claimType.isNotBlank()) claimType else {
            usersServiceAdapter.getById(uid)?.type ?: user.type
        }

        if (fresh == null) {
            val token = FirebaseAuthProvider.auth.currentUser
                ?.getIdToken(false)?.await()?.token ?: ""
            SessionManager.setSession(
                UserSession(
                    uid = uid,
                    email = user.email,
                    type = profileType,
                    idToken = token
                )
            )
        } else if (fresh.type.isBlank() && profileType.isNotBlank()) {
            SessionManager.setSession(fresh.copy(type = profileType))
        }
        user.copy(type = profileType)
    }

    fun signOut() {
        FirebaseAuthProvider.auth.signOut()
        SessionManager.clear()
    }

    suspend fun currentUser(): Result<User?> = runCatching {
        val firebaseUser = FirebaseAuthProvider.auth.currentUser ?: return@runCatching null
        val uid = firebaseUser.uid
        val u = usersServiceAdapter.getById(uid)

        SessionManager.ensureFreshIdToken(forceRefresh = false)
        u
    }

    suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        val u = FirebaseAuthProvider.auth.currentUser ?: error("No authenticated user")
        u.sendEmailVerification().await()
        Unit
    }

    suspend fun reloadCurrentUser(): Result<Unit> = runCatching {
        FirebaseAuthProvider.auth.currentUser?.reload()?.await()
        Unit
    }

    suspend fun isEmailVerified(): Result<Boolean> = runCatching {
        FirebaseAuthProvider.auth.currentUser?.reload()?.await()
        FirebaseAuthProvider.auth.currentUser?.isEmailVerified == true
    }

    suspend fun completeOnboarding(studentCode: String): Result<Unit> = runCatching {
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("No authenticated user")
        usersServiceAdapter.update(
            uid,
            mapOf(
                "studentCode" to studentCode,
                "onboardingCompleted" to true
            )
        )
    }

    suspend fun updateBuyerAddress(line: String) = runCatching {
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("No authenticated user")
        val address = mapOf(
            "direccion" to line,
            "edificio" to "",
            "piso" to "",
            "salon" to "",
            "local" to ""
        )
        buyersServiceAdapter.update(uid, mapOf("address" to listOf(address)))
    }   

    suspend fun updateBusinessAddressAndLogo(line: String, logoUrl: String) = runCatching {
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("No authenticated user")
        val address = mapOf(
            "direccion" to line,
            "edificio" to "",
            "piso" to "",
            "salon" to "",
            "local" to ""
        )
        businessesServiceAdapter.update(uid, mapOf("address" to address, "logo" to logoUrl))
    }
}