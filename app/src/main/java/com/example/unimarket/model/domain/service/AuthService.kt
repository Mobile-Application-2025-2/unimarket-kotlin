package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.dao.AuthDao
import com.example.unimarket.model.data.dao.BuyersDao
import com.example.unimarket.model.data.dao.BusinessesDao
import com.example.unimarket.model.data.dao.UsersDao
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
    private val authDao: AuthDao = AuthDao(),
    private val buyersDao: BuyersDao = BuyersDao(),
    private val businessesDao: BusinessesDao = BusinessesDao(),
    private val usersDao: UsersDao = UsersDao()
) {

    suspend fun signUp(
        user: User,
        password: String,

        businessName: String? = null,
        businessLogo: String? = null,
        businessAddress: Address? = null,

        buyerAddresses: List<Address>? = null
    ): Result<User> = runCatching {
        // 1) Validaciones base (users/)
        requireEmail(user.email)
        requireNotBlank(user.name, "name")
        requireNotBlank(user.idType, "idType")
        requireNotBlank(user.idNumber, "idNumber")
        require(user.type in listOf("buyer", "business")) { "type must be buyer|business" }
        require(password.length >= 6) { "password must be >= 6" }

        // 2) Crear credenciales + users/{uid}
        val created = authDao.signUp(user, password).getOrThrow()

        // 3) Inicializar colección por rol
        when (created.type) {
            "buyer" -> {
                val buyer = Buyer(
                    address = buyerAddresses ?: emptyList(),
                    cart = Cart(products = emptyList(), price = 0.0)
                )
                buyersDao.create(created.id, buyer)
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
                businessesDao.create(created.id, biz)
            }
        }

        // 4) Hacer login explícito
        authDao.signIn(user.email, password).getOrThrow()

        val fresh = SessionManager.ensureFreshIdToken(forceRefresh = true)

        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: created.id
        val email = FirebaseAuthProvider.auth.currentUser?.email ?: created.email
        val claimType = fresh?.type.orEmpty()
        val profileType = if (claimType.isNotBlank()) claimType else {
            usersDao.getById(uid)?.type ?: created.type
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
        val user = authDao.signIn(email, password).getOrThrow()

        val fresh = SessionManager.ensureFreshIdToken(forceRefresh = true)
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: user.id
        val claimType = fresh?.type.orEmpty()
        val profileType = if (claimType.isNotBlank()) claimType else {
            usersDao.getById(uid)?.type ?: user.type
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
        val u = usersDao.getById(uid)

        SessionManager.ensureFreshIdToken(forceRefresh = false)
        u
    }
}