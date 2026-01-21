package com.example.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.NavController
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.FeedbackDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun resetStates() {
        isSuccess = false
        isError = false
    }

    suspend fun sendFeedback(name: String, email: String, message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val feedbackDto = FeedbackDto(
                    name = name,
                    email = email,
                    message = message,
                    platform = "Aplikace"
                )
                ApiClient.apiService.sendFeedback(feedbackDto)
                true
            } catch (e: Exception) {
                android.util.Log.e("FeedbackScreen", "Error sending feedback", e)
                false
            }
        }
    }

    // ✅ Odstraněn Scaffold - padding se aplikuje z MainActivity
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Zpětná vazba", style = MaterialTheme.typography.headlineLarge) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            }
        )

        // Přidán scroll pomocí verticalScroll a rememberScrollState
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pokud najdete jakoukoli chybu v textu, můžete mi to tady poslat, pokud byste měli otázku, také mi to sem napište. Velmi mě zajímá vaše zpětná vazba ☺.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            if (isSuccess) {
                Text("Děkujeme! Vaše zpětná vazba byla úspěšně odeslána.", color = MaterialTheme.colorScheme.primary)
            } else {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        resetStates()
                    },
                    label = { Text("Vaše jméno") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it.trim()
                        resetStates()
                    },
                    label = { Text("Váš e-mail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        message = it
                        resetStates()
                    },
                    label = { Text("Zde napište svou zpětnou vazbu") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        resetStates()
                        isLoading = true
                        isError = false
                        isSuccess = false
                        coroutineScope.launch {
                            val result = sendFeedback(name, email, message)
                            isLoading = false
                            if (result) {
                                isSuccess = true
                            } else {
                                isError = true
                            }
                        }
                    },
                    enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isLoading) "Odesílání..." else "Odeslat zpětnou vazbu")
                }
                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Při odesílání došlo k chybě. Prosím zkuste to znovu.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeedbackScreenPreview() {
    // FeedbackScreen(navController = rememberNavController()) // Uncomment when NavController is available
}
