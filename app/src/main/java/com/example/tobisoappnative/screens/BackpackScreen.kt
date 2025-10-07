package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.tobisoappnative.BackpackManager
import com.example.tobisoappnative.model.*
import com.example.tobisoappnative.components.MultiplierIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackpackScreen(navController: NavController) {
    val context = LocalContext.current
    val backpackItems by BackpackManager.backpackItems.collectAsState()
    val equippedQuote by BackpackManager.equippedQuote.collectAsState()
    val equippedPet by BackpackManager.equippedPet.collectAsState()
    
    var selectedItem by remember { mutableStateOf<BackpackItem?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    // Pro scroll k sekcím
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val headerPositions = remember { mutableMapOf<BackpackCategory, Int>() }
    
    // Sledování aktivní kategorie
    val activeCategory by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val firstVisibleKey = visibleItems.first().key as? String
                when {
                    firstVisibleKey?.contains("QUOTES") == true -> BackpackCategory.QUOTES
                    firstVisibleKey?.contains("ICONS") == true -> BackpackCategory.ICONS
                    firstVisibleKey?.contains("PETS") == true -> BackpackCategory.PETS
                    firstVisibleKey?.contains("POWER_UPS") == true -> BackpackCategory.POWER_UPS
                    firstVisibleKey?.contains("STREAK_ITEMS") == true -> BackpackCategory.STREAK_ITEMS
                    else -> BackpackCategory.QUOTES
                }
            } else {
                BackpackCategory.QUOTES
            }
        }
    }
    
    // Inicializace BackpackManageru
    LaunchedEffect(Unit) {
        BackpackManager.init(context)
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        LargeTopAppBar(
            title = { Text("Aktovka") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            },
            actions = {
                MultiplierIndicator()
            }
        )
        
        if (backpackItems.isEmpty()) {
            // Prázdná aktovka
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkOff,
                        contentDescription = "Prázdná aktovka",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aktovka je prázdná",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nakup si něco v obchodě a objeví se to zde!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate("shop") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jít do obchodu")
                    }
                }
            }
        } else {
            // Kategorie navigace
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BackpackCategory.values().filter { category ->
                    BackpackManager.getItemsByCategory(category).isNotEmpty()
                }) { category ->
                    BackpackCategoryChip(
                        category = category,
                        isActive = category == activeCategory,
                        onClick = {
                            coroutineScope.launch {
                                val targetIndex = headerPositions[category]
                                if (targetIndex != null) {
                                    listState.animateScrollToItem(targetIndex)
                                }
                            }
                        }
                    )
                }
            }
            
            // Obsah aktovky
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var currentIndex = 0
                
                // Všechny kategorie pod sebou
                BackpackCategory.values().forEach { category ->
                    val categoryItems = BackpackManager.getItemsByCategory(category)
                    
                    if (categoryItems.isNotEmpty()) {
                        // Uloží pozici headeru
                        headerPositions[category] = currentIndex
                        
                        // Nadpis kategorie
                        item(key = "header_$category") {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                        currentIndex++
                        
                        // Položky kategorie
                        items(
                            items = categoryItems,
                            key = { item -> "item_${item.shopItem.id}" }
                        ) { backpackItem ->
                            BackpackItemCard(
                                backpackItem = backpackItem,
                                isEquipped = when (backpackItem.shopItem.type) {
                                    ShopItemType.PROFILE_QUOTE -> equippedQuote?.id == backpackItem.shopItem.id
                                    ShopItemType.PET -> equippedPet?.id == backpackItem.shopItem.id
                                    else -> false
                                },
                                onClick = {
                                    selectedItem = backpackItem
                                    showItemDialog = true
                                }
                            )
                        }
                        currentIndex += categoryItems.size
                        
                        // Mezera mezi kategoriemi
                        item(key = "spacer_$category") {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        currentIndex++
                    }
                }
                
                // Spodní padding
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    
    // Dialog pro zobrazení itemu
    if (showItemDialog && selectedItem != null) {
        BackpackItemDialog(
            backpackItem = selectedItem!!,
            isEquipped = when (selectedItem!!.shopItem.type) {
                ShopItemType.PROFILE_QUOTE -> equippedQuote?.id == selectedItem!!.shopItem.id
                ShopItemType.PET -> equippedPet?.id == selectedItem!!.shopItem.id
                else -> false
            },
            onEquip = {
                when (selectedItem!!.shopItem.type) {
                    ShopItemType.PROFILE_QUOTE -> {
                        BackpackManager.equipQuote(context, selectedItem!!.shopItem)
                        successMessage = "Citát byl vybaven!"
                    }
                    ShopItemType.PET -> {
                        BackpackManager.equipPet(context, selectedItem!!.shopItem)
                        successMessage = "Zvířátko bylo vybaveno!"
                    }
                    else -> {
                        successMessage = "Item byl aktivován!"
                    }
                }
                showSuccessMessage = true
                showItemDialog = false
            },
            onUnequip = {
                when (selectedItem!!.shopItem.type) {
                    ShopItemType.PROFILE_QUOTE -> {
                        BackpackManager.equipQuote(context, null)
                        successMessage = "Citát byl odstraněn"
                    }
                    ShopItemType.PET -> {
                        BackpackManager.equipPet(context, null)
                        successMessage = "Zvířátko bylo odstraněno"
                    }
                    else -> {
                        successMessage = "Item byl deaktivován"
                    }
                }
                showSuccessMessage = true
                showItemDialog = false
            },
            onDismiss = {
                showItemDialog = false
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
                    text = "✅ $successMessage",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BackpackCategoryChip(
    category: BackpackCategory,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Text(
            text = category.displayName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = contentColor,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun BackpackItemCard(
    backpackItem: BackpackItem,
    isEquipped: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isEquipped) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Pro zvířátka zobrazíme emoji ikonu nahoře
            if (backpackItem.shopItem.type == ShopItemType.PET && backpackItem.shopItem.petIcon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = backpackItem.shopItem.petIcon,
                        fontSize = 64.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = backpackItem.shopItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = backpackItem.shopItem.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (backpackItem.shopItem.quote != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${backpackItem.shopItem.quote}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (isEquipped) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Vybaveno",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vybaveno",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackpackItemDialog(
    backpackItem: BackpackItem,
    isEquipped: Boolean,
    onEquip: () -> Unit,
    onUnequip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = backpackItem.shopItem.name,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Pro zvířátka zobrazíme emoji ikonu
                if (backpackItem.shopItem.type == ShopItemType.PET && backpackItem.shopItem.petIcon != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = backpackItem.shopItem.petIcon,
                            fontSize = 64.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                Text(
                    text = backpackItem.shopItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(text = backpackItem.shopItem.description)
                
                if (backpackItem.shopItem.quote != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${backpackItem.shopItem.quote}\"",
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (isEquipped) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Vybaveno",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aktuálně vybaveno",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (backpackItem.shopItem.type == ShopItemType.PROFILE_QUOTE || 
                backpackItem.shopItem.type == ShopItemType.PET) {
                if (isEquipped) {
                    Button(onClick = onUnequip) {
                        Text("Odebrat")
                    }
                } else {
                    Button(onClick = onEquip) {
                        Text("Vybavit")
                    }
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