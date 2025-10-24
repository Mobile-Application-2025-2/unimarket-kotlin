package com.example.unimarket.model.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreProvider {
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    const val COL_USERS = "users"
    const val COL_BUYERS = "buyers"
    const val COL_BUSINESSES = "businesses"
    const val COL_CATEGORIES = "categories"
    const val COL_PRODUCTS = "products"
    const val COL_COMMENTS = "comments"

    fun users(): CollectionReference = db.collection(COL_USERS)
    fun buyers(): CollectionReference = db.collection(COL_BUYERS)
    fun businesses(): CollectionReference = db.collection(COL_BUSINESSES)
    fun categories(): CollectionReference = db.collection(COL_CATEGORIES)
    fun products(): CollectionReference = db.collection(COL_PRODUCTS)
    fun comments(): CollectionReference = db.collection(COL_COMMENTS)
}