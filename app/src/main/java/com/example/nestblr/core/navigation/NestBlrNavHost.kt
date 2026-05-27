package com.example.nestblr.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nestblr.feature.detail.DetailScreen
import com.example.nestblr.feature.search.SearchScreen

@Composable
fun NestBlrNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Search
    ) {
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