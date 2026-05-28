package com.example.nestblr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.nestblr.core.navigation.NestBlrNavHost
import com.example.nestblr.data.auth.AuthRepository
import com.example.nestblr.ui.theme.NestBLRTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val loggedIn = authRepository.isLoggedIn

        setContent {
            NestBLRTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NestBlrNavHost(isLoggedIn = loggedIn)
                }
            }
        }
    }
}