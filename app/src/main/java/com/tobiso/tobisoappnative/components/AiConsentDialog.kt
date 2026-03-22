package com.tobiso.tobisoappnative.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.navigation.PostDetailRoute

/**
 * Souhlas s podmínkami použití a zásadami ochrany osobních údajů AI funkcí.
 * Zobrazí se pouze jednou – při prvním použití AI.
 *
 * @param navController  pro navigaci na PostDetail s podmínkami/zásadami
 * @param onAccepted     voláno, když uživatel souhlasí (ukládání do SharedPreferences je na volajícím)
 * @param onDismissed    voláno, když uživatel odmítne nebo dialog zavře
 */
@Composable
fun AiConsentDialog(
    navController: NavController?,
    onAccepted: () -> Unit,
    onDismissed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        icon = {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "AI asistent",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Před použitím AI asistenta potvrďte, že souhlasíte s:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Odkaz na zásady ochrany osobních údajů (post 330)
                TextButton(
                    onClick = {
                        onDismissed()
                        navController?.navigate(PostDetailRoute(postId = 330))
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("• ")
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append("Zásady ochrany osobních údajů")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Odkaz na podmínky použití (post 331)
                TextButton(
                    onClick = {
                        onDismissed()
                        navController?.navigate(PostDetailRoute(postId = 331))
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("• ")
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append("Podmínky použití")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "AI asistent zpracovává obsah otevřeného článku a tvůj dotaz za účelem poskytnutí odpovědi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccepted,
                shape = RoundedCornerShape(50)
            ) {
                Text("Souhlasím")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissed,
                shape = RoundedCornerShape(50)
            ) {
                Text("Odmítám")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
