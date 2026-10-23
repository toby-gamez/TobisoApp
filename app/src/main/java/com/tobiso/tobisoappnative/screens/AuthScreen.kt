package com.tobiso.tobisoappnative.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import com.tobiso.tobisoappnative.components.Card
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.BuildConfig
import com.tobiso.tobisoappnative.repository.GoogleSignInResult
import com.tobiso.tobisoappnative.repository.requestGoogleIdToken
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import com.tobiso.tobisoappnative.viewmodel.auth.AuthEffect
import com.tobiso.tobisoappnative.viewmodel.auth.AuthIntent
import com.tobiso.tobisoappnative.viewmodel.auth.AuthMode
import com.tobiso.tobisoappnative.viewmodel.auth.AuthViewModel

/** Website origin derived from the API base URL (`https://host/api/` → `https://host`). */
private fun webUrl(path: String): String = BuildConfig.API_BASE_URL.removeSuffix("/").removeSuffix("/api") + path

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(navController: NavController) {
    val vm: AuthViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                AuthEffect.SignedIn -> {
                    val name = com.tobiso.tobisoappnative.model.AuthSession.user.value?.let { it.displayName.ifBlank { it.email.orEmpty() } }
                    android.widget.Toast.makeText(
                        context, if (name.isNullOrBlank()) "Přihlášeno" else "Přihlášen jako $name", android.widget.Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                }
            }
        }
    }

    fun openWeb(path: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl(path))))
    }

    if (state.googleTermsToken != null) {
        AlertDialog(
            onDismissRequest = { vm.onIntent(AuthIntent.DismissGoogleTerms) },
            title = { Text("Vytvořit účet Tobiso?") },
            text = {
                Column {
                    Text("K tomuto účtu Google ještě neexistuje účet Tobiso. Vytvořením účtu souhlasíš s Podmínkami použití a Zásadami ochrany osobních údajů.")
                    TextButton(onClick = { openWeb("/pravni-dokumenty") }) { Text("Zobrazit dokumenty") }
                }
            },
            confirmButton = { TextButton(onClick = { vm.onIntent(AuthIntent.AcceptGoogleTerms) }) { Text("Souhlasím a pokračovat") } },
            dismissButton = { TextButton(onClick = { vm.onIntent(AuthIntent.DismissGoogleTerms) }) { Text("Zrušit") } }
        )
    }


    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    if (state.mode == AuthMode.LOGIN) "Přihlášení" else "Registrace",
                    style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                }
            }
        )

        val pending = state.verificationEmail
        if (pending != null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Zkontroluj schránku", style = MaterialTheme.typography.headlineSmall)
                Text(
                    (if (state.justRegistered) "Poslali jsme ti e-mail s odkazem pro potvrzení účtu na "
                    else "Účet ještě není potvrzený. Poslali jsme ti znovu odkaz na ") + pending +
                        ". Po jeho otevření se můžeš přihlásit.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { vm.onIntent(AuthIntent.BackToLogin) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Přejít na přihlášení")
                }
            }
            return@Column
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Účet je volitelný – bez něj aplikace funguje dál jako dosud.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(shape = RoundedCornerShape(10.dp), 
                    selected = state.mode == AuthMode.LOGIN,
                    onClick = { vm.onIntent(AuthIntent.SetMode(AuthMode.LOGIN)) },
                    label = { Text("Přihlášení") }
                )
                FilterChip(shape = RoundedCornerShape(10.dp), 
                    selected = state.mode == AuthMode.REGISTER,
                    onClick = { vm.onIntent(AuthIntent.SetMode(AuthMode.REGISTER)) },
                    label = { Text("Registrace") }
                )
            }

            OutlinedTextField(shape = RoundedCornerShape(24.dp), 
                value = state.email,
                onValueChange = { vm.onIntent(AuthIntent.EmailChanged(it)) },
                label = { Text("E-mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            if (state.mode == AuthMode.REGISTER) {
                OutlinedTextField(shape = RoundedCornerShape(24.dp), 
                    value = state.displayName,
                    onValueChange = { vm.onIntent(AuthIntent.DisplayNameChanged(it)) },
                    label = { Text("Přezdívka (volitelné)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(shape = RoundedCornerShape(24.dp), 
                value = state.password,
                onValueChange = { vm.onIntent(AuthIntent.PasswordChanged(it)) },
                label = { Text("Heslo") },
                supportingText = if (state.mode == AuthMode.REGISTER) {
                    { Text("Alespoň ${AuthViewModel.MIN_PASSWORD_LENGTH} znaků") }
                } else null,
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showPassword) "Skrýt heslo" else "Zobrazit heslo"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (state.mode == AuthMode.REGISTER) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.acceptedTerms,
                        onCheckedChange = { vm.onIntent(AuthIntent.TermsChanged(it)) }
                    )
                    Column {
                        Text("Souhlasím s Podmínkami použití a Zásadami ochrany osobních údajů.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { openWeb("/pravni-dokumenty") }) { Text("Zobrazit dokumenty") }
                    }
                }
            }

            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = { vm.onIntent(AuthIntent.Submit) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (state.mode == AuthMode.LOGIN) "Přihlásit se" else "Vytvořit účet")
                }
            }
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            when (val r = requestGoogleIdToken(context, BuildConfig.GOOGLE_WEB_CLIENT_ID)) {
                                is GoogleSignInResult.Token -> vm.onIntent(AuthIntent.GoogleToken(r.idToken))
                                is GoogleSignInResult.Failed -> vm.onIntent(AuthIntent.GoogleFailed(r.message))
                                GoogleSignInResult.Cancelled -> vm.onIntent(
                                    AuthIntent.GoogleFailed("Přihlášení přes Google bylo zrušeno nebo ho Google odmítl. Zkus to znovu.")
                                )
                            }
                        }
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pokračovat přes Google") }
            }
            if (state.mode == AuthMode.LOGIN) {
                TextButton(onClick = { openWeb("/zapomenute-heslo") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Zapomenuté heslo")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
