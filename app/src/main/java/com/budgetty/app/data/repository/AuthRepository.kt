package com.budgetty.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around [FirebaseAuth]. Exposes the current user as a [Flow] and
 * suspend functions for the email/password and Google sign-in flows.
 */
class AuthRepository(private val auth: FirebaseAuth) {

    /** Emits the signed-in user (or null) and updates whenever auth state changes. */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUpEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    suspend fun signInEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    /** Sends a password-reset email to [email]. */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() = auth.signOut()

    /**
     * Permanently deletes the signed-in Firebase user. Propagates
     * [com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException] when the last sign-in is
     * too old, so the caller can prompt the user to re-authenticate and retry.
     */
    suspend fun deleteAccount() {
        val user = auth.currentUser ?: error("No signed-in user to delete")
        user.delete().await()
    }
}
