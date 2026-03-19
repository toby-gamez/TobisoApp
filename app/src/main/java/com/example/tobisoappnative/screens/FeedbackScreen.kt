package com.example.tobisoappnative.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.tooling.preview.Preview
import com.example.tobisoappnative.viewmodel.feedback.FeedbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    navController: NavController,
    vm: FeedbackViewModel = hiltViewModel()
) {
    val name by vm.name.collectAsState()
    val email by vm.email.collectAsState()
    val message by vm.message.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val isSuccess by vm.isSuccess.collectAsState()
    val isError by vm.isError.collectAsState()

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
                    onValueChange = { vm.onNameChange(it) },
                    label = { Text("Vaše jméno") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { vm.onEmailChange(it) },
                    label = { Text("Váš e-mail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { vm.onMessageChange(it) },
                    label = { Text("Zde napište svou zpětnou vazbu") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { vm.sendFeedback() },
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
