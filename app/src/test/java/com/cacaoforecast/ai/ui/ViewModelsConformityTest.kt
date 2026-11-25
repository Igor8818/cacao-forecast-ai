package com.cacaoforecast.ai.ui

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cacaoforecast.ai.data.local.CacaoDatabase
import com.cacaoforecast.ai.data.repository.DefaultCacaoRepository
import com.cacaoforecast.ai.domain.ml.DetectedFruitBox
import com.cacaoforecast.ai.domain.ml.FruitAnalysisEngine
import com.cacaoforecast.ai.domain.ml.FruitHealthCategory
import com.cacaoforecast.ai.domain.ml.PlantPhotoAnalysisResult
import com.cacaoforecast.ai.domain.model.EvaluationStatus
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder
import com.cacaoforecast.ai.pdf.PdfReportGenerator
import com.cacaoforecast.ai.ui.farms.FarmsViewModel
import com.cacaoforecast.ai.ui.history.PlotHistoryViewModel
import com.cacaoforecast.ai.ui.reports.FarmReportViewModel
import com.cacaoforecast.ai.ui.sampling.SamplingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewModelsConformityTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: CacaoDatabase
    private lateinit var sessionsRoot: File
    private var currentClock = 1_000_000L
    private lateinit var repository: DefaultCacaoRepository

    private class FakeFruitEngine : FruitAnalysisEngine {
        var releaseCount = 0
        override suspend fun prepareSession(): Result<Unit> = Result.success(Unit)
        override suspend fun analyzePhoto(
            photoUri: Uri,
            countThreshold: Float,
            diseaseThreshold: Float,
            maxResults: Int
        ): Result<PlantPhotoAnalysisResult> = Result.success(
            PlantPhotoAnalysisResult(
                boxes = listOf(
                    DetectedFruitBox("box1", 0.1f, 0.1f, 0.2f, 0.2f, FruitHealthCategory.SADIO, 0.9f),
                    DetectedFruitBox("box2", 0.3f, 0.3f, 0.4f, 0.4f, FruitHealthCategory.MONILIASE, 0.85f)
                ),
                countHealthy = 1,
                countDiseased = 1,
                countPodridaoParda = 0,
                countMoniliase = 1,
                countVassouraBruxa = 0,
                imageWidthPx = 1600,
                imageHeightPx = 1200,
                inferenceTimeMs = 150L
            )
        )

        override fun releaseSession() {
            releaseCount++
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CacaoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionsRoot = File(context.cacheDir, "vm_conformity_sessions").apply {
            deleteRecursively()
            mkdirs()
        }
        repository = DefaultCacaoRepository(
            databaseProvider = { db },
            sessionDirProvider = { id -> File(sessionsRoot, "s_$id").apply { mkdirs() } },
            clockMillis = { currentClock }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
        sessionsRoot.deleteRecursively()
    }

    private suspend fun <T> Flow<T>.awaitValue(predicate: (T) -> Boolean): T =
        withTimeout(3_000L) { first(predicate) }

    @Test
    fun `FarmsViewModel valida campos obrigatorios e realiza CRUD completo de fazendas e rocas`() = runBlocking {
        val vm = FarmsViewModel(repository)

        // Bloqueia nome vazio
        vm.saveFarm(Farm(nome = "   "))
        val errMsg = vm.message.awaitValue { it != null }
        assertTrue(errMsg!!.contains("nome", ignoreCase = true))
        assertTrue(vm.farms.value.isEmpty())

        // Salva fazenda valida
        var saved = false
        vm.saveFarm(Farm(nome = "Fazenda Boa Vista", municipioUf = "Ilhéus/BA")) { saved = true }
        val farmsAfterCreate = vm.farms.awaitValue { it.size == 1 }
        assertTrue(saved)
        val farmId = farmsAfterCreate.single().id

        // Bloqueia quadra com area <= 0 e NPF fora de 1..500
        vm.clearMessage()
        var plotSaved = false
        vm.savePlot(Plot(fazendaId = farmId, nome = "Roça Inválida", areaHa = 0.0, npt = 1000, fa = 250.0, npf = 20)) {
            plotSaved = true
        }
        assertNotNull(vm.message.awaitValue { it != null })
        assertFalse(plotSaved)

        vm.clearMessage()
        vm.savePlot(Plot(fazendaId = farmId, nome = "Roça NPF Alto", areaHa = 2.0, npt = 1000, fa = 250.0, npf = 501)) {
            plotSaved = true
        }
        assertNotNull(vm.message.awaitValue { it != null })
        assertFalse(plotSaved)

        // Salva quadra valida
        vm.savePlot(Plot(fazendaId = farmId, nome = "Roça Sede", areaHa = 3.0, npt = 1500, fa = 250.0, npf = 20)) {
            plotSaved = true
        }
        val plots = vm.observePlots(farmId).awaitValue { it.size == 1 }
        assertTrue(plotSaved)
        assertEquals("Roça Sede", plots.single().nome)

        // Arquiva fazenda
        vm.archiveFarm(farmId)
        val farmsAfterArchive = vm.farms.awaitValue { it.isEmpty() }
        assertTrue(farmsAfterArchive.isEmpty())
    }

    @Test
    fun `SamplingViewModel realiza revisao rapida, ajustes mais menos, saveDraftAndExit e discardActiveSession`() = runBlocking {
        val farmId = repository.saveFarm(Farm(nome = "Fazenda Revisão")).getOrThrow()
        val plotId = repository.savePlot(
            Plot(fazendaId = farmId, nome = "Roça Baixada", areaHa = 2.0, npt = 1000, fa = 250.0, npf = 3)
        ).getOrThrow()
        val plot = repository.getPlotById(plotId)!!

        val fakeEngine = FakeFruitEngine()
        val vm = SamplingViewModel(
            repository = repository,
            fruitAnalysisEngine = fakeEngine,
            sessionDirProvider = { id -> File(sessionsRoot, "s_$id").apply { mkdirs() } }
        )

        var evalId = 0L
        vm.startNewSession(plot) { evalId = it }
        vm.uiState.awaitValue { it.activeDraft != null && !it.isPreparingEngine }
        assertTrue(evalId > 0L)

        val photo1 = vm.sessionPhotoFile(evalId, 1).apply { writeText("jpg") }
        vm.analyzeCapturedPhoto(evalId, 1, Uri.fromFile(photo1).toString())
        val analyzed = vm.uiState.awaitValue { !it.isAnalyzingPhoto && it.detectedBoxes.size == 2 }

        assertEquals(1, analyzed.reviewNfs)
        assertEquals(1, analyzed.reviewNfd)
        assertEquals(1, analyzed.reviewMoniliase)
        assertFalse(analyzed.manualEdited)

        // Toque na caixa Sadio -> promove para Podridao Parda
        vm.cycleDetectedBox("box1")
        assertEquals(0, vm.uiState.value.reviewNfs)
        assertEquals(2, vm.uiState.value.reviewNfd)
        assertEquals(1, vm.uiState.value.reviewPodridao)
        assertEquals(1, vm.uiState.value.reviewMoniliase)
        assertTrue(vm.uiState.value.manualEdited)
        assertTrue(vm.uiState.value.detectedBoxes.first { it.id == "box1" }.isManualOverride)

        // Segundo toque na mesma caixa -> remove
        vm.cycleDetectedBox("box1")
        assertEquals(0, vm.uiState.value.reviewNfs)
        assertEquals(1, vm.uiState.value.reviewNfd)
        assertEquals(0, vm.uiState.value.reviewPodridao)

        // Ajustes +/- respeitando piso zero
        vm.adjustHealthyCount(2)
        vm.adjustHealthyCount(-5)
        assertEquals(0, vm.uiState.value.reviewNfs)

        vm.adjustDiseasedCount(1)
        assertEquals(2, vm.uiState.value.reviewNfd)

        // Re-chamar analyzeCapturedPhoto com mesma URI nao sobrescreve edicao manual
        vm.analyzeCapturedPhoto(evalId, 1, Uri.fromFile(photo1).toString())
        assertEquals(2, vm.uiState.value.reviewNfd)
        assertTrue(vm.uiState.value.manualEdited)

        // clearCurrentPhotoReview limpa o estado antes de refazer foto
        vm.clearCurrentPhotoReview()
        assertNull(vm.uiState.value.currentPhotoUri)
        assertTrue(vm.uiState.value.detectedBoxes.isEmpty())
        assertFalse(vm.uiState.value.manualEdited)

        vm.adjustDiseasedCount(1)
        vm.adjustDiseasedCount(-10)
        assertEquals(0, vm.uiState.value.reviewNfd)
        assertEquals(0, vm.uiState.value.reviewPodridao)
        assertEquals(0, vm.uiState.value.reviewMoniliase)

        // Salvar rascunho e sair: mantem IN_PROGRESS e libera engine
        var exited = false
        vm.saveDraftAndExit { exited = true }
        assertTrue(exited)
        assertEquals(1, fakeEngine.releaseCount)
        assertEquals(EvaluationStatus.IN_PROGRESS, repository.getEvaluationById(evalId)!!.status)

        // Descartar sessao ativa: marca CANCELLED e libera engine
        var discarded = false
        vm.discardActiveSession { discarded = true }
        vm.uiState.awaitValue { it.activeDraft == null }
        assertTrue(discarded)
        assertEquals(2, fakeEngine.releaseCount)
        assertEquals(EvaluationStatus.CANCELLED, repository.getEvaluationById(evalId)!!.status)
    }

    @Test
    fun `PlotHistoryViewModel e FarmReportViewModel refletem producao real e filtros do Room`() = runBlocking {
        val farmId = repository.saveFarm(Farm(nome = "Fazenda Relatório")).getOrThrow()
        val plot1Id = repository.savePlot(
            Plot(fazendaId = farmId, nome = "Roça Norte", areaHa = 2.0, npt = 1000, fa = 250.0, npf = 1)
        ).getOrThrow()
        val plot2Id = repository.savePlot(
            Plot(fazendaId = farmId, nome = "Roça Sul", areaHa = 4.0, npt = 2000, fa = 250.0, npf = 1)
        ).getOrThrow()

        val p1 = repository.getPlotById(plot1Id)!!
        val p2 = repository.getPlotById(plot2Id)!!

        // Avaliacao p1: 18 sadios, 2 doentes -> 80 @ bruta, 72 @ liquida, 10% perda
        currentClock = 2_000_000L
        val d1 = repository.startNewSamplingSession(p1).getOrThrow()
        repository.savePlantSample(d1.evaluationId, 1, 18, 2, 2, 0, 0, false, null).getOrThrow()
        repository.finalizeSamplingSession(d1.evaluationId).getOrThrow()

        // Avaliacao p2: 15 sadios, 5 doentes (Moniliase) -> 160 @ bruta, 120 @ liquida, 25% perda
        currentClock = 2_500_000L
        val d2 = repository.startNewSamplingSession(p2).getOrThrow()
        repository.savePlantSample(d2.evaluationId, 1, 15, 5, 0, 5, 0, false, null).getOrThrow()
        repository.finalizeSamplingSession(d2.evaluationId).getOrThrow()

        // PlotHistoryViewModel: atualiza producao real
        val historyVm = PlotHistoryViewModel(plot1Id, repository)
        val initialHistory = historyVm.evaluations.awaitValue { it.size == 1 }
        assertNull(initialHistory.single().producaoRealArrobas)

        historyVm.updateRealProduction(d1.evaluationId, 38.5)
        val updatedHistory = historyVm.evaluations.awaitValue { it.singleOrNull()?.producaoRealArrobas != null }
        assertEquals(38.5, updatedHistory.single().producaoRealArrobas!!, 1e-6)

        // FarmReportViewModel: filtros e bloqueio de PDF quando vazio (sem depender de coletor em farms)
        val reportVm = FarmReportViewModel(repository, PdfReportGenerator(context))
        reportVm.selectFarm(farmId)
        reportVm.selectPeriod(1_000_000L, 3_000_000L, "Período Teste")

        val initialReport = reportVm.report.awaitValue {
            it != null && it.rows.size == 2 && it.periodLabel == "Período Teste"
        }!!
        assertEquals("Roça Sul", initialReport.rows.first().plotName) // LOSS_DESC padrao
        assertEquals(192.0, initialReport.footer.totalProducaoExibidaArrobas, 1e-6) // 72 + 120
        assertEquals(5, initialReport.footer.totalMoniliaseFruits)
        assertEquals(listOf("Roça Sul"), initialReport.footer.moniliaseAlertPlotNames)

        // Verifica Relatorio Cumulativo (Modo 2) vinculado a Roça Norte com producao real lancada
        reportVm.selectCumulativePlot(plot1Id)
        val cumulative = reportVm.cumulativeReport.awaitValue {
            it.plotId == plot1Id && it.points.size == 1 && it.evaluationsWithRealCount == 1
        }
        assertEquals(72.0, cumulative.totalEstimadaArrobas, 1e-6)
        assertEquals(38.5, cumulative.totalRealArrobas, 1e-6)

        // Alterna criterio para Bruto NFT e ordenacao por nome
        reportVm.selectBasis(ProductionBasis.GROSS_TOTAL_NFT)
        reportVm.selectSortOrder(ReportSortOrder.PLOT_NAME_ASC)
        val grossReport = reportVm.report.awaitValue {
            it != null && it.basis == ProductionBasis.GROSS_TOTAL_NFT && it.sortOrder == ReportSortOrder.PLOT_NAME_ASC
        }!!
        assertEquals(listOf("Roça Norte", "Roça Sul"), grossReport.rows.map { it.plotName })
        assertEquals(240.0, grossReport.footer.totalProducaoExibidaArrobas, 1e-6) // 80 + 160

        // Filtro por quadra unica
        reportVm.selectPlotFilter(plot1Id)
        val singlePlotReport = reportVm.report.awaitValue { it != null && it.rows.size == 1 }!!
        assertEquals(plot1Id, singlePlotReport.rows.single().plotId)

        // Periodo sem dados bloqueia geracao de PDF
        reportVm.selectPeriod(9_000_000L, 10_000_000L, "Vazio")
        reportVm.report.awaitValue { it != null && it.rows.isEmpty() }
        var pdfCalled = false
        reportVm.generatePdf { _, _ -> pdfCalled = true }
        assertFalse(pdfCalled)
        assertNotNull(reportVm.message.value)
    }
}
