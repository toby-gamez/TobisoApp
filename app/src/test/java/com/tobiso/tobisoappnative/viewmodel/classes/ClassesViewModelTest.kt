package com.tobiso.tobisoappnative.viewmodel.classes

import app.cash.turbine.test
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.AuthUser
import com.tobiso.tobisoappnative.model.ClassGroup
import com.tobiso.tobisoappnative.model.MyClass
import com.tobiso.tobisoappnative.model.SchoolClass
import com.tobiso.tobisoappnative.repository.ClassesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClassesViewModelTest {

    private lateinit var repo: ClassesRepository
    private lateinit var vm: ClassesViewModel

    private val groups = listOf(ClassGroup(1, "Skupina 1"), ClassGroup(2, "Skupina 2"))
    private val seventhA = SchoolClass(10, "7.A", gradeName = "7. ročník", groups = groups)
    private val approved = MyClass(10, "7.A", "Approved", groups = listOf(groups[0]), availableGroups = groups)

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        AuthSession.signIn("jwt", AuthUser("Jana"))
        repo = mockk()
        vm = ClassesViewModel(repo)
    }

    @After
    fun tearDown() {
        AuthSession.signOut()
        Dispatchers.setMain(Dispatchers.Default)
    }

    @Test
    fun `without a membership the class picker is loaded`() = runTest {
        coEvery { repo.getMyClass() } returns Result.success(null)
        coEvery { repo.getClasses() } returns Result.success(listOf(seventhA))
        vm.onIntent(ClassesIntent.Load)

        assertNull(vm.uiState.value.mine)
        assertEquals(listOf(seventhA), vm.uiState.value.classes)
        assertTrue(!vm.uiState.value.isLoading)
    }

    @Test
    fun `an approved member does not need the class list`() = runTest {
        coEvery { repo.getMyClass() } returns Result.success(approved)
        vm.onIntent(ClassesIntent.Load)

        assertEquals(approved, vm.uiState.value.mine)
        assertTrue(vm.uiState.value.classes.isEmpty())
        coVerify(exactly = 0) { repo.getClasses() }
    }

    @Test
    fun `joining a class with groups requires choosing one`() = runTest {
        coEvery { repo.getMyClass() } returns Result.success(null)
        coEvery { repo.getClasses() } returns Result.success(listOf(seventhA))
        vm.onIntent(ClassesIntent.Load)
        vm.onIntent(ClassesIntent.SelectClass(10))

        vm.effect.test {
            vm.onIntent(ClassesIntent.Join)
            assertTrue(awaitItem() is ClassesEffect.ShowMessage)
        }
        coVerify(exactly = 0) { repo.join(any(), any()) }
    }

    @Test
    fun `join sends the selected groups and stores the pending membership`() = runTest {
        val pending = MyClass(10, "7.A", "Pending", groups = listOf(groups[1]))
        coEvery { repo.getMyClass() } returns Result.success(null)
        coEvery { repo.getClasses() } returns Result.success(listOf(seventhA))
        coEvery { repo.join(10, listOf(2)) } returns Result.success(pending)
        vm.onIntent(ClassesIntent.Load)
        vm.onIntent(ClassesIntent.SelectClass(10))
        vm.onIntent(ClassesIntent.ToggleGroup(2))
        vm.onIntent(ClassesIntent.Join)

        assertEquals(pending, vm.uiState.value.mine)
        assertTrue(!vm.uiState.value.isBusy)
    }

    @Test
    fun `leaving reloads the picker`() = runTest {
        coEvery { repo.getMyClass() } returnsMany listOf(Result.success(approved), Result.success(null))
        coEvery { repo.getClasses() } returns Result.success(listOf(seventhA))
        coEvery { repo.leave() } returns Result.success(Unit)
        vm.onIntent(ClassesIntent.Load)
        vm.onIntent(ClassesIntent.Leave)

        assertNull(vm.uiState.value.mine)
        assertEquals(1, vm.uiState.value.classes.size)
    }

    @Test
    fun `group change prefills current groups and ends editing after saving`() = runTest {
        val changed = approved.copy(groupChangePending = true, pendingGroup = groups[1])
        coEvery { repo.getMyClass() } returns Result.success(approved)
        coEvery { repo.updateGroups(listOf(2)) } returns Result.success(changed)
        vm.onIntent(ClassesIntent.Load)
        vm.onIntent(ClassesIntent.StartEditGroups)
        assertEquals(setOf(1), vm.uiState.value.selectedGroupIds)

        vm.onIntent(ClassesIntent.ToggleGroup(1))
        vm.onIntent(ClassesIntent.ToggleGroup(2))
        vm.onIntent(ClassesIntent.SaveGroups)

        assertEquals(changed, vm.uiState.value.mine)
        assertTrue(!vm.uiState.value.editingGroups)
    }
}
