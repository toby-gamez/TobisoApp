package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.components.IconButton

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.components.StatChip
import com.tobiso.tobisoappnative.components.formatPointsBalance
import com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle

/** Obchod + Aktovka as one screen with a segmented toggle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopHubScreen(navController: NavController, initialTab: Int) {
    var tab by remember { mutableIntStateOf(initialTab) }
    val points by PointsManager.instance.totalPointsFloat.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (tab == 0) "Obchod" else "Aktovka", style = SecondaryTopBarTitle) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                }
            },
            actions = {
                StatChip(icon = Icons.Rounded.Stars, text = formatPointsBalance(points))
                Spacer(Modifier.width(12.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
        ) {
            listOf("Obchod" to Icons.Rounded.Storefront, "Aktovka" to Icons.Rounded.Backpack).forEachIndexed { index, (label, icon) ->
                if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (tab == index) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { tab = index },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(icon, null, modifier = Modifier.size(18.dp))
                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 4 * dir } + fadeOut())
            },
            label = "shopTab"
        ) { t ->
            if (t == 0) ShopScreen(navController = navController, embedded = true)
            else BackpackScreen(navController = navController, embedded = true)
        }
    }
}
