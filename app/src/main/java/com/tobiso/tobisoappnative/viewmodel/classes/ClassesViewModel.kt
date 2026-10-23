package com.tobiso.tobisoappnative.viewmodel.classes

import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseViewModel
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.MyClass
import com.tobiso.tobisoappnative.repository.ClassesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassesViewModel @Inject constructor(
    private val repo: ClassesRepository
) : BaseViewModel<ClassesState, ClassesIntent, ClassesEffect>(ClassesState()) {

    override fun onIntent(intent: ClassesIntent) {
        when (intent) {
            ClassesIntent.Load -> load()
            is ClassesIntent.SelectClass -> setState { copy(selectedClassId = intent.classId, selectedGroupIds = emptySet()) }
            is ClassesIntent.ToggleGroup -> setState {
                copy(selectedGroupIds = if (intent.groupId in selectedGroupIds) selectedGroupIds - intent.groupId else selectedGroupIds + intent.groupId)
            }
            ClassesIntent.Join -> join()
            ClassesIntent.StartEditGroups -> setState {
                copy(editingGroups = true, selectedGroupIds = mine?.groups?.map { it.id }?.toSet().orEmpty())
            }
            ClassesIntent.CancelEditGroups -> setState { copy(editingGroups = false, selectedGroupIds = emptySet()) }
            ClassesIntent.SaveGroups -> saveGroups()
            ClassesIntent.Leave -> leave()
        }
    }

    private fun load() {
        if (!AuthSession.isSignedIn) {
            setState { copy(isLoading = false) }
            return
        }
        setState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val mine = repo.getMyClass()
            mine.onFailure { e -> setState { copy(isLoading = false, error = e.message) }; return@launch }
            val myClass = mine.getOrNull()
            // The picker is only needed while there is no membership (or a rejected one).
            val classes = if (myClass == null || myClass.membershipStatus == com.tobiso.tobisoappnative.model.ClassMembershipStatus.Rejected)
                repo.getClasses().getOrElse { e -> setState { copy(isLoading = false, error = e.message) }; return@launch }
            else emptyList()
            setState { copy(isLoading = false, mine = myClass, classes = classes) }
        }
    }

    private fun join() {
        val s = currentState
        val classId = s.selectedClassId ?: return
        if (s.isBusy) return
        // A class with groups requires choosing at least one.
        if (s.selectedClass?.groups?.isNotEmpty() == true && s.selectedGroupIds.isEmpty()) {
            emitEffect(ClassesEffect.ShowMessage("Vyber alespoň jednu skupinu."))
            return
        }
        setState { copy(isBusy = true) }
        viewModelScope.launch {
            repo.join(classId, s.selectedGroupIds.toList()).fold(
                onSuccess = { m -> setState { copy(isBusy = false, mine = m, classes = emptyList(), selectedClassId = null, selectedGroupIds = emptySet()) } },
                onFailure = { e -> setState { copy(isBusy = false) }; emitEffect(ClassesEffect.ShowMessage(e.message ?: "Nepodařilo se odeslat žádost.")) }
            )
        }
    }

    private fun saveGroups() {
        val s = currentState
        if (s.isBusy) return
        if (s.selectedGroupIds.isEmpty()) {
            emitEffect(ClassesEffect.ShowMessage("Vyber alespoň jednu skupinu."))
            return
        }
        setState { copy(isBusy = true) }
        viewModelScope.launch {
            repo.updateGroups(s.selectedGroupIds.toList()).fold(
                onSuccess = { m: MyClass ->
                    setState { copy(isBusy = false, mine = m, editingGroups = false, selectedGroupIds = emptySet()) }
                    emitEffect(ClassesEffect.ShowMessage("Žádost o změnu skupiny byla odeslána ke schválení."))
                },
                onFailure = { e -> setState { copy(isBusy = false) }; emitEffect(ClassesEffect.ShowMessage(e.message ?: "Změna se nepodařila.")) }
            )
        }
    }

    private fun leave() {
        if (currentState.isBusy) return
        setState { copy(isBusy = true) }
        viewModelScope.launch {
            repo.leave().fold(
                onSuccess = { setState { copy(isBusy = false, mine = null, editingGroups = false) }; load() },
                onFailure = { e -> setState { copy(isBusy = false) }; emitEffect(ClassesEffect.ShowMessage(e.message ?: "Odchod se nepodařil.")) }
            )
        }
    }
}
