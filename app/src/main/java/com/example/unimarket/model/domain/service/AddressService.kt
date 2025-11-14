package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.dao.BuyersDao
import com.example.unimarket.model.data.dao.BusinessesDao
import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.domain.entity.Address
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank

class AddressService(
    private val buyersDao: BuyersDao = BuyersDao(),
    private val businessesDao: BusinessesDao = BusinessesDao()
) {
    suspend fun setBusinessAddress(address: Address): Result<Unit> = runCatching {
        validateAddress(address)
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        businessesDao.setAddress(uid, address)
    }

    suspend fun addBuyerAddress(address: Address): Result<Unit> = runCatching {
        validateAddress(address)
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        buyersDao.appendAddress(uid, address)
    }

    private fun validateAddress(a: Address) {
        requireNotBlank(a.direccion, "direccion")
        requireNotBlank(a.edificio, "edificio")
        requireNotBlank(a.piso, "piso")
        requireNotBlank(a.salon, "salon")
        requireNotBlank(a.local, "local")
    }
}