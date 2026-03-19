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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.ShopManager
import com.example.tobisoappnative.StreakFreezeManager
import com.example.tobisoappnative.data.ShopData
import com.example.tobisoappnative.components.MultiplierIndicator
import com.example.tobisoappnative.model.*
import com.example.tobisoappnative.IconPackManager
import com.example.tobisoappnative.viewmodel.shop.ShopViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    navController: NavController,
    vm: ShopViewModel = hiltViewModel()
) {
    val totalPoints by PointsManager.totalPoints.collectAsState()
    val purchasedItemIds by ShopManager.purchasedItems.collectAsState()
    val activeMultiplier by PointsManager.activeMultiplier.collectAsState()
    val availableFreezes by StreakFreezeManager.availableFreezes.collectAsState()

    val selectedItem by vm.selectedItem.collectAsState()
    val showPurchaseDialog by vm.showPurchaseDialog.collectAsState()
    val showUsePowerUpDialog by vm.showUsePowerUpDialog.collectAsState()
    val showSuccessMessage by vm.showSuccessMessage.collectAsState()
    val showErrorMessage by vm.showErrorMessage.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()

    // Pro scroll k sekcím - trackování pozic nadpisů
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val headerPositions = remember { mutableMapOf<ShopCategory, Int>() }

    // Sledování aktivní kategorie na základě scroll pozice - jednoduše!
    val activeCategory by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val firstIndex = if (visibleItems.isNotEmpty()) visibleItems.first().index else 0

            // Použij uložené pozice headerů a najdi nejbližší
            val sortedPositions = headerPositions.toList().sortedBy { it.second }

            for (i in sortedPositions.indices.reversed()) {
                if (firstIndex >= sortedPositions[i].second) {
                    return@derivedStateOf sortedPositions[i].first
                }
            }

            // Default
            ShopCategory.STREAK
        }
    }


    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Obchod", style = MaterialTheme.typography.headlineLarge) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            },
            actions = {
                // Ikona aktovky
                IconButton(
                    onClick = { navController.navigate("backpack") }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Backpack,
                        contentDescription = "Aktovka",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Zobrazení aktivního multiplikátoru
                MultiplierIndicator()
                
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
        
        // Navigační kategorie (pouze pro scroll)
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ShopCategory.values().toList()) { category ->
                CategoryNavigationChip(
                    category = category,
                    isActive = category == activeCategory,
                    onClick = {
                        coroutineScope.launch {
                            // Použij uložený index pozice pro danou kategorii
                            val targetIndex = headerPositions[category]
                            println("Scrolling to category: $category, targetIndex: $targetIndex")
                            
                            if (targetIndex != null) {
                                try {
                                    listState.animateScrollToItem(
                                        index = targetIndex,
                                        scrollOffset = 0 // Scroll přesně na začátek položky
                                    )
                                } catch (e: Exception) {
                                    println("Error scrolling: $e")
                                    // Fallback
                                    listState.scrollToItem(targetIndex)
                                }
                            } else {
                                println("No saved position, calculating...")
                                // Dynamic calculation fallback
                                val categories = ShopCategory.values()
                                var calculatedIndex = 0
                                for (cat in categories) {
                                    if (cat == category) break
                                    calculatedIndex++ // header
                                    calculatedIndex += ShopData.getItemsByCategory(cat).size // items
                                    if (cat != categories.last()) calculatedIndex++ // spacer
                                }
                                println("Calculated index: $calculatedIndex")
                                listState.animateScrollToItem(calculatedIndex, scrollOffset = 0)
                            }
                        }
                    }
                )
            }
        }
        
        // Obsah - všechny kategorie pod sebou s LazyColumn
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            var currentIndex = 0
            
            // Všechny kategorie pod sebou
            ShopCategory.values().forEachIndexed { categoryIndex, category ->
                    // Uloží pozici headeru pro navigaci
                    headerPositions[category] = currentIndex
                    println("Saved header position for $category at index: $currentIndex")
                    
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
                val categoryItems = ShopData.getItemsByCategory(category)
                items(
                    items = categoryItems,
                    key = { item -> "item_${item.id}" }
                ) { item ->
                    ShopItemCard(
                        item = item,
                        isPurchased = when (item.type) {
                            ShopItemType.STREAK_FREEZE -> !ShopManager.canPurchaseStreakFreeze()
                            else -> purchasedItemIds.contains(item.id)
                        },
                        canAfford = totalPoints >= item.price,
                        isOnCooldown = ShopManager.isOnCooldown(item.id),
                        cooldownTimeLeft = ShopManager.getCooldownTimeLeft(item.id),
                        onClick = {
                            vm.selectItem(item, purchasedItemIds)
                        }
                    )
                }
                currentIndex += categoryItems.size
                
                // Mezera mezi kategoriemi (kromě poslední)
                if (categoryIndex < ShopCategory.values().size - 1) {
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
    
    // Purchase Dialog
    if (showPurchaseDialog && selectedItem != null) {
        PurchaseDialog(
            item = selectedItem!!,
            totalPoints = totalPoints,
            isPurchased = purchasedItemIds.contains(selectedItem!!.id),
            onConfirm = { vm.confirmPurchase() },
            onDismiss = { vm.dismissPurchaseDialog() }
        )
    }

    // Use Power-Up Dialog
    if (showUsePowerUpDialog && selectedItem != null) {
        UsePowerUpDialog(
            item = selectedItem!!,
            isOnCooldown = ShopManager.isOnCooldown(selectedItem!!.id),
            cooldownTimeLeft = ShopManager.getCooldownTimeLeft(selectedItem!!.id),
            onConfirm = { vm.confirmUsePowerUp() },
            onDismiss = { vm.dismissUsePowerUpDialog() }
        )
    }

    // Success Snackbar
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(2000)
            vm.clearSuccessMessage()
        }
    }

    // Error Snackbar
    LaunchedEffect(showErrorMessage) {
        if (showErrorMessage) {
            delay(3000)
            vm.clearErrorMessage()
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
fun CategoryNavigationChip(
    category: ShopCategory,
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
    
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = containerColor
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
        isOnCooldown && item.type == ShopItemType.POINTS_MULTIPLIER -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        !canAfford -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val isClickable = !isPurchased && !(isOnCooldown && item.type == ShopItemType.POINTS_MULTIPLIER)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable) { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Pro zvířátka zobrazíme emoji ikonu nahoře
            if (item.type == ShopItemType.PET && item.petIcon != null) {
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
                        text = item.petIcon,
                        fontSize = 64.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Pro power-upy zobrazíme barevný text multiplikátoru
            if (item.type == ShopItemType.POINTS_MULTIPLIER && item.powerUpIcon != null) {
                val powerUpColor = when (item.multiplier) {
                    1.5f -> Color(0xFF00BCD4) // Aqua
                    2.0f -> Color(0xFFFFD700) // Zlatá
                    3.0f -> Color(0xFF9C27B0) // Fialová
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            powerUpColor.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.powerUpIcon,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = powerUpColor
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Pro balíčky ikon zobrazíme preview ikon
            if (item.type == ShopItemType.ICON_PACK && item.subjectIcons != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        // Grid layout pro 3 řádky po 3 ikonách
                        val visibleIcons = item.subjectIcons.take(6)
                        val rows = visibleIcons.chunked(3)
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rows.forEach { rowIcons ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowIcons.forEach { subjectIcon ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (item.iconPackType == IconPackType.EMOJI) {
                                                    Text(
                                                        text = subjectIcon.icon,
                                                        fontSize = 20.sp,
                                                        maxLines = 1
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = when (subjectIcon.icon) {
                                                            "edit" -> Icons.Default.Edit
                                                            "library_books" -> Icons.Default.LibraryBooks
                                                            "article" -> Icons.Default.Article
                                                            "music_note" -> Icons.Default.MusicNote
                                                            "functions" -> Icons.Default.Functions
                                                            "biotech" -> Icons.Default.Biotech
                                                            "bolt" -> Icons.Default.Bolt
                                                            "local_florist" -> Icons.Default.LocalFlorist
                                                            "language" -> Icons.Default.Language
                                                            // Přidáno mapování pro klasické ikony
                                                            "spellcheck" -> Icons.Default.Spellcheck
                                                            "menu_book" -> Icons.Default.MenuBook
                                                            "description" -> Icons.Default.Description
                                                            "library_music" -> Icons.Default.LibraryMusic
                                                            "calculate" -> Icons.Default.Calculate
                                                            "science" -> Icons.Default.Science
                                                            "precision_manufacturing" -> Icons.Default.PrecisionManufacturing
                                                            "eco" -> Icons.Default.Eco
                                                            "public" -> Icons.Default.Public
                                                            else -> Icons.Default.Book
                                                        },
                                                        contentDescription = subjectIcon.subjectName,
                                                        tint = getSubjectColorByName(subjectIcon.subjectName),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = subjectIcon.subjectName.take(4),
                                                fontSize = 9.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    
                                    // Vyplnění prázdných sloupců
                                    repeat(3 - rowIcons.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            
                            // Zobrazení počtu dalších ikon
                            if (item.subjectIcons.size > 6) {
                                Text(
                                    text = "...a dalších ${item.subjectIcons.size - 6} ikon",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
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
                        } else if (item.type == ShopItemType.STREAK_FREEZE) {
                            // Pro Zmražení řady zobraz počet dostupných
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Zmražení řady",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${StreakFreezeManager.getAvailableFreezes()}/3",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                        // Pro Zmražení řady zobraz počet vlastněných místo ceny
                        if (item.type == ShopItemType.STREAK_FREEZE) {
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Zmražení řady",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${StreakFreezeManager.getAvailableFreezes()}/3",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                // Zobraz cenu menším písmem pod počtem
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = "Body",
                                        tint = if (canAfford) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = item.price.toString(),
                                        color = if (canAfford) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                if (!canAfford) {
                                    Text(
                                        text = "Nedostatek bodů",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else if (item.type == ShopItemType.POINTS_MULTIPLIER && isOnCooldown) {
                            // Pro power-upy na cooldownu (i když nejsou vlastněné)
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
                            // Standardní zobrazení ceny pro ostatní itemy
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
            val titleText = when {
                item.type == ShopItemType.STREAK_FREEZE && isPurchased -> "Maximum dosaženo"
                isPurchased -> "Již vlastníš"
                else -> "Potvrdit nákup"
            }
            Text(
                text = titleText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Pro zvířátka zobrazíme emoji ikonu
                if (item.type == ShopItemType.PET && item.petIcon != null) {
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
                            text = item.petIcon,
                            fontSize = 48.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Pro power-upy zobrazíme barevný text multiplikátoru
                if (item.type == ShopItemType.POINTS_MULTIPLIER && item.powerUpIcon != null) {
                    val powerUpColor = when (item.multiplier) {
                        1.5f -> Color(0xFF00BCD4) // Aqua
                        2.0f -> Color(0xFFFFD700) // Zlatá
                        3.0f -> Color(0xFF9C27B0) // Fialová
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                powerUpColor.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.powerUpIcon,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = powerUpColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Pro balíčky ikon zobrazíme kompletní seznam
                if (item.type == ShopItemType.ICON_PACK && item.subjectIcons != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Obsah balíčku:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Grid s 3 sloupci - lepší layout
                            val chunkedIcons = item.subjectIcons.chunked(3)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chunkedIcons.forEach { rowIcons ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        rowIcons.forEach { subjectIcon ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (item.iconPackType == IconPackType.EMOJI) {
                                                        Text(
                                                            text = subjectIcon.icon,
                                                            fontSize = 24.sp,
                                                            maxLines = 1
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = when (subjectIcon.icon) {
                                                                "edit" -> Icons.Default.Edit
                                                                "library_books" -> Icons.Default.LibraryBooks
                                                                "article" -> Icons.Default.Article
                                                                "music_note" -> Icons.Default.MusicNote
                                                                "functions" -> Icons.Default.Functions
                                                                "biotech" -> Icons.Default.Biotech
                                                                "bolt" -> Icons.Default.Bolt
                                                                "local_florist" -> Icons.Default.LocalFlorist
                                                                "language" -> Icons.Default.Language
                                                                // Přidáno mapování pro klasické ikony
                                                                "spellcheck" -> Icons.Default.Spellcheck
                                                                "menu_book" -> Icons.Default.MenuBook
                                                                "description" -> Icons.Default.Description
                                                                "library_music" -> Icons.Default.LibraryMusic
                                                                "calculate" -> Icons.Default.Calculate
                                                                "science" -> Icons.Default.Science
                                                                "precision_manufacturing" -> Icons.Default.PrecisionManufacturing
                                                                "eco" -> Icons.Default.Eco
                                                                "public" -> Icons.Default.Public
                                                                else -> Icons.Default.Book
                                                            },
                                                            contentDescription = subjectIcon.subjectName,
                                                            tint = getSubjectColorByName(subjectIcon.subjectName),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = subjectIcon.subjectName,
                                                    fontSize = 10.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        // Vyplnění prázdných míst
                                        repeat(3 - rowIcons.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(text = item.description)
                
                if (item.type == ShopItemType.STREAK_FREEZE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aktuálně vlastníš: ${StreakFreezeManager.getAvailableFreezes()}/3 Zmražení",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
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
                val canPurchase = if (item.type == ShopItemType.STREAK_FREEZE) {
                    totalPoints >= item.price && ShopManager.canPurchaseStreakFreeze()
                } else {
                    totalPoints >= item.price
                }
                
                Button(
                    onClick = onConfirm,
                    enabled = canPurchase
                ) {
                    Text(
                        if (item.type == ShopItemType.STREAK_FREEZE && !ShopManager.canPurchaseStreakFreeze()) {
                            "Maximum dosaženo"
                        } else {
                            "Koupit"
                        }
                    )
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