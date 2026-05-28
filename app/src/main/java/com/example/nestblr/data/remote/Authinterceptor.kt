package com.example.nestblr.data.remote

import com.example.nestblr.data.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

/**
 * Attaches the Firebase ID token to every outgoing request as a Bearer header.
 *
 * Why runBlocking: OkHttp interceptors are synchronous, but getIdToken() is
 * suspend. OkHttp runs interceptors on a background thread (Dispatcher), so
 * blocking here does NOT block the main thread. This is the accepted pattern.
 *
 * Why Provider<AuthRepository>: avoids a Hilt dependency cycle
 * (NetworkModule needs the interceptor; the interceptor needs AuthRepository
 * which itself does not depend on the network, but Provider keeps it lazy
 * and safe).
 */
class AuthInterceptor @Inject constructor(
    private val authRepositoryProvider: Provider<AuthRepository>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Public endpoints (search, detail) don't strictly need a token,
        // but attaching it when available is harmless. Skip if not logged in.
        val token = runBlocking {
            runCatching { authRepositoryProvider.get().getIdToken() }.getOrNull()
        }

        val request = if (token != null) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}