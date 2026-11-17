package com.tobiso.tobisoappnative.viewmodel.ai

import com.tobiso.tobisoappnative.AiCreditManager
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.CompareRequest
import com.tobiso.tobisoappnative.model.ConceptEdge
import com.tobiso.tobisoappnative.model.ConceptMapResponse
import com.tobiso.tobisoappnative.model.ConceptNode
import com.tobiso.tobisoappnative.model.SolverStep
import com.tobiso.tobisoappnative.model.StepSolverResponse
import com.tobiso.tobisoappnative.model.CompareResponse
import com.tobiso.tobisoappnative.model.CrossConnection
import com.tobiso.tobisoappnative.model.CrossConnectionResponse
import com.tobiso.tobisoappnative.model.ExamQuestion
import com.tobiso.tobisoappnative.model.ExamQuestionsResponse
import com.tobiso.tobisoappnative.model.FormulaEntry
import com.tobiso.tobisoappnative.model.FormulaVarsResponse
import com.tobiso.tobisoappnative.model.Grade
import com.tobiso.tobisoappnative.model.KeyTerm
import com.tobiso.tobisoappnative.model.KeyTermsResponse
import com.tobiso.tobisoappnative.model.RewriteGradeRequest
import com.tobiso.tobisoappnative.model.RewriteRegisterRequest
import com.tobiso.tobisoappnative.model.RewriteResponse
import com.tobiso.tobisoappnative.model.WhyRequest
import com.tobiso.tobisoappnative.model.WhyResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AiToolsViewModelTest {

    private lateinit var api: ApiService
    private lateinit var vm: AiToolsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        // viewModel reads the device id from the process-wide singleton, which needs a Context to build.
        val creditManager = mockk<AiCreditManager>()
        every { creditManager.deviceId } returns "device-1"
        AiCreditManager::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, creditManager)
        api = mockk()
        vm = AiToolsViewModel(api)
    }

    @After
    fun tearDown() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    /** The loaders run on Dispatchers.IO, so wait (bounded) for the state to reach [condition]. */
    private fun await(condition: (AiToolsState) -> Boolean): AiToolsState {
        val deadline = System.currentTimeMillis() + 3_000
        while (System.currentTimeMillis() < deadline) {
            val state = vm.state.value
            if (condition(state)) return state
            Thread.sleep(10)
        }
        fail("state never reached the expected condition: ${vm.state.value}")
        error("unreachable")
    }

    @Test
    fun `exam questions are loaded`() {
        coEvery { api.getExamQuestions("tobiso-android", "device-1", 5) } returns
            ExamQuestionsResponse(listOf(ExamQuestion("Co je atom?", "Nejmenší část prvku")))

        vm.loadExamQuestions(5)

        val state = await { !it.isLoading && it.examQuestions.isNotEmpty() }
        assertEquals("Co je atom?", state.examQuestions.single().question)
        assertNull(state.error)
    }

    @Test
    fun `exam questions failure sets an error`() {
        coEvery { api.getExamQuestions(any(), any(), any()) } throws RuntimeException("boom")

        vm.loadExamQuestions(5)

        val state = await { it.error != null }
        assertTrue(state.examQuestions.isEmpty())
        assertTrue(!state.isLoading)
    }

    @Test
    fun `key terms are loaded`() {
        coEvery { api.getKeyTerms(any(), any(), 2) } returns KeyTermsResponse(listOf(KeyTerm("Atom", "Částice")))

        vm.loadKeyTerms(2)

        assertEquals("Atom", await { it.keyTerms.isNotEmpty() }.keyTerms.single().term)
    }

    @Test
    fun `compare does nothing for a blank target`() {
        vm.setCompareTo("   ")

        vm.submitCompare(3)

        coVerify(exactly = 0) { api.compareWith(any(), any(), any()) }
        assertTrue(!vm.state.value.isLoading)
    }

    @Test
    fun `compare sends the trimmed target and keeps the table`() {
        coEvery { api.compareWith(any(), any(), CompareRequest(3, "Tygr")) } returns CompareResponse("| A | B |")
        vm.setCompareTo("  Tygr ")

        vm.submitCompare(3)

        assertEquals("| A | B |", await { it.compareTable.isNotEmpty() }.compareTable)
    }

    @Test
    fun `why sends the sentence and shows the explanation`() {
        coEvery { api.explainWhy(any(), any(), WhyRequest(4, "Voda vře při 100 °C")) } returns WhyResponse("Kvůli tlaku.")
        vm.setWhySentence(" Voda vře při 100 °C ")

        vm.submitWhy(4)

        assertEquals("Kvůli tlaku.", await { it.whyResult.isNotEmpty() }.whyResult)
    }

    @Test
    fun `why ignores a blank sentence`() {
        vm.submitWhy(4)

        coVerify(exactly = 0) { api.explainWhy(any(), any(), any()) }
    }

    @Test
    fun `rewrite register key calls the register endpoint`() {
        coEvery { api.rewriteForRegister(any(), any(), RewriteRegisterRequest(1, "simple")) } returns RewriteResponse("Jednodušeji.")

        vm.rewrite(1, "register:simple")

        val state = await { it.rewriteContent.isNotEmpty() }
        assertEquals("Jednodušeji.", state.rewriteContent)
        assertEquals("register:simple", state.rewriteSelected)
        coVerify(exactly = 0) { api.rewriteForGrade(any(), any(), any()) }
    }

    @Test
    fun `rewrite grade key calls the grade endpoint with the level`() {
        coEvery { api.rewriteForGrade(any(), any(), RewriteGradeRequest(1, 7)) } returns RewriteResponse("Pro sedmáky.")

        vm.rewrite(1, "grade:7")

        assertEquals("Pro sedmáky.", await { it.rewriteContent.isNotEmpty() }.rewriteContent)
        coVerify(exactly = 0) { api.rewriteForRegister(any(), any(), any()) }
    }

    @Test
    fun `rewrite failure keeps the selection so retry is possible`() {
        coEvery { api.rewriteForRegister(any(), any(), any()) } throws RuntimeException("boom")

        vm.rewrite(1, "register:expert")

        val state = await { it.error != null }
        assertEquals("register:expert", state.rewriteSelected)
        assertTrue(state.rewriteContent.isEmpty())
    }

    @Test
    fun `grades are sorted by level`() {
        coEvery { api.getGrades() } returns listOf(Grade(3, "9. třída", 9), Grade(1, "6. třída", 6), Grade(2, "7. třída", 7))

        vm.loadGrades()

        assertEquals(listOf(6, 7, 9), await { it.grades.isNotEmpty() }.grades.map { it.level })
    }

    @Test
    fun `formulas are loaded`() {
        coEvery { api.getFormulaVars(any(), any(), 8) } returns FormulaVarsResponse(listOf(FormulaEntry(formula = "F = m·a")))

        vm.loadFormulas(8)

        assertEquals("F = m·a", await { it.formulas.isNotEmpty() }.formulas.single().formula)
    }

    @Test
    fun `cross connections are loaded`() {
        coEvery { api.getCrossConnections(any(), any(), 9) } returns
            CrossConnectionResponse(listOf(CrossConnection("Fotosyntéza", "Biologie", "Souvisí s energií.")))

        vm.loadCrossConnections(9)

        assertEquals("Biologie", await { it.crossConnections.isNotEmpty() }.crossConnections.single().subject)
    }

    @Test
    fun `opening a tool resets the previous tool state`() {
        vm.setWhySentence("x")

        vm.openTool(AiTool.KEY_TERMS)

        val state = vm.state.value
        assertEquals(AiTool.KEY_TERMS, state.activeTool)
        assertEquals("", state.whySentence)
        assertNotNull(state)
    }

    @Test
    fun `step solver steps are loaded in order`() {
        coEvery { api.getStepSolver(any(), any(), 11) } returns
            StepSolverResponse(listOf(SolverStep("2x = 6", "Vydělíme dvěma"), SolverStep("x = 3", "Výsledek")))

        vm.loadStepSolver(11)

        assertEquals(listOf("2x = 6", "x = 3"), await { it.solverSteps.isNotEmpty() }.solverSteps.map { it.step })
    }

    @Test
    fun `step solver failure sets an error and no steps`() {
        coEvery { api.getStepSolver(any(), any(), any()) } throws RuntimeException("boom")

        vm.loadStepSolver(11)

        assertTrue(await { it.error != null }.solverSteps.isEmpty())
    }

    @Test
    fun `concept map nodes and edges are loaded`() {
        coEvery { api.getConceptMap(any(), any(), 12) } returns ConceptMapResponse(
            nodes = listOf(ConceptNode("1", "Atom"), ConceptNode("2", "Jádro")),
            edges = listOf(ConceptEdge("1", "2", "má"))
        )

        vm.loadConceptMap(12)

        val state = await { it.conceptNodes.isNotEmpty() }
        assertEquals(2, state.conceptNodes.size)
        assertEquals("má", state.conceptEdges.single().label)
    }

    @Test
    fun `concept map failure sets an error`() {
        coEvery { api.getConceptMap(any(), any(), any()) } throws RuntimeException("boom")

        vm.loadConceptMap(12)

        assertTrue(await { it.error != null }.conceptNodes.isEmpty())
    }
}
