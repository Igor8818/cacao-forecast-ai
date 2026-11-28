package com.cacaoforecast.ai.data.demo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cacaoforecast.ai.data.local.CacaoDatabase
import com.cacaoforecast.ai.data.repository.DefaultCacaoRepository
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
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DemoSeedManagerTest {

    private lateinit var context: Context
    private lateinit var db: CacaoDatabase
    private lateinit var sessionsRoot: File
    private lateinit var repository: DefaultCacaoRepository
    private lateinit var seeder: DemoSeedManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CacaoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionsRoot = File(context.cacheDir, "demo_seed_sessions").apply {
            deleteRecursively()
            mkdirs()
        }
        repository = DefaultCacaoRepository(
            databaseProvider = { db },
            sessionDirProvider = { id -> File(sessionsRoot, "s_$id").apply { mkdirs() } }
        )
        seeder = DemoSeedManager { db }
    }

    @After
    fun tearDown() {
        db.close()
        sessionsRoot.deleteRecursively()
    }

    @Test
    fun `seedSempreVivaDemo gera numeros exatos do fluxograma e substitui apenas Sempre Viva com idempotencia`() = runBlocking {
        // Cria uma fazenda pre-existente que nao pode ser apagada
        repository.saveFarm(com.cacaoforecast.ai.domain.model.Farm(nome = "Fazenda Original"))

        val refCal = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 24, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val refNow = refCal.timeInMillis

        // Executa duas vezes seguidas para validar idempotencia
        assertTrue(seeder.seedSempreVivaDemo(refNow).isSuccess)
        assertTrue(seeder.seedSempreVivaDemo(refNow).isSuccess)

        val farms = repository.observeActiveFarms().first()
        assertEquals(2, farms.size)
        assertTrue(farms.any { it.nome == "Fazenda Original" })
        val sempreViva = farms.single { it.nome == "Sempre Viva" }

        val plots = repository.observeActivePlotsByFarm(sempreViva.id).first()
        assertEquals(3, plots.size)

        // Relatorio Parcial em Dez/2026 na base Potencial Bruto (NFT)
        val monthStart = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthEnd = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val report = repository.observeFarmReport(
            farmId = sempreViva.id,
            startMillis = monthStart,
            endMillis = monthEnd,
            selectedPlotId = null,
            basis = ProductionBasis.GROSS_TOTAL_NFT,
            sortOrder = ReportSortOrder.PLOT_NAME_ASC,
            periodLabel = "Dez/2026"
        ).first()

        assertEquals(3, report.rows.size)
        val byName = report.rows.associateBy { it.plotName }

        val topo = byName.getValue("Topo do Morro")
        assertEquals(3.0, topo.areaHa, 1e-6)
        assertEquals(2500, topo.npt)
        assertEquals(300.0, topo.producaoExibidaArrobas, 1e-4)
        assertEquals(100.0, topo.produtividadeExibidaPorHa, 1e-4)
        assertEquals(5.0, topo.perdasPct, 1e-4)

        val jatium = byName.getValue("Jatium")
        assertEquals(5.0, jatium.areaHa, 1e-6)
        assertEquals(4000, jatium.npt)
        assertEquals(400.0, jatium.producaoExibidaArrobas, 1e-4)
        assertEquals(80.0, jatium.produtividadeExibidaPorHa, 1e-4)
        assertEquals(0.0, jatium.perdasPct, 1e-4)

        val peroto = byName.getValue("Peroto do Rio")
        assertEquals(1.0, peroto.areaHa, 1e-6)
        assertEquals(600, peroto.npt)
        assertEquals(80.0, peroto.producaoExibidaArrobas, 1e-4)
        assertEquals(80.0, peroto.produtividadeExibidaPorHa, 1e-4)
        assertEquals(8.0, peroto.perdasPct, 1e-4)

        // Curva cumulativa de 6 pontos em Topo do Morro
        val topoHistory = repository.observeCompletedEvaluationsByPlot(topo.plotId).first()
        assertEquals(6, topoHistory.size)
        assertTrue(topoHistory.all { it.producaoRealArrobas != null })
    }
}