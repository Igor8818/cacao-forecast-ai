package com.cacaoforecast.ai.domain.calculator

import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.DominantDisease
import com.cacaoforecast.ai.domain.model.EvaluationStatus
import com.cacaoforecast.ai.domain.model.EvaluationSummary
import com.cacaoforecast.ai.domain.model.EvaluationWithPeriodCount
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FarmReportConsolidatorTest {

    @Test
    fun `consolidado usa ponderacao exata por arrobas e area e nao media simples`() {
        val farm = Farm(id = 1L, nome = "Fazenda Esperança")
        val plotA = Plot(id = 10L, fazendaId = 1L, nome = "Roça Pequena", areaHa = 1.0, npt = 500, fa = 250.0, npf = 20)
        val plotB = Plot(id = 20L, fazendaId = 1L, nome = "Roça Grande", areaHa = 9.0, npt = 9000, fa = 250.0, npf = 20)
        val plotC = Plot(id = 30L, fazendaId = 1L, nome = "Roça Pendente", areaHa = 2.0, npt = 1200, fa = 250.0, npf = 20)

        // Roça Pequena: 100 @ bruta, 50 @ liquida -> 50% perda
        val evalA = eval(
            id = 1L, plot = plotA, nfs = 50, nfd = 50,
            bruta = 100.0, liquida = 50.0, perdasPct = 50.0,
            dominant = DominantDisease.VASSOURA_BRUXA
        )
        // Roça Grande: 900 @ bruta, 810 @ liquida -> 10% perda
        val evalB = eval(
            id = 2L, plot = plotB, nfs = 180, nfd = 20,
            bruta = 900.0, liquida = 810.0, perdasPct = 10.0,
            dominant = DominantDisease.PODRIDAO_PARDA
        )

        val report = FarmReportConsolidator.buildReport(
            farm = farm,
            allActivePlots = listOf(plotA, plotB, plotC),
            deduplicatedEvaluations = listOf(
                EvaluationWithPeriodCount(evalA, 1),
                EvaluationWithPeriodCount(evalB, 2)
            ),
            periodLabel = "Nov/2026",
            startMillis = 0L,
            endMillis = 1000L,
            selectedPlotId = null,
            basis = ProductionBasis.NET_HEALTHY_NFS,
            sortOrder = ReportSortOrder.LOSS_DESC
        )

        // Ordenacao padrao: maior perda no topo
        assertEquals("Roça Pequena", report.rows.first().plotName)
        assertEquals("Roça Pequena", report.criticalHighlightRow?.plotName)
        assertEquals(listOf("Roça Pendente"), report.footer.pendingPlotNames)
        assertEquals(2, report.footer.totalPlotsEvaluated)
        assertEquals(3, report.footer.totalActivePlotsInFarm)

        // Soma de area e plantas exclui Roça Pendente (1 + 9 = 10 ha; 500 + 9000 = 9500)
        assertEquals(10.0, report.footer.totalAreaHa, 1e-6)
        assertEquals(9500, report.footer.totalNpt)

        // Producao Liquida Consolidada: 50 + 810 = 860 @ -> 860 / 10 ha = 86 @/ha
        assertEquals(860.0, report.footer.totalProducaoExibidaArrobas, 1e-6)
        assertEquals(86.0, report.footer.produtividadeConsolidadaPorHa, 1e-6)

        // Perda ponderada: (50 + 90) / (100 + 900) * 100 = 140 / 1000 * 100 = 14.0%
        // Media simples seria (50% + 10%) / 2 = 30.0% (errado!)
        assertEquals(140.0, report.footer.totalPerdaArrobas, 1e-6)
        assertEquals(14.0, report.footer.perdasPonderadasPct, 1e-6)
        assertNotEquals(30.0, report.footer.perdasPonderadasPct, 1e-3)
        assertEquals(DiseaseSeverity.WARNING, report.footer.severity)
    }

    @Test
    fun `alternar criterio para bruto NFT exibe producao bruta mantendo perda ponderada consistente`() {
        val farm = Farm(id = 1L, nome = "Fazenda Cacau")
        val plot = Plot(id = 10L, fazendaId = 1L, nome = "Roça Sede", areaHa = 4.0, npt = 2000, fa = 250.0, npf = 20)
        val eval = eval(
            id = 1L, plot = plot, nfs = 75, nfd = 25,
            bruta = 200.0, liquida = 150.0, perdasPct = 25.0,
            dominant = DominantDisease.MONILIASE
        )

        val report = FarmReportConsolidator.buildReport(
            farm = farm,
            allActivePlots = listOf(plot),
            deduplicatedEvaluations = listOf(EvaluationWithPeriodCount(eval, 1)),
            periodLabel = "Nov/2026",
            startMillis = 0L,
            endMillis = 1000L,
            selectedPlotId = null,
            basis = ProductionBasis.GROSS_TOTAL_NFT,
            sortOrder = ReportSortOrder.LOSS_DESC
        )

        assertEquals(200.0, report.rows.single().producaoExibidaArrobas, 1e-6)
        assertEquals(50.0, report.rows.single().produtividadeExibidaPorHa, 1e-6)
        assertEquals(200.0, report.footer.totalProducaoExibidaArrobas, 1e-6)
        assertEquals(50.0, report.footer.produtividadeConsolidadaPorHa, 1e-6)
        assertEquals(25.0, report.footer.perdasPonderadasPct, 1e-6)
    }

    private fun eval(
        id: Long,
        plot: Plot,
        nfs: Int,
        nfd: Int,
        bruta: Double,
        liquida: Double,
        perdasPct: Double,
        dominant: DominantDisease
    ): EvaluationSummary {
        return EvaluationSummary(
            id = id,
            farmId = plot.fazendaId,
            plotId = plot.id,
            status = EvaluationStatus.COMPLETED,
            snapshotPlotName = plot.nome,
            snapshotAreaHa = plot.areaHa,
            snapshotNpt = plot.npt,
            snapshotFa = plot.fa,
            snapshotNpf = plot.npf,
            nPlantasAmostradas = plot.npf,
            nfs = nfs,
            nfd = nfd,
            nPodridao = if (dominant == DominantDisease.PODRIDAO_PARDA) nfd else 0,
            nMoniliase = if (dominant == DominantDisease.MONILIASE) nfd else 0,
            nVassoura = if (dominant == DominantDisease.VASSOURA_BRUXA) nfd else 0,
            producaoBrutaArrobas = bruta,
            producaoLiquidaArrobas = liquida,
            produtividadeBrutaPorHa = bruta / plot.areaHa,
            produtividadeLiquidaPorHa = liquida / plot.areaHa,
            perdasPct = perdasPct,
            severity = ForecastCalculator.classifySeverity(perdasPct),
            dominantDisease = dominant,
            producaoRealArrobas = null,
            producaoRealEmMillis = null,
            iniciadoEmMillis = 100L,
            dataMillis = 200L
        )
    }
}
