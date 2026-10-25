package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.components.IconButton

import com.tobiso.tobisoappnative.components.TextButton

import com.tobiso.tobisoappnative.components.Button

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tobiso.tobisoappnative.navigation.BackpackRoute
import com.tobiso.tobisoappnative.components.formatPointsBalance
import timber.log.Timber

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.ShopManager
import com.tobiso.tobisoappnative.StreakFreezeManager
import com.tobiso.tobisoappnative.data.ShopData
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.components.MysteryBoxOpeningOverlay
import com.tobiso.tobisoappnative.components.formatPointsBalance
import com.tobiso.tobisoappnative.model.*
import com.tobiso.tobisoappnative.IconPackManager
import com.tobiso.tobisoappnative.viewmodel.shop.ShopViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    navController: NavController,
    embedded: Boolean = false,
    vm: ShopViewModel = hiltViewModel()
) {
    val totalPoints by PointsManager.instance.totalPoints.collectAsStateWithLifecycle()
    val totalPointsFloat by PointsManager.instance.totalPointsFloat.collectAsStateWithLifecycle()
    val purchasedItemIds by ShopManager.instance.purchasedItems.collectAsStateWithLifecycle()
    val activeMultiplier by PointsManager.instance.activeMultiplier.collectAsStateWithLifecycle()
    val availableFreezes by StreakFreezeManager.instance.availableFreezes.collectAsStateWithLifecycle()

    val selectedItem by vm.selectedItem.collectAsStateWithLifecycle()
    val showPurchaseDialog by vm.showPurchaseDialog.collectAsStateWithLifecycle()
    val showUsePowerUpDialog by vm.showUsePowerUpDialog.collectAsStateWithLifecycle()
    val showSuccessMessage by vm.showSuccessMessage.collectAsStateWithLifecycle()
    val successMessage by vm.successMessage.collectAsStateWithLifecycle()
    val showErrorMessage by vm.showErrorMessage.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    val showMysteryBoxOverlay by vm.showMysteryBoxOverlay.collectAsStateWithLifecycle()
    val mysteryBoxRewardText by vm.mysteryBoxRewardText.collectAsStateWithLifecycle()

    // Pro scroll k sekcím - trackování pozic nadpisů
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val categories = remember { ShopCategory.values().filter { it != ShopCategory.EXCLUSIVE } }
    // Layout sekcí se sestaví jednou, aby se seznam itemů ani pozice headerů
    // nepřepočítávaly při každé rekombinaci.
    val sections = remember(categories) {
        categories.map { category -> category to ShopData.getItemsByCategory(category) }
    }
    val headerIndices = remember(sections) {
        buildMap {
            var index = 0
            sections.forEachIndexed { sectionIndex, (category, items) ->
                put(category, index)
                index += 1 + items.size
                if (sectionIndex < sections.lastIndex) index++ // spacer
            }
        }
    }
    val sortedHeaderIndices = remember(headerIndices) {
        headerIndices.entries.sortedBy { it.value }
    }

    // Sledování aktivní kategorie na základě scroll pozice.
    // firstVisibleItemIndex se mění jen na hranicích itemů, zatímco layoutInfo
    // se aktualizuje každý frame a při scrollu zbytečně alokoval.
    val activeCategory by remember(listState, sortedHeaderIndices, categories) {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            sortedHeaderIndices.lastOrNull { firstIndex >= it.value }?.key
                ?: categories.first()
        }
    }

    // Hoisted stav pro kartičky, aby si každá při scrollu nezakládala vlastní kolekci.
    val foodCount by com.tobiso.tobisoappnative.PetManager.foodCount.collectAsStateWithLifecycle()
    val waterCount by com.tobiso.tobisoappnative.PetManager.waterCount.collectAsStateWithLifecycle()

    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!embedded) {
        // Top App Bar
        TopAppBar(
            title = { Text("Obchod", style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                }
            },
            actions = {
                com.tobiso.tobisoappnative.components.StatChip(
                    icon = Icons.Rounded.Stars,
                    text = formatPointsBalance(totalPointsFloat)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        )
        }

        // Navigační kategorie (pouze pro scroll)
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.name }) { category ->
                CategoryNavigationChip(
                    category = category,
                    isActive = category == activeCategory,
                    onClick = {
                        headerIndices[category]?.let { targetIndex ->
                            coroutineScope.launch {
                                try {
                                    listState.animateScrollToItem(index = targetIndex)
                                } catch (e: Exception) {
                                    Timber.d("Error scrolling: $e")
                                    listState.scrollToItem(targetIndex)
                                }
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
            // Všechny kategorie pod sebou
            sections.forEachIndexed { categoryIndex, (category, categoryItems) ->
                // Nadpis kategorie
                item(key = "header_$category", contentType = "header") {
                    Text(
                        text = category.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp)
                    )
                }

                // Položky kategorie
                items(
                    items = categoryItems,
                    key = { item -> "item_${item.id}" },
                    contentType = { item -> item.type }
                ) { item ->
                    ShopItemCard(
                        item = item,
                        isPurchased = when (item.type) {
                            ShopItemType.STREAK_FREEZE -> availableFreezes >= 3
                            ShopItemType.AI_CREDIT -> false
                            ShopItemType.MYSTERY_BOX -> false
                            else -> purchasedItemIds.contains(item.id)
                        },
                        canAfford = totalPoints >= item.price,
                        isOnCooldown = ShopManager.instance.isOnCooldown(item.id),
                        cooldownTimeLeft = ShopManager.instance.getCooldownTimeLeft(item.id),
                        availableFreezes = availableFreezes,
                        foodCount = foodCount,
                        waterCount = waterCount,
                        onClick = {
                            vm.selectItem(item, purchasedItemIds)
                        }
                    )
                }

                // Mezera mezi kategoriemi (kromě poslední)
                if (categoryIndex < sections.lastIndex) {
                    item(key = "spacer_$category", contentType = "spacer") {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // Spodní padding
            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Purchase Dialog
    if (showPurchaseDialog) {
        selectedItem?.let { item ->
            PurchaseDialog(
                item = item,
                totalPoints = totalPoints,
                isPurchased = when (item.type) {
                    ShopItemType.AI_CREDIT -> false
                    else -> purchasedItemIds.contains(item.id)
                },
                onConfirm = { vm.confirmPurchase() },
                onDismiss = { vm.dismissPurchaseDialog() }
            )
        }
    }

    // Use Power-Up Dialog
    if (showUsePowerUpDialog) {
        selectedItem?.let { item ->
            UsePowerUpDialog(
                item = item,
                isOnCooldown = ShopManager.instance.isOnCooldown(item.id),
                cooldownTimeLeft = ShopManager.instance.getCooldownTimeLeft(item.id),
                onConfirm = { vm.confirmUsePowerUp() },
                onDismiss = { vm.dismissUsePowerUpDialog() }
            )
        }
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

    // Mystery Box opening overlay
    if (showMysteryBoxOverlay) {
        MysteryBoxOpeningOverlay(
            rewardText = mysteryBoxRewardText,
            onDismiss = { vm.dismissMysteryBoxOverlay() }
        )
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
                    text = successMessage,
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
fun PricePill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, container: Color, content: Color) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .background(container, RoundedCornerShape(50))
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(16.dp))
        Text(text, color = content, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
    }
}

@Composable
fun CategoryNavigationChip(
    category: ShopCategory,
    isActive: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        leadingIcon = if (isActive) ({ Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) }) else null,
        label = { Text(text = category.displayName, fontWeight = FontWeight.SemiBold) }
    )
}



@Composable
fun ShopItemCard(
    item: ShopItem,
    isPurchased: Boolean,
    canAfford: Boolean,
    isOnCooldown: Boolean = false,
    cooldownTimeLeft: Long = 0,
    availableFreezes: Int = 0,
    foodCount: Int = 0,
    waterCount: Int = 0,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    
    val isClickable = !isPurchased && !(isOnCooldown && item.type == ShopItemType.POINTS_MULTIPLIER)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable) { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
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

            // Pro krmivo a vodu zobrazíme emoji ikonu
            if ((item.type == ShopItemType.PET_FOOD || item.type == ShopItemType.PET_WATER) && item.petIcon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            if (item.type == ShopItemType.PET_FOOD)
                                Color(0xFFFFF3E0).copy(alpha = 0.6f)
                            else
                                Color(0xFFE3F2FD).copy(alpha = 0.6f),
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
            
            // Pro motivy profilu zobrazíme emoji na gradientu
            if (item.type == ShopItemType.PROFILE_THEME && item.powerUpIcon != null) {
                val themeColors = ProfileThemeData.getThemeColors(item.id)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    themeColors?.primary ?: MaterialTheme.colorScheme.primary,
                                    themeColors?.secondary ?: MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item.powerUpIcon, fontSize = 54.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Pro tajemnou krabici zobrazíme dárek s otazníkem
            if (item.type == ShopItemType.MYSTERY_BOX && item.powerUpIcon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Color(0xFFFFF3E0).copy(alpha = 0.8f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = item.powerUpIcon, fontSize = 54.sp)
                        Text(
                            text = "?",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Pro AI kredity zobrazíme robot emoji
            if (item.type == ShopItemType.AI_CREDIT && item.powerUpIcon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = item.powerUpIcon, fontSize = 48.sp)
                        Text(
                            text = "+${item.creditCount} otázek / 24 h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
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
                                                            "edit" -> Icons.Rounded.Edit
                                                            "library_books" -> Icons.AutoMirrored.Rounded.LibraryBooks
                                                            "article" -> Icons.AutoMirrored.Rounded.Article
                                                            "music_note" -> Icons.Rounded.MusicNote
                                                            "functions" -> Icons.Rounded.Functions
                                                            "biotech" -> Icons.Rounded.Biotech
                                                            "bolt" -> Icons.Rounded.Bolt
                                                            "local_florist" -> Icons.Rounded.LocalFlorist
                                                            "language" -> Icons.Rounded.Language
                                                            // Přidáno mapování pro klasické ikony
                                                            "spellcheck" -> Icons.Rounded.Spellcheck
                                                            "menu_book" -> Icons.AutoMirrored.Rounded.MenuBook
                                                            "description" -> Icons.Rounded.Description
                                                            "library_music" -> Icons.Rounded.LibraryMusic
                                                            "calculate" -> Icons.Rounded.Calculate
                                                            "science" -> Icons.Rounded.Science
                                                            "precision_manufacturing" -> Icons.Rounded.PrecisionManufacturing
                                                            "eco" -> Icons.Rounded.Eco
                                                            "public" -> Icons.Rounded.Public
                                                            else -> Icons.Rounded.Book
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
                                            imageVector = Icons.Rounded.Schedule,
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
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = "Zmražení řady",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${StreakFreezeManager.instance.getAvailableFreezes()}/3",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            PricePill(
                                text = "Vlastníš",
                                icon = Icons.Rounded.Check,
                                container = MaterialTheme.colorScheme.secondaryContainer,
                                content = MaterialTheme.colorScheme.onSecondaryContainer
                            )
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
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = "Zmražení řady",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${StreakFreezeManager.instance.getAvailableFreezes()}/3",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                // Zobraz cenu menším písmem pod počtem
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Stars,
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
                        } else if (item.type == ShopItemType.PET_FOOD || item.type == ShopItemType.PET_WATER) {
                            val count = if (item.type == ShopItemType.PET_FOOD) foodCount else waterCount
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                if (count > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.petIcon ?: ""} ${count}x",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Stars,
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
                        } else if (item.type == ShopItemType.POINTS_MULTIPLIER && isOnCooldown) {
                            // Pro power-upy na cooldownu (i když nejsou vlastněné)
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
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
                            if (canAfford) {
                                PricePill(
                                    text = item.price.toString(),
                                    icon = Icons.Rounded.Stars,
                                    container = MaterialTheme.colorScheme.primary,
                                    content = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                PricePill(
                                    text = item.price.toString(),
                                    icon = Icons.Rounded.Lock,
                                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    content = MaterialTheme.colorScheme.onSurfaceVariant
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

                // Pro krmivo a vodu zobrazíme emoji ikonu
                if ((item.type == ShopItemType.PET_FOOD || item.type == ShopItemType.PET_WATER) && item.petIcon != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                if (item.type == ShopItemType.PET_FOOD)
                                    Color(0xFFFFF3E0).copy(alpha = 0.6f)
                                else
                                    Color(0xFFE3F2FD).copy(alpha = 0.6f),
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

                // Pro AI kredity zobrazíme robot emoji
                if (item.type == ShopItemType.AI_CREDIT && item.powerUpIcon != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = item.powerUpIcon, fontSize = 36.sp)
                            Text(
                                text = "+${item.creditCount} otázek / 24 h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Pro motivy profilu zobrazíme emoji na gradientu v dialogu
                if (item.type == ShopItemType.PROFILE_THEME && item.powerUpIcon != null) {
                    val themeColors = ProfileThemeData.getThemeColors(item.id)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        themeColors?.primary ?: MaterialTheme.colorScheme.primary,
                                        themeColors?.secondary ?: MaterialTheme.colorScheme.tertiary
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.powerUpIcon, fontSize = 48.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Pro tajemnou krabici zobrazíme dárek v dialogu
                if (item.type == ShopItemType.MYSTERY_BOX && item.powerUpIcon != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                Color(0xFFFFF3E0).copy(alpha = 0.8f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = item.powerUpIcon, fontSize = 48.sp)
                        }
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
                                                                "edit" -> Icons.Rounded.Edit
                                                                "library_books" -> Icons.AutoMirrored.Rounded.LibraryBooks
                                                                "article" -> Icons.AutoMirrored.Rounded.Article
                                                                "music_note" -> Icons.Rounded.MusicNote
                                                                "functions" -> Icons.Rounded.Functions
                                                                "biotech" -> Icons.Rounded.Biotech
                                                                "bolt" -> Icons.Rounded.Bolt
                                                                "local_florist" -> Icons.Rounded.LocalFlorist
                                                                "language" -> Icons.Rounded.Language
                                                                // Přidáno mapování pro klasické ikony
                                                                "spellcheck" -> Icons.Rounded.Spellcheck
                                                            "menu_book" -> Icons.AutoMirrored.Rounded.MenuBook
                                                                "description" -> Icons.Rounded.Description
                                                                "library_music" -> Icons.Rounded.LibraryMusic
                                                                "calculate" -> Icons.Rounded.Calculate
                                                                "science" -> Icons.Rounded.Science
                                                                "precision_manufacturing" -> Icons.Rounded.PrecisionManufacturing
                                                                "eco" -> Icons.Rounded.Eco
                                                                "public" -> Icons.Rounded.Public
                                                                else -> Icons.Rounded.Book
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
                        text = "Aktuálně vlastníš: ${StreakFreezeManager.instance.getAvailableFreezes()}/3 Zmražení",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (item.type == ShopItemType.PET_FOOD) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "V zásobě: ${com.tobiso.tobisoappnative.PetManager.foodCount.value}x krmiva",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (item.type == ShopItemType.PET_WATER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "V zásobě: ${com.tobiso.tobisoappnative.PetManager.waterCount.value}x vody",
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
                                imageVector = Icons.Rounded.Stars,
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
                    totalPoints >= item.price && ShopManager.instance.canPurchaseStreakFreeze()
                } else {
                    totalPoints >= item.price
                }
                
                Button(
                    onClick = onConfirm,
                    enabled = canPurchase
                ) {
                    Text(
                        if (item.type == ShopItemType.STREAK_FREEZE && !ShopManager.instance.canPurchaseStreakFreeze()) {
                            "Maximum dosaženo"
                        } else if (item.type == ShopItemType.PET_FOOD) {
                            "Koupit (5 porcí)"
                        } else if (item.type == ShopItemType.PET_WATER) {
                            "Koupit (5 porcí)"
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
                            imageVector = Icons.Rounded.Schedule,
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