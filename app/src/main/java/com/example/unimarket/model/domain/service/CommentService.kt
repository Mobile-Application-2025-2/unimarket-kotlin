package com.example.unimarket.model.domain.service

import com.example.unimarket.model.data.dao.CommentsDao
import com.example.unimarket.model.data.firebase.FirebaseAuthProvider
import com.example.unimarket.model.domain.entity.Comment
import com.example.unimarket.model.domain.validation.Validators.requireNotBlank

class CommentService(
    private val commentsDao: CommentsDao = CommentsDao()
) {

    suspend fun createComment(
        rating: Double,
        comment: String,
        productId: String,
        businessUid: String
    ): Result<String> = runCatching {
        require(rating in 0.0..5.0) { "rating must be 0..5" }
        requireNotBlank(comment, "comment")
        requireNotBlank(productId, "product")
        requireNotBlank(businessUid, "business")

        val uid = FirebaseAuthProvider.auth.currentUser?.uid ?: error("Not authenticated")

        commentsDao.create(
            Comment(
                rating = rating,
                comment = comment,
                user = uid,
                product = productId,
                business = businessUid
            )
        )
    }
}