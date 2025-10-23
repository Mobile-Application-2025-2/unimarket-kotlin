package com.example.unimarket.model.data.firebase

import com.google.firebase.auth.FirebaseAuth

object FirebaseAuthProvider {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
}