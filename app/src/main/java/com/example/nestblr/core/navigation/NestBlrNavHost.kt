package com.example.nestblr.core.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.nestblr.feature.auth.AuthScreen
import com.example.nestblr.feature.auth.AuthViewModel
import com.example.nestblr.feature.auth.ForgotPasswordScreen
import com.example.nestblr.feature.auth.RoleGateScreen
import com.example.nestblr.feature.detail.DetailScreen
import com.example.nestblr.feature.favorites.FavoritesScreen
import com.example.nestblr.feature.owner.CreateListingScreen
import com.example.nestblr.feature.owner.InquiriesScreen
import com.example.nestblr.feature.owner.ManagePhotosScreen
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
                },
                onForgotPasswordClick = { navController.navigate(Route.ForgotPassword) }
            )
        }

        composable<Route.ForgotPassword> {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
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
                onFavoritesClick = { navController.navigate(Route.Favorites) },
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

        composable<Route.Favorites> {
            FavoritesScreen(
                onListingClick = { listingId ->
                    navController.navigate(Route.Detail(listingId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.OwnerHome> {
            // Get AuthViewModel scoped to this entry for sign-out
            val authViewModel: AuthViewModel = hiltViewModel()
            OwnerHomeScreen(
                onCreateListing = { navController.navigate(Route.CreateListing) },
                onEditListing = { listingId ->
                    navController.navigate(Route.EditListing(listingId))
                },
                onManagePhotos = { listingId ->
                    navController.navigate(Route.ManageListingPhotos(listingId))
                },
                onInquiriesClick = { navController.navigate(Route.Inquiries) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Route.Auth) {
                        popUpTo(0) { inclusive = true }  // clear entire back stack
                    }
                }
            )
        }

        composable<Route.Inquiries> {
            InquiriesScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.CreateListing> {
            CreateListingScreen(
                onBack = { navController.popBackStack() },
                // After create, jump straight to photo management for the new listing,
                // removing CreateListing from the back stack so Back returns to owner home.
                onCreated = { newListingId ->
                    navController.navigate(Route.ManageListingPhotos(newListingId)) {
                        popUpTo(Route.CreateListing) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.EditListing> { backStackEntry ->
            val listingId = backStackEntry.toRoute<Route.EditListing>().listingId
            CreateListingScreen(
                isEditMode = true,
                onBack = { navController.popBackStack() },
                // After save, pop back to OwnerHome — it reloads on resume.
                onCreated = { navController.popBackStack() },
                onManagePhotos = {
                    navController.navigate(Route.ManageListingPhotos(listingId))
                }
            )
        }

        composable<Route.ManageListingPhotos> {
            ManagePhotosScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}