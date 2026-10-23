package com.tobiso.tobisoappnative.viewmodel.classes

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState
import com.tobiso.tobisoappnative.model.MyClass
import com.tobiso.tobisoappnative.model.SchoolClass

data class ClassesState(
    val isLoading: Boolean = true,
    /** True while a join / group change / leave request is in flight. */
    val isBusy: Boolean = false,
    val classes: List<SchoolClass> = emptyList(),
    val mine: MyClass? = null,
    val selectedClassId: Int? = null,
    val selectedGroupIds: Set<Int> = emptySet(),
    /** Approved student is choosing new groups (requires admin approval). */
    val editingGroups: Boolean = false,
    val error: String? = null
) : UiState {
    val selectedClass: SchoolClass? get() = classes.firstOrNull { it.id == selectedClassId }
}

sealed interface ClassesIntent : UiIntent {
    object Load : ClassesIntent
    data class SelectClass(val classId: Int) : ClassesIntent
    data class ToggleGroup(val groupId: Int) : ClassesIntent
    object Join : ClassesIntent
    object StartEditGroups : ClassesIntent
    object CancelEditGroups : ClassesIntent
    object SaveGroups : ClassesIntent
    /** Leave the class, or withdraw a pending request / dismiss a rejected one. */
    object Leave : ClassesIntent
}

sealed interface ClassesEffect : UiEffect {
    data class ShowMessage(val message: String) : ClassesEffect
}
