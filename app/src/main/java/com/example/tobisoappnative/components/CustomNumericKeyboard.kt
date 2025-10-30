package com.example.tobisoappnative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomNumericKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    // Symbol shown in place of the percent key; default is "%"
    alternateSymbol: String = "%"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            
            // První řada: 7, 8, 9, +, -
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeyboardButton("7", onKeyPress, Modifier.weight(1f))
                KeyboardButton("8", onKeyPress, Modifier.weight(1f))
                KeyboardButton("9", onKeyPress, Modifier.weight(1f))
                KeyboardButton("+", onKeyPress, Modifier.weight(1f), isOperator = true)
                KeyboardButton("-", onKeyPress, Modifier.weight(1f), isOperator = true)
            }
            
            // Druhá řada: 4, 5, 6, *, /
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeyboardButton("4", onKeyPress, Modifier.weight(1f))
                KeyboardButton("5", onKeyPress, Modifier.weight(1f))
                KeyboardButton("6", onKeyPress, Modifier.weight(1f))
                KeyboardButton("*", onKeyPress, Modifier.weight(1f), isOperator = true)
                KeyboardButton("/", onKeyPress, Modifier.weight(1f), isOperator = true)
            }
            
            // Třetí řada: 1, 2, 3, %, mezera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeyboardButton("1", onKeyPress, Modifier.weight(1f))
                KeyboardButton("2", onKeyPress, Modifier.weight(1f))
                KeyboardButton("3", onKeyPress, Modifier.weight(1f))
                KeyboardButton(alternateSymbol, onKeyPress, Modifier.weight(1f), isOperator = true)
                KeyboardButton("␣", onKeyPress, Modifier.weight(1f), actualValue = " ", isOperator = true)
            }
            
            // Čtvrtá řada: široká 0, čárka, široké backspace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WideKeyboardButton("0", onKeyPress, Modifier.weight(2f))
                KeyboardButton(",", onKeyPress, Modifier.weight(1f), isOperator = true)
                WideBackspaceButton(onBackspace, Modifier.weight(2f))
            }
    }
}

@Composable
private fun KeyboardButton(
    text: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
    actualValue: String = text,
    isOperator: Boolean = false
) {
    FilledTonalButton(
        onClick = { onKeyPress(actualValue) },
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isOperator) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isOperator) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = if (isOperator) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun BackspaceButton(
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onBackspace,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Backspace,
            contentDescription = "Smazat",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun WideKeyboardButton(
    text: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
    actualValue: String = text,
    isOperator: Boolean = false
) {
    FilledTonalButton(
        onClick = { onKeyPress(actualValue) },
        modifier = modifier.aspectRatio(2f), // Šířka 2x, výška 1x
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isOperator) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isOperator) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = if (isOperator) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun WideBackspaceButton(
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onBackspace,
        modifier = modifier.aspectRatio(2f), // Šířka 2x, výška 1x
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Backspace,
            contentDescription = "Smazat",
            modifier = Modifier.size(24.dp)
        )
    }
}