package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.serviceAdapter.UsersServiceAdapter
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank

class UserService(
    private val usersServiceAdapter: UsersServiceAdapter = UsersServiceAdapter()
) {
    suspend fun getById(uid: String) = runCatching {
        requireNotBlank(uid, "uid")
        usersServiceAdapter.getById(uid) ?: error("User not found")
    }

    suspend fun update(uid: String, partial: Map<String, Any?>) = runCatching {
        requireNotBlank(uid, "uid")
        usersServiceAdapter.update(uid, partial)
    }

    suspend fun completeOnboarding(uid: String, studentCode: String) = runCatching {
        requireNotBlank(uid, "uid")
        requireNotBlank(studentCode, "studentCode")
        usersServiceAdapter.update(
            uid,
            mapOf(
                "studentCode" to studentCode,
                "onboardingCompleted" to true
            )
        )
    }
}