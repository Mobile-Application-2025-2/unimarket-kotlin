package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.dao.CategoriesDao
import com.example.unimarket.model.domain.entity.Category
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank

class CategoryService(
    private val dao: CategoriesDao = CategoriesDao()
) {
    suspend fun listAll() = runCatching { dao.listAll() }
    suspend fun getById(id: String) = runCatching {
        requireNotBlank(id, "categoryId")
        dao.getById(id) ?: error("Category not found")
    }
    suspend fun create(c: Category) = runCatching {
        requireNotBlank(c.name, "name")
        dao.create(c)
    }
    suspend fun update(id: String, partial: Map<String, Any?>) = runCatching {
        requireNotBlank(id, "categoryId")
        dao.update(id, partial)
    }
}