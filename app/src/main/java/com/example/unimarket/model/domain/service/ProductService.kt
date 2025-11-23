package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.serviceAdapter.BusinessesServiceAdapter
import com.example.unimarket.model.data.serviceAdapter.ProductsServiceAdapter
import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank

class ProductService(
    private val productsServiceAdapter: ProductsServiceAdapter = ProductsServiceAdapter(),
    private val businessesServiceAdapter: BusinessesServiceAdapter = BusinessesServiceAdapter()
) {
    suspend fun createProduct(p: Product): Result<String> = runCatching {
        requireNotBlank(p.name, "name")
        requireNotBlank(p.category, "category")
        require(p.price >= 0.0) { "price must be >= 0" }

        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        val businessUid = p.business.ifBlank { uid }
        require(businessUid == uid) { "You can only create products for your own business" }

        val biz = businessesServiceAdapter.getById(businessUid) ?: error("Business profile not found")

        val toSave = p.copy(business = biz.id.ifBlank { businessUid })
        productsServiceAdapter.create(toSave)
    }

    suspend fun updateProduct(id: String, p: Product): Result<Unit> = runCatching {
        requireNotBlank(id, "productId")
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        require(p.business == uid) { "You can only update your own business products" }
        productsServiceAdapter.update(id, p)
    }

    suspend fun deleteProduct(id: String): Result<Unit> = runCatching {
        requireNotBlank(id, "productId")
        productsServiceAdapter.delete(id)
    }

    suspend fun getById(id: String) = runCatching {
        requireNotBlank(id, "productId")
        productsServiceAdapter.getById(id) ?: error("Product not found")
    }

    suspend fun listByIds(ids: List<String>): Result<List<Product>> = runCatching {
        require(ids.isNotEmpty()) { "productIds must not be empty" }

        val result = mutableListOf<Product>()

        for (rawId in ids) {
            requireNotBlank(rawId, "productId")

            val p = productsServiceAdapter.getById(rawId)
            if (p != null) {
                result += p
            }
        }

        result.toList()
    }
}