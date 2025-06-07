package com.example.cuatroenrayaOnline.auth

import com.google.firebase.auth.FirebaseAuth

object AnonymousAuth {
    fun signInAnonymously(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener { result ->
                onSuccess(result.user?.uid ?: "")
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun getCurrentUserUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
}