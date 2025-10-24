package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.dao.BusinessesDao
import com.example.unimarket.model.data.dao.ProductsDao
import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.domain.entity.Product
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank

class ProductService(
    private val productsDao: ProductsDao = ProductsDao(),
    private val businessesDao: BusinessesDao = BusinessesDao()
) {
    /**
     * Crea un producto asegurando:
     *  - category existe
     *  - business == UID autenticado
     *  - businesses/{uid} existe (coincide con reglas)
     */
    suspend fun createProduct(p: Product): Result<String> = runCatching {
        requireNotBlank(p.name, "name")
        requireNotBlank(p.category, "category")
        require(p.price >= 0.0) { "price must be >= 0" }

        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        val businessUid = p.business.ifBlank { uid }
        require(businessUid == uid) { "You can only create products for your own business" }

        // Verifica que el perfil de business exista
        val biz = businessesDao.getById(businessUid) ?: error("Business profile not found")

        // Guardar con business correcto
        val toSave = p.copy(business = biz.id.ifBlank { businessUid })
        productsDao.create(toSave)
    }

    suspend fun updateProduct(id: String, p: Product): Result<Unit> = runCatching {
        requireNotBlank(id, "productId")
        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")
        require(p.business == uid) { "You can only update your own business products" }
        productsDao.update(id, p)
    }

    suspend fun deleteProduct(id: String): Result<Unit> = runCatching {
        requireNotBlank(id, "productId")
        productsDao.delete(id)
    }

    suspend fun getById(id: String) = runCatching {
        requireNotBlank(id, "productId")
        productsDao.getById(id) ?: error("Product not found")
    }

    suspend fun listByCategory(categoryId: String) = runCatching {
        requireNotBlank(categoryId, "categoryId")
        productsDao.listByCategory(categoryId)
    }

    suspend fun listByBusiness(businessUid: String) = runCatching {
        requireNotBlank(businessUid, "businessUid")
        productsDao.listByBusiness(businessUid)
    }
}