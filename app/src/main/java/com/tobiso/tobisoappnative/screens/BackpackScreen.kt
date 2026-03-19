package com.tobiso.tobisoappnative.screens

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
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.BackpackManager
import com.tobiso.tobisoappnative.IconPackManager
import com.tobiso.tobisoappnative.model.*
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.viewmodel.backpack.BackpackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackpackScreen(
    navController: NavController,
    vm: BackpackViewModel = hiltViewModel()
) {
    val backpackItems by BackpackManager.backpackItems.collectAsState()
    val equippedQuote by BackpackManager.equippedQuote.collectAsState()
    val equippedPet by BackpackManager.equippedPet.collectAsState()

    val selectedItem by vm.selectedItem.collectAsState()
    val showItemDialog by vm.showItemDialog.collectAsState()
    val showSuccessMessage by vm.showSuccessMessage.collectAsState()
    val successMessage by vm.successMessage.collectAsState()

    // Pro scroll k sekcím
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val headerPositions = remember { mutableMapOf<BackpackCategory, Int>() }

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

            // Default - první kategorie s obsahem
            BackpackCategory.entries.firstOrNull { category ->
                BackpackManager.getItemsByCategory(category).isNotEmpty()
            } ?: BackpackCategory.QUOTES
        }
    }


    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Aktovka", style = MaterialTheme.typography.headlineLarge) },
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
                        imageVector = Icons.Outlined.Backpack,
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
                            imageVector = Icons.Outlined.ShoppingBag,
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
                items(BackpackCategory.entries.filter { category ->
                    BackpackManager.getItemsByCategory(category).isNotEmpty()
                }) { category ->
                    BackpackCategoryChip(
                        category = category,
                        isActive = category == activeCategory,
                        onClick = {
                            coroutineScope.launch {
                                val targetIndex = headerPositions[category]
                                if (targetIndex != null) {
                                    try {
                                        listState.animateScrollToItem(
                                            index = targetIndex,
                                            scrollOffset = 0
                                        )
                                    } catch (e: Exception) {
                                        listState.scrollToItem(targetIndex)
                                    }
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
                BackpackCategory.entries.forEach { category ->
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
                            key = { item -> "item_${category.name}_${item.shopItem.id}" }
                        ) { backpackItem ->
                            BackpackItemCard(
                                backpackItem = backpackItem,
                                isEquipped = when (backpackItem.shopItem.type) {
                                    ShopItemType.PROFILE_QUOTE -> equippedQuote?.id == backpackItem.shopItem.id
                                    ShopItemType.PET -> equippedPet?.id == backpackItem.shopItem.id
                                    ShopItemType.ICON_PACK -> BackpackManager.equippedIconPack.collectAsState().value?.id == backpackItem.shopItem.id
                                    else -> false
                                },
                                onClick = {
                                    vm.selectItem(backpackItem)
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
                ShopItemType.ICON_PACK -> BackpackManager.equippedIconPack.collectAsState().value?.id == selectedItem!!.shopItem.id
                else -> false
            },
            onEquip = { vm.equipItem(selectedItem!!) },
            onUnequip = { vm.unequipItem(selectedItem!!) },
            onDismiss = { vm.dismissDialog() }
        )
    }

    // Success Snackbar
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(2000)
            vm.clearSuccessMessage()
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
            
            // Pro balíčky ikon zobrazíme náhled
            if (backpackItem.shopItem.type == ShopItemType.ICON_PACK && backpackItem.shopItem.subjectIcons != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        // Kompaktní row pro 4 ikony
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            backpackItem.shopItem.subjectIcons.take(4).forEach { subjectIcon ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (backpackItem.shopItem.iconPackType == IconPackType.EMOJI) {
                                            Text(
                                                text = subjectIcon.icon,
                                                fontSize = 16.sp,
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
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = subjectIcon.subjectName.take(3),
                                        fontSize = 8.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            // Zobrazení +X počtu zbývajících ikon
                            if (backpackItem.shopItem.subjectIcons.size > 4) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${backpackItem.shopItem.subjectIcons.size - 4}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "více",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                                text = "Nasazeno",
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
                
                // Pro balíčky ikon zobrazíme všechny ikony
                if (backpackItem.shopItem.type == ShopItemType.ICON_PACK && backpackItem.shopItem.subjectIcons != null) {
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
                            
                            // Grid s 3 sloupci
                            val chunkedIcons = backpackItem.shopItem.subjectIcons.chunked(3)
                            chunkedIcons.forEach { rowIcons ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowIcons.forEach { subjectIcon ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (backpackItem.shopItem.iconPackType == IconPackType.EMOJI) {
                                                Text(
                                                    text = subjectIcon.icon,
                                                    fontSize = 28.sp
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
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = subjectIcon.subjectName,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                    // Vyplnění prázdných míst
                                    repeat(3 - rowIcons.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                if (rowIcons != chunkedIcons.last()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
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
                            text = "Aktuálně nasazeno",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (backpackItem.shopItem.type == ShopItemType.PROFILE_QUOTE || 
                backpackItem.shopItem.type == ShopItemType.PET ||
                backpackItem.shopItem.type == ShopItemType.ICON_PACK) {
                if (isEquipped) {
                    Button(onClick = onUnequip) {
                        Text(when (backpackItem.shopItem.type) {
                            ShopItemType.ICON_PACK -> "Deaktivovat"
                            else -> "Odebrat"
                        })
                    }
                } else {
                    Button(onClick = onEquip) {
                        Text(when (backpackItem.shopItem.type) {
                            ShopItemType.ICON_PACK -> "Aktivovat"
                            else -> "Nasadit"
                        })
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