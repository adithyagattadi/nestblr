package com.example.nestblr.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Firebase Auth for email/password.
 * Exposes suspend functions returning Result for clean error handling.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    val currentEmail: String?
        get() = firebaseAuth.currentUser?.email

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        Unit
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    /**
     * Sends a Firebase password-reset email. Firebase returns success even when
     * the email isn't registered (intentional, prevents account enumeration), so
     * callers must not branch on whether the address exists.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Unit
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Gets a fresh Firebase ID token to send to our backend.
     * The token auto-refreshes; forceRefresh=false uses cached if valid.
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        val user = firebaseAuth.currentUser ?: return null
        return user.getIdToken(forceRefresh).await().token
    }
}