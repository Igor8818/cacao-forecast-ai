package com.cacaoforecast.ai.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cacaoforecast.ai.data.local.CacaoDatabase
import com.cacaoforecast.ai.domain.model.EvaluationStatus
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SnapshotAndDeduplicationTest {

    private lateinit var context: Context
    private lateinit var db: CacaoDatabase
    private lateinit var tempSessionsRoot: File
    private var currentClock = 1_000_000L
    private lateinit var repository: DefaultCacaoRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CacaoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tempSessionsRoot = File(context.cacheDir, "test_sessions").apply {
            deleteRecursively()
            mkdirs()
        }
        repository = DefaultCacaoRepository(
            databaseProvider = { db },
            sessionDirProvider = { evalId -> File(tempSessionsRoot, "eval_$evalId") },
            clockMillis = { currentClock }
        )
    }

    @After
    fun tearDown() {
        db.close()
        tempSessionsRoot.deleteRecursively()
    }

    @Test
    fun `alterar cadastro de A NPT FA na quadra nao altera avaliacoes passadas`() = runBlocking {
        val farmId = repository.saveFarm(Farm(nome = "Fazenda São José")).getOrThrow()
        val plotId = repository.savePlot(
            Plot(fazendaId = farmId, nome = "Roça Velha", areaHa = 2.0, npt = 1000, fa = 250.0, npf = 2)
        ).getOrThrow()
        val plot = repository.getPlotById(plotId)!!

        val draft = repository.startNewSamplingSession(plot).getOrThrow()
        repository.savePlantSample(draft.evaluationId, 1, nfs = 9, nfd = 1, 1, 0, 0, false, null).getOrThrow()
        repository.savePlantSample(draft.evaluationId, 2, nfs = 9, nfd = 1, 1, 0, 0, false, null).getOrThrow()
        currentClock = 2_000_000L
        repository.finalizeSamplingSession(draft.evaluationId).getOrThrow()

        // Edita parametros da quadra posteriormente (replantio + retificacao de area + FA)
        currentClock = 3_000_000L
        repository.savePlot(
            plot.copy(nome = "Roça Replantada", areaHa = 5.0, npt = 4000, fa = 200.0, npf = 20)
        ).getOrThrow()

        val pastEval = repository.getEvaluationById(draft.evaluationId)!!
        assertEquals("Roça Velha", pastEval.snapshotPlotName)
        assertEquals(2.0, pastEval.snapshotAreaHa, 1e-6)
        assertEquals(1000, pastEval.snapshotNpt)
        assertEquals(250.0, pastEval.snapshotFa, 1e-6)
        assertEquals(2, pastEval.snapshotNpf)
        // NFS = 18, NFD = 2, NFT = 20, NPF = 2, NPT = 1000, FA = 250 -> 40.0 @ bruta, 36.0 @ liquida
        assertEquals(40.0, pastEval.producaoBrutaArrobas, 1e-6)
        assertEquals(36.0, pastEval.producaoLiquidaArrobas, 1e-6)
    }

    @Test
    fun `avaliacoes IN_PROGRESS e CANCELLED nunca vazam para relatorio nem historico`() = runBlocking {
        val farmId = repository.saveFarm(Farm(nome = "Fazenda Rio Novo")).getOrThrow()
        val plotId = repository.savePlot(
            Plot(fazendaId = farmId, nome = "Roça Beira Rio", areaHa = 3.0, npt = 1500, fa = 250.0, npf = 3)
        ).getOrThrow()
        val plot = repository.getPlotById(plotId)!!

        // Sessao em andamento (1 de 3 plantas)
        val inProgress = repository.startNewSamplingSession(plot).getOrThrow()
        repository.savePlantSample(inProgress.evaluationId, 1, nfs = 10, nfd = 5, 5, 0, 0, false, null).getOrThrow()

        val report = repository.observeFarmReport(
            farmId = farmId,
            startMillis = 0L,
            endMillis = 10_000_000L,
            selectedPlotId = null,
            basis = ProductionBasis.NET_HEALTHY_NFS,
            sortOrder = ReportSortOrder.LOSS_DESC
        ).first()

        assertTrue(report.rows.isEmpty())
        assertEquals(0, report.footer.totalPlotsEvaluated)
        assertEquals(listOf("Roça Beira Rio"), report.footer.pendingPlotNames)
        assertTrue(repository.observeCompletedEvaluationsByPlot(plotId).first().isEmpty())

        // Tentar finalizar antes de NPF deve falhar
        val earlyFinalize = repository.finalizeSamplingSession(inProgress.evaluationId)
        assertTrue(earlyFinalize.isFailure)
    }

    @Test
    fun `duas avaliacoes COMPLETED da mesma quadra no periodo retornam estritamente a mais recente`() = runBlocking {
        val farmId = repository.saveFarm(Farm(nome = "Fazenda Duplicidade")).getOrThrow()
        val plotId = repository.savePlot(
            Plot(fazendaId = farmId, nome = "Roça Cacau Fino", areaHa = 4.0, npt = 2000, fa = 250.0, npf = 2)
        ).getOrThrow()
        val plot = repository.getPlotById(plotId)!!

        // Avaliacao 1 (dia 05): 50% perda -> 40 @ bruta, 20 @ liquida
        currentClock = 1_000_000L
        val eval1 = repository.startNewSamplingSession(plot).getOrThrow()
        repository.savePlantSample(eval1.evaluationId, 1, nfs = 5, nfd = 5, 5, 0, 0, false, null).getOrThrow()
        repository.savePlantSample(eval1.evaluationId, 2, nfs = 5, nfd = 5, 5, 0, 0, false, null).getOrThrow()
        currentClock = 1_500_000L
        repository.finalizeSamplingSession(eval1.evaluationId).getOrThrow()

        // Avaliacao 2 (dia 25): 10% perda -> 80 @ bruta, 72 @ liquida
        currentClock = 2_000_000L
        val eval2 = repository.startNewSamplingSession(plot).getOrThrow()
        repository.savePlantSample(eval2.evaluationId, 1, nfs = 9, nfd = 1, 0, 0, 1, false, null).getOrThrow()
        repository.savePlantSample(eval2.evaluationId, 2, nfs = 9, nfd = 1, 0, 0, 1, false, null).getOrThrow()
        currentClock = 2_500_000L
        repository.finalizeSamplingSession(eval2.evaluationId).getOrThrow()

        val report = repository.observeFarmReport(
            farmId = farmId,
            startMillis = 1_000_000L,
            endMillis = 3_000_000L,
            selectedPlotId = null,
            basis = ProductionBasis.NET_HEALTHY_NFS,
            sortOrder = ReportSortOrder.LOSS_DESC
        ).first()

        assertEquals(1, report.rows.size)
        val row = report.rows.single()
        assertEquals(eval2.evaluationId, row.evaluationId)
        assertEquals(2, row.evaluationsCountInPeriod)
        assertEquals(10.0, row.perdasPct, 1e-6)
        assertEquals(72.0, row.producaoExibidaArrobas, 1e-6)
        assertEquals(4.0, report.footer.totalAreaHa, 1e-6)
        assertEquals(2000, report.footer.totalNpt)
        assertEquals(72.0, report.footer.totalProducaoExibidaArrobas, 1e-6)
        assertEquals(EvaluationStatus.COMPLETED, repository.getEvaluationById(eval1.evaluationId)!!.status)
    }
}
