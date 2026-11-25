package com.cacaoforecast.ai.ui.sampling

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class SamplingSessionStateTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: CacaoDatabase
    private lateinit var sessionsRoot: File
    private lateinit var repository: DefaultCacaoRepository
    private val fakeEngine = object : FruitAnalysisEngine {
        var releasedCount = 0
        override suspend fun prepareSession(): Result<Unit> = Result.success(Unit)
        override suspend fun analyzePhoto(
            photoUri: Uri,
            countThreshold: Float,
            diseaseThreshold: Float,
            maxResults: Int
        ): Result<PlantPhotoAnalysisResult> {
            return Result.success(
                PlantPhotoAnalysisResult(
                    boxes = listOf(
                        DetectedFruitBox("1", 0.1f, 0.1f, 0.2f, 0.2f, FruitHealthCategory.SADIO, 0.9f),
                        DetectedFruitBox("2", 0.3f, 0.3f, 0.4f, 0.4f, FruitHealthCategory.PODRIDAO_PARDA, 0.8f)
                    ),
                    countHealthy = 1,
                    countDiseased = 1,
                    countPodridaoParda = 1,
                    countMoniliase = 0,
                    countVassouraBruxa = 0,
                    imageWidthPx = 1600,
                    imageHeightPx = 1200,
                    inferenceTimeMs = 100L
                )
            )
        }

        override fun releaseSession() {
            releasedCount++
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, CacaoDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        sessionsRoot = File(context.cacheDir, "sampling_state_test").apply {
            deleteRecursively()
            mkdirs()
        }
        repository = DefaultCacaoRepository(
            databaseProvider = { db },
            sessionDirProvider = { evalId -> File(sessionsRoot, "session_$evalId").apply { mkdirs() } }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
        sessionsRoot.deleteRecursively()
    }

    @Test
    fun `bloqueia finalizacao antes de NPF, retoma exatamente em k mais 1, sela COMPLETED e remove fotos`() = runTest(testDispatcher) {
        val farmId = repository.saveFarm(Farm(nome = "Fazenda Retomada")).getOrThrow()
        val plotId = repository.savePlot(
            Plot(fazendaId = farmId, nome = "Roça Ladeira", areaHa = 2.0, npt = 1000, fa = 250.0, npf = 2)
        ).getOrThrow()
        val plot = repository.getPlotById(plotId)!!

        val vm1 = SamplingViewModel(
            repository = repository,
            fruitAnalysisEngine = fakeEngine,
            sessionDirProvider = { evalId -> File(sessionsRoot, "session_$evalId").apply { mkdirs() } }
        )

        var createdEvalId = 0L
        vm1.startNewSession(plot) { createdEvalId = it }
        advanceUntilIdle()
        assertTrue(createdEvalId > 0L)
        assertFalse(vm1.uiState.value.canFinalize)

        // Foto 1/2
        val photo1 = vm1.sessionPhotoFile(createdEvalId, 1).apply { writeText("fake-jpeg-1") }
        vm1.analyzeCapturedPhoto(createdEvalId, 1, Uri.fromFile(photo1).toString())
        advanceUntilIdle()

        var wentNext = false
        var completedEarly = false
        vm1.confirmCurrentPlant(onNextPlant = { wentNext = true }, onCompleted = { completedEarly = true })
        advanceUntilIdle()

        assertTrue(wentNext)
        assertFalse(completedEarly)
        assertFalse(vm1.uiState.value.canFinalize)

        // Tentativa manual de finalizar em 1/2 deve falhar
        vm1.finalizeIfReady(createdEvalId) { completedEarly = true }
        advanceUntilIdle()
        assertFalse(completedEarly)
        assertNotNull(vm1.uiState.value.errorMessage)

        // Simula crash / reinicio com novo ViewModel
        val vm2 = SamplingViewModel(
            repository = repository,
            fruitAnalysisEngine = fakeEngine,
            sessionDirProvider = { evalId -> File(sessionsRoot, "session_$evalId").apply { mkdirs() } }
        )
        vm2.initializeSetup(farmId, plotId)
        advanceUntilIdle()

        val draft = vm2.uiState.value.pendingResumeDraft
        assertNotNull(draft)
        assertEquals(1, draft!!.sampledCount)
        assertEquals(2, draft.nextPlantIndex)

        vm2.resumeSession(createdEvalId)
        advanceUntilIdle()

        // Foto 2/2 -> finaliza automaticamente e remove diretorio temporario
        val photo2 = vm2.sessionPhotoFile(createdEvalId, 2).apply { writeText("fake-jpeg-2") }
        vm2.analyzeCapturedPhoto(createdEvalId, 2, Uri.fromFile(photo2).toString())
        advanceUntilIdle()

        var finishedEvalId = 0L
        vm2.confirmCurrentPlant(onNextPlant = {}, onCompleted = { finishedEvalId = it })
        advanceUntilIdle()

        assertEquals(createdEvalId, finishedEvalId)
        val saved = repository.getEvaluationById(createdEvalId)!!
        assertEquals(EvaluationStatus.COMPLETED, saved.status)
        assertEquals(2, saved.nPlantasAmostradas)
        assertFalse(File(sessionsRoot, "session_$createdEvalId").exists())
    }
}
