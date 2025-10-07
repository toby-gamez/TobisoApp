package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.ShopManager
import com.example.tobisoappnative.data.ShopData
import com.example.tobisoappnative.model.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(navController: NavController) {
    val context = LocalContext.current
    val totalPoints by PointsManager.totalPoints.collectAsState()
    val purchasedItemIds by ShopManager.purchasedItems.collectAsState()
    val activeMultiplier by PointsManager.activeMultiplier.collectAsState()
    
    var selectedCategory by remember { mutableStateOf(ShopCategory.STREAK) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ShopItem?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showUsePowerUpDialog by remember { mutableStateOf(false) }
    
    // Inicializace ShopManageru
    LaunchedEffect(Unit) {
        ShopManager.init(context)
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        LargeTopAppBar(
            title = { Text("Obchod") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            },
            actions = {
                // Zobrazení aktivního multiplikátoru
                if (activeMultiplier > 1.0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Multiplikátor",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${activeMultiplier}x",
                            color = MaterialTheme.colorScheme.onTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Zobrazení bodů
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "Body",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = totalPoints.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        )
        
        // Kategorie tabs
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ShopCategory.values().toList()) { category ->
                CategoryChip(
                    category = category,
                    isSelected = category == selectedCategory,
                    onClick = { selectedCategory = category }
                )
            }
        }
        
        // Obsah kategorie
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val categoryItems = ShopData.getItemsByCategory(selectedCategory)
            
            items(categoryItems) { item ->
                ShopItemCard(
                    item = item,
                    isPurchased = purchasedItemIds.contains(item.id),
                    canAfford = totalPoints >= item.price,
                    isOnCooldown = ShopManager.isOnCooldown(context, item.id),
                    cooldownTimeLeft = ShopManager.getCooldownTimeLeft(context, item.id),
                    onClick = {
                        selectedItem = item
                        if (purchasedItemIds.contains(item.id) && item.type == ShopItemType.POINTS_MULTIPLIER) {
                            showUsePowerUpDialog = true
                        } else {
                            showPurchaseDialog = true
                        }
                    }
                )
            }
            
            // Spodní padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Purchase Dialog
    if (showPurchaseDialog && selectedItem != null) {
        PurchaseDialog(
            item = selectedItem!!,
            totalPoints = totalPoints,
            isPurchased = purchasedItemIds.contains(selectedItem!!.id),
            onConfirm = {
                val success = ShopManager.purchaseItem(context, selectedItem!!)
                if (success) {
                    showSuccessMessage = true
                } else {
                    errorMessage = "Nedostatek bodů pro nákup tohoto itemu!"
                    showErrorMessage = true
                }
                showPurchaseDialog = false
                selectedItem = null
            },
            onDismiss = {
                showPurchaseDialog = false
                selectedItem = null
            }
        )
    }
    
    // Use Power-Up Dialog
    if (showUsePowerUpDialog && selectedItem != null) {
        UsePowerUpDialog(
            item = selectedItem!!,
            isOnCooldown = ShopManager.isOnCooldown(context, selectedItem!!.id),
            cooldownTimeLeft = ShopManager.getCooldownTimeLeft(context, selectedItem!!.id),
            onConfirm = {
                val success = ShopManager.usePowerUp(context, selectedItem!!)
                if (success) {
                    showSuccessMessage = true
                } else {
                    if (ShopManager.isOnCooldown(context, selectedItem!!.id)) {
                        errorMessage = "Power-up je na cooldownu!"
                    } else {
                        errorMessage = "Chyba při aktivaci power-upu!"
                    }
                    showErrorMessage = true
                }
                showUsePowerUpDialog = false
                selectedItem = null
            },
            onDismiss = {
                showUsePowerUpDialog = false
                selectedItem = null
            }
        )
    }
    
    // Success Snackbar
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(2000)
            showSuccessMessage = false
        }
    }
    
    // Error Snackbar
    LaunchedEffect(showErrorMessage) {
        if (showErrorMessage) {
            delay(3000)
            showErrorMessage = false
        }
    }
    
    // Snackbar pro zprávy
    if (showSuccessMessage) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "✅ Úspěch!",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    if (showErrorMessage) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "❌ $errorMessage",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onError,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: ShopCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Text(
            text = category.displayName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    isPurchased: Boolean,
    canAfford: Boolean,
    isOnCooldown: Boolean = false,
    cooldownTimeLeft: Long = 0,
    onClick: () -> Unit
) {
    val containerColor = when {
        isPurchased -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        !canAfford -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPurchased) { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (item.quote != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${item.quote}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (isPurchased) {
                        if (item.type == ShopItemType.POINTS_MULTIPLIER) {
                            if (isOnCooldown) {
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Cooldown",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${cooldownTimeLeft}min",
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = "Cooldown",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onClick,
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Použít",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Zakoupeno",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Vlastníš",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "Body",
                                tint = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.price.toString(),
                                color = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (!canAfford) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Nedostatek bodů",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseDialog(
    item: ShopItem,
    totalPoints: Int,
    isPurchased: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPurchased) "Již vlastníš" else "Potvrdit nákup",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(text = item.description)
                
                if (item.quote != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${item.quote}\"",
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (!isPurchased) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cena:",
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "Body",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.price.toString(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tvoje body:",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = totalPoints.toString(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Po nákupu:",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = (totalPoints - item.price).toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (totalPoints >= item.price) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isPurchased) {
                Button(
                    onClick = onConfirm,
                    enabled = totalPoints >= item.price
                ) {
                    Text("Koupit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPurchased) "Zavřít" else "Zrušit")
            }
        }
    )
}

@Composable
fun UsePowerUpDialog(
    item: ShopItem,
    isOnCooldown: Boolean,
    cooldownTimeLeft: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isOnCooldown) "Power-up na cooldownu" else "Použít power-up",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(text = item.description)
                
                if (item.multiplier != null && item.durationMinutes != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Multiplikátor: ${item.multiplier}x",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Trvání: ${item.durationMinutes} minut",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                if (isOnCooldown) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Cooldown",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cooldown: ${cooldownTimeLeft} minut",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (item.cooldownMinutes != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cooldown po použití: ${item.cooldownMinutes} minut",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            if (!isOnCooldown) {
                Button(onClick = onConfirm) {
                    Text("Použít")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít")
            }
        }
    )
}