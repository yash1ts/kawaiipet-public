package com.kawaiipet.app.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kawaiipet.app.R
import com.kawaiipet.app.ui.auth.AuthEmailViewModel
import com.kawaiipet.app.ui.auth.AuthFormErrorBanner
import com.kawaiipet.app.ui.auth.toAuthUserMessage
import com.kawaiipet.app.ui.navigation.AuthRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    onAuthenticated: () -> Unit,
    viewModel: AuthEmailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resources = context.resources
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_signup_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.auth_signup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                    formError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                enabled = !busy,
                isError = emailError != null,
                supportingText = emailError?.let { msg ->
                    { Text(msg, color = MaterialTheme.colorScheme.error) }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                    formError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !busy,
                isError = passwordError != null,
                supportingText = passwordError?.let { msg ->
                    { Text(msg, color = MaterialTheme.colorScheme.error) }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmError = null
                    formError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.confirm_password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !busy,
                isError = confirmError != null,
                supportingText = confirmError?.let { msg ->
                    { Text(msg, color = MaterialTheme.colorScheme.error) }
                },
            )
            Spacer(modifier = Modifier.height(20.dp))

            AuthFormErrorBanner(message = formError)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    formError = null
                    emailError = null
                    passwordError = null
                    confirmError = null

                    when {
                        email.isBlank() -> {
                            emailError = context.getString(R.string.auth_error_required_email)
                        }
                        password.isBlank() -> {
                            passwordError = context.getString(R.string.auth_error_required_password)
                        }
                        confirmPassword.isBlank() -> {
                            confirmError = context.getString(R.string.auth_error_required_confirm)
                        }
                        password != confirmPassword -> {
                            confirmError = context.getString(R.string.auth_error_password_mismatch)
                        }
                        password.length < 6 -> {
                            passwordError = context.getString(R.string.auth_error_password_too_short)
                        }
                        else -> {
                            busy = true
                            viewModel.signUp(email, password) { result ->
                                busy = false
                                result.fold(
                                    onSuccess = { onAuthenticated() },
                                    onFailure = {
                                        formError = it.toAuthUserMessage(resources)
                                    },
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                enabled = !busy,
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.auth_action_create_account))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = {
                    navController.navigate(AuthRoutes.LOGIN) {
                        popUpTo(AuthRoutes.SIGN_UP) { inclusive = true }
                    }
                },
            ) {
                Text(stringResource(R.string.auth_switch_to_login))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
