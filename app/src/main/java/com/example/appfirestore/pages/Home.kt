package com.example.appfirestore.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appfirestore.AuthViewModel


@Composable
fun Home(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.observeAsState()
    val userEmail by authViewModel.userEmail.observeAsState("")

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Unauthenticated) {
            navController.navigate("login") { popUpTo("home") { inclusive = true } }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Você está na Home", fontSize = 28.sp, color = Color(0xff0000b5))
                    Text("Email atual: $userEmail", fontSize = 18.sp)

                    Button(
                        onClick = { navController.navigate("ToDoList") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lista de Tarefas")
                    }


                    TextButton(
                        onClick = { authViewModel.signout() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout")
                    }

                    if (authState is AuthViewModel.AuthState.Error) {
                        Text(
                            text = (authState as AuthViewModel.AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                }
            }
        }
    }
}