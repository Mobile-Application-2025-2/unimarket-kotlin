package com.example.unimarket.model.repository

import com.example.unimarket.model.entity.Category


interface ExploreRepository {
    suspend fun fetchAllCategories(): List<Category>
}
