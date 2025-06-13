package com.example.cuatroenrayaOnline.auth

import com.google.firebase.auth.FirebaseAuth

class AnonymousAuth(private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()) {

    fun signInAnonymously(onComplete: (Boolean) -> Unit) {
        firebaseAuth.signInAnonymously()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    fun getCurrentUserUid(): String? {
        return firebaseAuth.currentUser?.uid
    }
}
