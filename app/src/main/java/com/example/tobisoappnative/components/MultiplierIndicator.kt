package com.example.tobisoappnative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tobisoappnative.PointsManager
import kotlinx.coroutines.delay

@Composable
fun MultiplierIndicator() {
    val activeMultiplier by PointsManager.activeMultiplier.collectAsState()
    var showTimeLeft by remember { mutableStateOf(false) }
    var timeLeftSeconds by remember { mutableStateOf(0L) }
    
    // Automatické aktualizování zbývajícího času každou sekundu pro přesné zobrazení
    LaunchedEffect(activeMultiplier) {
        if (activeMultiplier > 1.0f) {
            while (activeMultiplier > 1.0f) {
                timeLeftSeconds = PointsManager.getMultiplierTimeLeftInSeconds()
                delay(1_000) // Aktualizace každou sekundu pro přesný čas
            }
        }
    }
    
    if (activeMultiplier > 1.0f) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(end = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { 
                    showTimeLeft = !showTimeLeft
                    if (showTimeLeft) {
                        timeLeftSeconds = PointsManager.getMultiplierTimeLeftInSeconds()
                    }
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Multiplikátor",
                tint = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            
            if (showTimeLeft) {
                val minutes = timeLeftSeconds / 60
                val seconds = timeLeftSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "${activeMultiplier}x",
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}