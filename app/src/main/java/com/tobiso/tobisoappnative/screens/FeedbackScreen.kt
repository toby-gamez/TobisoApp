package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.components.IconButton

import com.tobiso.tobisoappnative.components.Button

import androidx.compose.foundation.shape.RoundedCornerShape
import com.tobiso.tobisoappnative.components.Card
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.foundation.verticalScroll
import com.tobiso.tobisoappnative.model.FeedbackType
import com.tobiso.tobisoappnative.viewmodel.feedback.FeedbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    navController: NavController,
    vm: FeedbackViewModel = hiltViewModel()
) {
    val name by vm.name.collectAsStateWithLifecycle()
    val email by vm.email.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()
    val type by vm.type.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val isSuccess by vm.isSuccess.collectAsStateWithLifecycle()
    val isError by vm.isError.collectAsStateWithLifecycle()

    // ✅ Odstraněn Scaffold - padding se aplikuje z MainActivity
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Zpětná vazba", style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
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
            Text("Našel jsi chybu nebo máš nápad? Napiš mi - čtu všechno ☺.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            if (isSuccess) {
                Text("Děkujeme! Vaše zpětná vazba byla úspěšně odeslána.", color = MaterialTheme.colorScheme.primary)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == FeedbackType.Bug,
                        onClick = { vm.onTypeChange(FeedbackType.Bug) },
                        label = { Text("Chyba v obsahu") }
                    )
                    FilterChip(
                        selected = type == FeedbackType.FeatureRequest,
                        onClick = { vm.onTypeChange(FeedbackType.FeatureRequest) },
                        label = { Text("Návrh na vylepšení") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(shape = RoundedCornerShape(24.dp), 
                    value = name,
                    onValueChange = { vm.onNameChange(it) },
                    label = { Text("Vaše jméno") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(shape = RoundedCornerShape(24.dp), 
                    value = email,
                    onValueChange = { vm.onEmailChange(it) },
                    label = { Text("Váš e-mail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(shape = RoundedCornerShape(24.dp), 
                    value = title,
                    onValueChange = { vm.onTitleChange(it) },
                    label = { Text("Nadpis") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(shape = RoundedCornerShape(24.dp), 
                    value = message,
                    onValueChange = { vm.onMessageChange(it) },
                    label = { Text("Zde napište svou zpětnou vazbu") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { vm.sendFeedback() },
                    enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && title.isNotBlank() && message.isNotBlank(),
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

