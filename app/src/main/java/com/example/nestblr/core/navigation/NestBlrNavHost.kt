package com.example.nestblr.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nestblr.feature.auth.AuthScreen
import com.example.nestblr.feature.detail.DetailScreen
import com.example.nestblr.feature.search.SearchScreen

@Composable
fun NestBlrNavHost(
    isLoggedIn: Boolean
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Route.Search else Route.Auth
    ) {
        composable<Route.Auth> {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Route.Search) {
                        popUpTo(Route.Auth) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.Search> {
            SearchScreen(
                onListingClick = { listingId ->
                    navController.navigate(Route.Detail(listingId))
                }
            )
        }

        composable<Route.Detail> {
            DetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}