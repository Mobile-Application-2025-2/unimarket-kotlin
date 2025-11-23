package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.serviceAdapter.BuyersServiceAdapter
import com.example.unimarket.model.data.serviceAdapter.BusinessesServiceAdapter
import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.domain.entity.Address
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank

class AddressService(
    private val buyersServiceAdapter: BuyersServiceAdapter = BuyersServiceAdapter(),
    private val businessesServiceAdapter: BusinessesServiceAdapter = BusinessesServiceAdapter()
) {
    suspend fun setBusinessAddress(address: Address): Result<Unit> = runCatching {
        validateAddress(address)
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        businessesServiceAdapter.setAddress(uid, address)
    }

    suspend fun addBuyerAddress(address: Address): Result<Unit> = runCatching {
        validateAddress(address)
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        buyersServiceAdapter.appendAddress(uid, address)
    }

    private fun validateAddress(a: Address) {
        requireNotBlank(a.direccion, "direccion")
        requireNotBlank(a.edificio, "edificio")
        requireNotBlank(a.piso, "piso")
        requireNotBlank(a.salon, "salon")
        requireNotBlank(a.local, "local")
    }
}