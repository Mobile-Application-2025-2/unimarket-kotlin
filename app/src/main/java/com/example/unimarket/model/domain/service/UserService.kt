package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.dao.UsersDao
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank

class UserService(
    private val usersDao: UsersDao = UsersDao()
) {
    suspend fun getById(uid: String) = runCatching {
        requireNotBlank(uid, "uid")
        usersDao.getById(uid) ?: error("User not found")
    }

    suspend fun update(uid: String, partial: Map<String, Any?>) = runCatching {
        requireNotBlank(uid, "uid")
        usersDao.update(uid, partial)
    }

    suspend fun completeOnboarding(uid: String, studentCode: String) = runCatching {
        requireNotBlank(uid, "uid")
        requireNotBlank(studentCode, "studentCode")
        usersDao.update(
            uid,
            mapOf(
                "studentCode" to studentCode,
                "onboardingCompleted" to true
            )
        )
    }
}