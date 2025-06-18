package com.example.cuatroenrayaOnline.auth

import com.google.firebase.auth.FirebaseAuth

// Handles anonymous sign-in using Firebase
class AnonymousAuth(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance() // Firebase auth instance
) {

    // Signs in anonymously and returns success/failure via callback
    fun signInAnonymously(onComplete: (Boolean) -> Unit) {
        firebaseAuth.signInAnonymously()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful) // true if sign-in was successful
            }
    }

    // Gets the UID of the current signed-in user, or null if not signed in
    fun getCurrentUserUid(): String? {
        return firebaseAuth.currentUser?.uid
    }
}
