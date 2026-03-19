package com.tobiso.tobisoappnative.components

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Hook pro použití standardního Android Toast systému
 */
@Composable
fun ToastHandler(
    toastMessage: String?,
    onClearToast: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onClearToast()
        }
    }
}