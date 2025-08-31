package com.example.appfirestore

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appfirestore.pages.Login
import com.example.appfirestore.pages.Cadastro
import com.example.appfirestore.pages.Home
import com.example.appfirestore.pages.ToDoList

@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "Cadastro", builder = {
        composable("Login") {
            Login(modifier, navController, authViewModel)
        }
        composable("Cadastro") {
            Cadastro(navController, authViewModel)
        }
        composable("Home") {
            Home(navController = navController, authViewModel = authViewModel)
        }
        composable("ToDoList") {
            ToDoList()
        }
    })
}