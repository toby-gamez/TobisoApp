package com.tobiso.tobisoappnative.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import com.tobiso.tobisoappnative.components.Card
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.ClassGroup
import com.tobiso.tobisoappnative.model.ClassMembershipStatus
import com.tobiso.tobisoappnative.model.MyClass
import com.tobiso.tobisoappnative.navigation.AuthRoute
import com.tobiso.tobisoappnative.viewmodel.classes.ClassesEffect
import com.tobiso.tobisoappnative.viewmodel.classes.ClassesIntent
import com.tobiso.tobisoappnative.viewmodel.classes.ClassesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClassesScreen(navController: NavController) {
    val vm: ClassesViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val user by AuthSession.user.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(user) { vm.onIntent(ClassesIntent.Load) }
    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is ClassesEffect.ShowMessage -> Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Moje třída", style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                }
            }
        )

        val mine = state.mine
        when {
            user == null -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Třídy jsou dostupné po přihlášení.", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Po schválení třídy uvidíš v kalendáři i testy a úkoly své třídy a skupin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { navController.navigate(AuthRoute) }) { Text("Přihlásit se") }
            }
            state.isLoading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            state.error != null && mine == null && state.classes.isEmpty() -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { vm.onIntent(ClassesIntent.Load) }) { Text("Zkusit znovu") }
            }
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (mine != null && mine.membershipStatus != ClassMembershipStatus.Rejected) {
                    MembershipCard(
                        mine = mine,
                        editing = state.editingGroups,
                        selectedGroupIds = state.selectedGroupIds,
                        busy = state.isBusy,
                        onToggleGroup = { vm.onIntent(ClassesIntent.ToggleGroup(it)) },
                        onStartEdit = { vm.onIntent(ClassesIntent.StartEditGroups) },
                        onCancelEdit = { vm.onIntent(ClassesIntent.CancelEditGroups) },
                        onSaveGroups = { vm.onIntent(ClassesIntent.SaveGroups) },
                        onLeave = { confirmLeave = true }
                    )
                } else {
                    if (mine != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                "Žádost o přijetí do třídy ${mine.className} byla zamítnuta. Můžeš zvolit jinou třídu.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Text("Vyber svou třídu", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Přijetí potvrzuje správce. Do té doby se ti zobrazují jen společné události.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.classes.isEmpty()) Text("Zatím nejsou k dispozici žádné třídy.")
                    state.classes.forEach { c ->
                        val selected = c.id == state.selectedClassId
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = selected,
                                onClick = { vm.onIntent(ClassesIntent.SelectClass(c.id)) },
                                role = Role.RadioButton
                            )
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected, onClick = null)
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                                    c.gradeName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                    state.selectedClass?.takeIf { it.groups.isNotEmpty() }?.let { c ->
                        Text("Skupiny", style = MaterialTheme.typography.titleSmall)
                        GroupChips(c.groups, state.selectedGroupIds) { vm.onIntent(ClassesIntent.ToggleGroup(it)) }
                    }
                    Button(
                        onClick = { vm.onIntent(ClassesIntent.Join) },
                        enabled = state.selectedClassId != null && !state.isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Požádat o přijetí") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmLeave) {
        val pending = state.mine?.membershipStatus == ClassMembershipStatus.Pending
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(if (pending) "Zrušit žádost?" else "Opustit třídu?") },
            text = { Text(if (pending) "Žádost o přijetí do třídy bude stažena." else "Přestaneš vidět události své třídy a skupin.") },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; vm.onIntent(ClassesIntent.Leave) }) {
                    Text(if (pending) "Zrušit žádost" else "Opustit")
                }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Zpět") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupChips(groups: List<ClassGroup>, selected: Set<Int>, onToggle: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.forEach { g ->
            FilterChip(shape = RoundedCornerShape(10.dp), selected = g.id in selected, onClick = { onToggle(g.id) }, label = { Text(g.name) })
        }
    }
}

@Composable
private fun MembershipCard(
    mine: MyClass,
    editing: Boolean,
    selectedGroupIds: Set<Int>,
    busy: Boolean,
    onToggleGroup: (Int) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveGroups: () -> Unit,
    onLeave: () -> Unit
) {
    val pending = mine.membershipStatus == ClassMembershipStatus.Pending
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (pending) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(mine.className, style = MaterialTheme.typography.headlineSmall)
            Text(
                if (pending) "Čeká na schválení správcem" else "Jsi členem třídy",
                style = MaterialTheme.typography.labelLarge
            )
            if (mine.groups.isNotEmpty()) {
                Text("Skupiny: " + mine.groups.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium)
            }
            if (mine.groupChangePending) {
                Text(
                    "Změna skupiny čeká na schválení" + (mine.pendingGroup?.let { " (${it.name})" } ?: "") + ".",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (editing) {
                GroupChips(mine.availableGroups, selectedGroupIds, onToggleGroup)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveGroups, enabled = !busy) { Text("Odeslat žádost") }
                    TextButton(onClick = onCancelEdit) { Text("Zrušit") }
                }
            } else if (!pending && mine.availableGroups.isNotEmpty() && !mine.groupChangePending) {
                OutlinedButton(onClick = onStartEdit) { Text("Požádat o změnu skupiny") }
            }
            TextButton(onClick = onLeave, enabled = !busy) { Text(if (pending) "Zrušit žádost" else "Opustit třídu") }
        }
    }
}
