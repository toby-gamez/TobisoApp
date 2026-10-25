package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.components.IconButton

import com.tobiso.tobisoappnative.components.TextButton

import com.tobiso.tobisoappnative.components.OutlinedButton

import com.tobiso.tobisoappnative.components.Button

import androidx.compose.foundation.shape.RoundedCornerShape
import com.tobiso.tobisoappnative.components.Card
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.tobiso.tobisoappnative.R
import com.tobiso.tobisoappnative.components.CozySwap
import com.tobiso.tobisoappnative.components.CozyTabs
import com.tobiso.tobisoappnative.components.cozyEntrance
import com.tobiso.tobisoappnative.components.cozyPress
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

        val isLogin = state.mode == AuthMode.LOGIN
        val fieldShape = RoundedCornerShape(20.dp)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Image(
                painter = painterResource(if (isSystemInDarkTheme()) R.drawable.logo_dark else R.drawable.logo_light),
                contentDescription = null,
                modifier = Modifier.size(84.dp).cozyEntrance("auth-logo")
            )
            CozySwap(isLogin) { login ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (login) "Vítej zpět" else "Pojď mezi nás",
                        fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp
                    )
                    Text(
                        "Účet je volitelný – bez něj aplikace funguje dál jako dosud.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            CozyTabs(
                labels = listOf("Přihlášení", "Registrace"),
                selectedIndex = if (isLogin) 0 else 1,
                onSelect = { vm.onIntent(AuthIntent.SetMode(if (it == 0) AuthMode.LOGIN else AuthMode.REGISTER)) }
            )

            Column(
                Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    shape = fieldShape,
                    value = state.email,
                    onValueChange = { vm.onIntent(AuthIntent.EmailChanged(it)) },
                    label = { Text("E-mail") },
                    leadingIcon = { Icon(Icons.Rounded.Email, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(
                    visible = !isLogin,
                    enter = fadeIn() + expandVertically(spring(dampingRatio = 0.8f)),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        shape = fieldShape,
                        value = state.displayName,
                        onValueChange = { vm.onIntent(AuthIntent.DisplayNameChanged(it)) },
                        label = { Text("Přezdívka (volitelné)") },
                        leadingIcon = { Icon(Icons.Rounded.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    shape = fieldShape,
                    value = state.password,
                    onValueChange = { vm.onIntent(AuthIntent.PasswordChanged(it)) },
                    label = { Text("Heslo") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                    supportingText = if (!isLogin) {
                        { Text("Alespoň ${AuthViewModel.MIN_PASSWORD_LENGTH} znaků") }
                    } else null,
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            CozySwap(showPassword) { shown ->
                                Icon(
                                    if (shown) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (shown) "Skrýt heslo" else "Zobrazit heslo"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(
                    visible = !isLogin,
                    enter = fadeIn() + expandVertically(spring(dampingRatio = 0.8f)),
                    exit = fadeOut() + shrinkVertically()
                ) {
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
                AnimatedVisibility(
                    visible = state.error != null,
                    enter = fadeIn() + expandVertically(spring(dampingRatio = 0.7f)),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().cozyEntrance(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            state.error.orEmpty(),
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Button(
                onClick = { vm.onIntent(AuthIntent.Submit) },
                enabled = !state.isLoading,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(54.dp).cozyPress(0.97f)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isLogin) "Přihlásit se" else "Vytvořit účet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text("nebo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }
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
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(54.dp).cozyPress(0.97f)
                ) {
                    Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Pokračovat přes Google", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
            AnimatedVisibility(visible = isLogin) {
                TextButton(onClick = { openWeb("/zapomenute-heslo") }) {
                    Text("Zapomenuté heslo")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
