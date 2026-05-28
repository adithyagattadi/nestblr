package com.example.nestblr.core.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nestblr.feature.auth.AuthScreen
import com.example.nestblr.feature.auth.AuthViewModel
import com.example.nestblr.feature.auth.RoleGateScreen
import com.example.nestblr.feature.detail.DetailScreen
import com.example.nestblr.feature.owner.CreateListingScreen
import com.example.nestblr.feature.owner.OwnerHomeScreen
import com.example.nestblr.feature.search.SearchScreen

@Composable
fun NestBlrNavHost(
    isLoggedIn: Boolean
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Route.RoleGate else Route.Auth
    ) {
        composable<Route.Auth> {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Route.RoleGate) {
                        popUpTo(Route.Auth) { inclusive = true }
                    }
                }
            )
        }

        // Post-login gate: fetch role, route accordingly
        composable<Route.RoleGate> {
            RoleGateScreen(
                onResolved = { role ->
                    val dest = if (role == "OWNER") Route.OwnerHome else Route.Search
                    navController.navigate(dest) {
                        popUpTo(Route.RoleGate) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.Search> {
            // Get AuthViewModel scoped to this entry for sign-out
            val authViewModel: AuthViewModel = hiltViewModel()
            SearchScreen(
                onListingClick = { listingId ->
                    navController.navigate(Route.Detail(listingId))
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Route.Auth) {
                        popUpTo(0) { inclusive = true }  // clear entire back stack
                    }
                }
            )
        }

        composable<Route.Detail> {
            DetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.OwnerHome> {
            // Get AuthViewModel scoped to this entry for sign-out
            val authViewModel: AuthViewModel = hiltViewModel()
            OwnerHomeScreen(
                onCreateListing = { navController.navigate(Route.CreateListing) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Route.Auth) {
                        popUpTo(0) { inclusive = true }  // clear entire back stack
                    }
                }
            )
        }

        composable<Route.CreateListing> {
            CreateListingScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }  // back to owner home (it reloads)
            )
        }
    }
}