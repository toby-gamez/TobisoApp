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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomNumericKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // První řada: 7, 8, 9, +, -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyboardButton("1", onKeyPress, Modifier.weight(1f))
            KeyboardButton("2", onKeyPress, Modifier.weight(1f))
            KeyboardButton("3", onKeyPress, Modifier.weight(1f))
            KeyboardButton("%", onKeyPress, Modifier.weight(1f), isOperator = true)
            KeyboardButton("␣", onKeyPress, Modifier.weight(1f), actualValue = " ", isOperator = true) // Mezera
        }
        
        // Čtvrtá řada: 0, desetinná čárka, backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            KeyboardButton("0", onKeyPress, Modifier.weight(1f))
            KeyboardButton(",", onKeyPress, Modifier.weight(1f), isOperator = true)
            BackspaceButton(onBackspace, Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
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
    Button(
        onClick = { onKeyPress(actualValue) },
        modifier = modifier.aspectRatio(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOperator) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (isOperator) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BackspaceButton(
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onBackspace,
        modifier = modifier.aspectRatio(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Backspace,
            contentDescription = "Smazat"
        )
    }
}