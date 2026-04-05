package com.kawaiipet.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.kawaiipet.app.util.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferenceManager
) : ViewModel() {
    val apiKey = prefs.apiKey
    val petName = prefs.petName
    val modelName = prefs.modelName
    val personalityPrompt = prefs.personalityPrompt

    suspend fun setApiKey(value: String) = prefs.setApiKey(value)
    suspend fun setPetName(value: String) = prefs.setPetName(value)
    suspend fun setModelName(value: String) = prefs.setModelName(value)
    suspend fun setPersonalityPrompt(value: String) = prefs.setPersonalityPrompt(value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState(initial = "")
    val petName by viewModel.petName.collectAsState(initial = "Mochi")
    val modelName by viewModel.modelName.collectAsState(initial = "gemini-1.5-flash")
    val personality by viewModel.personalityPrompt.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text("Gemini API Key", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { scope.launch { viewModel.setApiKey(it) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your Gemini API key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Pet Name", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = petName,
                onValueChange = { scope.launch { viewModel.setPetName(it) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Give your pet a name") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Gemini Model", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = modelName,
                onValueChange = { scope.launch { viewModel.setModelName(it) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. gemini-1.5-flash") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Pet Personality", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = personality,
                onValueChange = { scope.launch { viewModel.setPersonalityPrompt(it) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe your pet's personality") },
                minLines = 4,
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
