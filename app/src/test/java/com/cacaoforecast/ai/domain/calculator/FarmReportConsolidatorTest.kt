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

    @Test
    fun `suporta ordenacoes LOSS_DESC PRODUCTION_DESC e PLOT_NAME_ASC`() {
        val farm = Farm(id = 1L, nome = "Fazenda Ordenação")
        val plotZ = Plot(id = 1L, fazendaId = 1L, nome = "Zebra", areaHa = 2.0, npt = 1000, fa = 250.0)
        val plotA = Plot(id = 2L, fazendaId = 1L, nome = "Alvorada", areaHa = 2.0, npt = 1000, fa = 250.0)
        val plotM = Plot(id = 3L, fazendaId = 1L, nome = "Meio", areaHa = 2.0, npt = 1000, fa = 250.0)

        val evals = listOf(
            EvaluationWithPeriodCount(eval(1L, plotZ, 90, 10, bruta = 100.0, liquida = 90.0, perdasPct = 10.0, DominantDisease.PODRIDAO_PARDA), 1),
            EvaluationWithPeriodCount(eval(2L, plotA, 50, 50, bruta = 80.0, liquida = 40.0, perdasPct = 50.0, DominantDisease.VASSOURA_BRUXA), 1),
            EvaluationWithPeriodCount(eval(3L, plotM, 190, 10, bruta = 200.0, liquida = 190.0, perdasPct = 5.0, DominantDisease.MONILIASE), 1)
        )

        val byLoss = FarmReportConsolidator.buildReport(
            farm, listOf(plotZ, plotA, plotM), evals, "Nov/2026", 0L, 1000L, null,
            ProductionBasis.NET_HEALTHY_NFS, ReportSortOrder.LOSS_DESC
        )
        assertEquals(listOf("Alvorada", "Zebra", "Meio"), byLoss.rows.map { it.plotName })

        val byProd = FarmReportConsolidator.buildReport(
            farm, listOf(plotZ, plotA, plotM), evals, "Nov/2026", 0L, 1000L, null,
            ProductionBasis.NET_HEALTHY_NFS, ReportSortOrder.PRODUCTION_DESC
        )
        assertEquals(listOf("Meio", "Zebra", "Alvorada"), byProd.rows.map { it.plotName })

        val byName = FarmReportConsolidator.buildReport(
            farm, listOf(plotZ, plotA, plotM), evals, "Nov/2026", 0L, 1000L, null,
            ProductionBasis.NET_HEALTHY_NFS, ReportSortOrder.PLOT_NAME_ASC
        )
        assertEquals(listOf("Alvorada", "Meio", "Zebra"), byName.rows.map { it.plotName })
    }

    @Test
    fun `filtro por quadra unica e exclusao de quadras arquivadas ajustam rodape corretamente`() {
        val farm = Farm(id = 1L, nome = "Fazenda Filtro")
        val plot1 = Plot(id = 1L, fazendaId = 1L, nome = "Roça 1", areaHa = 2.0, npt = 1000, fa = 250.0, ativo = true)
        val plot2 = Plot(id = 2L, fazendaId = 1L, nome = "Roça 2", areaHa = 3.0, npt = 1500, fa = 250.0, ativo = true)
        val archivedPlot = Plot(id = 3L, fazendaId = 1L, nome = "Roça Arquivada", areaHa = 5.0, npt = 2500, fa = 250.0, ativo = false)

        val evals = listOf(
            EvaluationWithPeriodCount(eval(1L, plot1, 90, 10, 100.0, 90.0, 10.0, DominantDisease.PODRIDAO_PARDA), 1),
            EvaluationWithPeriodCount(eval(2L, plot2, 70, 30, 100.0, 70.0, 30.0, DominantDisease.VASSOURA_BRUXA), 1)
        )

        val singlePlotReport = FarmReportConsolidator.buildReport(
            farm = farm,
            allActivePlots = listOf(plot1, plot2, archivedPlot),
            deduplicatedEvaluations = evals,
            periodLabel = "Nov/2026",
            startMillis = 0L,
            endMillis = 1000L,
            selectedPlotId = 1L,
            basis = ProductionBasis.NET_HEALTHY_NFS,
            sortOrder = ReportSortOrder.LOSS_DESC
        )

        assertEquals(1, singlePlotReport.rows.size)
        assertEquals(1L, singlePlotReport.rows.single().plotId)
        assertEquals(1, singlePlotReport.footer.totalPlotsEvaluated)
        assertEquals(1, singlePlotReport.footer.totalActivePlotsInFarm)
        assertEquals(emptyList<String>(), singlePlotReport.footer.pendingPlotNames)
        assertEquals(2.0, singlePlotReport.footer.totalAreaHa, 1e-6)
        assertEquals(1000, singlePlotReport.footer.totalNpt)
        assertEquals(90.0, singlePlotReport.footer.totalProducaoExibidaArrobas, 1e-6)
        assertEquals(10.0, singlePlotReport.footer.perdasPonderadasPct, 1e-6)

        // Sem filtro de quadra, quadra arquivada nao entra nas pendentes
        val allPlotsReport = FarmReportConsolidator.buildReport(
            farm = farm,
            allActivePlots = listOf(plot1, archivedPlot),
            deduplicatedEvaluations = listOf(evals.first()),
            periodLabel = "Nov/2026",
            startMillis = 0L,
            endMillis = 1000L,
            selectedPlotId = null,
            basis = ProductionBasis.NET_HEALTHY_NFS,
            sortOrder = ReportSortOrder.LOSS_DESC
        )
        assertEquals(1, allPlotsReport.footer.totalActivePlotsInFarm)
        assertEquals(emptyList<String>(), allPlotsReport.footer.pendingPlotNames)
    }

    @Test
    fun `relatorio sem avaliacoes no periodo retorna zeros sem NaN e lista todas rocas ativas pendentes`() {
        val farm = Farm(id = 1L, nome = "Fazenda Vazia")
        val plotB = Plot(id = 2L, fazendaId = 1L, nome = "Roça B", areaHa = 2.0, npt = 1000, fa = 250.0)
        val plotA = Plot(id = 1L, fazendaId = 1L, nome = "Roça A", areaHa = 3.0, npt = 1500, fa = 250.0)

        val report = FarmReportConsolidator.buildReport(
            farm = farm,
            allActivePlots = listOf(plotB, plotA),
            deduplicatedEvaluations = emptyList(),
            periodLabel = "Nov/2026",
            startMillis = 0L,
            endMillis = 1000L,
            selectedPlotId = null,
            basis = ProductionBasis.NET_HEALTHY_NFS,
            sortOrder = ReportSortOrder.LOSS_DESC
        )

        assertEquals(0, report.rows.size)
        assertEquals(null, report.criticalHighlightRow)
        assertEquals(listOf("Roça A", "Roça B"), report.footer.pendingPlotNames)
        assertEquals(0.0, report.footer.totalAreaHa, 1e-9)
        assertEquals(0.0, report.footer.produtividadeConsolidadaPorHa, 1e-9)
        assertEquals(0.0, report.footer.perdasPonderadasPct, 1e-9)
        assertEquals(DiseaseSeverity.CONTROLLED, report.footer.severity)
        assertEquals(0, report.footer.totalMoniliaseFruits)
        assertEquals(emptyList<String>(), report.footer.moniliaseAlertPlotNames)
    }

    @Test
    fun `consolida alerta quarentenario de moniliase com total de frutos e rocas afetadas`() {
        val farm = Farm(id = 1L, nome = "Fazenda Quarentena")
        val plotA = Plot(id = 1L, fazendaId = 1L, nome = "Roça Baixa", areaHa = 2.0, npt = 1000, fa = 250.0)
        val plotB = Plot(id = 2L, fazendaId = 1L, nome = "Roça Alta", areaHa = 3.0, npt = 1500, fa = 250.0)

        val evalA = eval(1L, plotA, 95, 5, 100.0, 95.0, 5.0, DominantDisease.MONILIASE).copy(nMoniliase = 3)
        val evalB = eval(2L, plotB, 90, 10, 100.0, 90.0, 10.0, DominantDisease.PODRIDAO_PARDA)

        val report = FarmReportConsolidator.buildReport(
            farm = farm,
            allActivePlots = listOf(plotA, plotB),
            deduplicatedEvaluations = listOf(
                EvaluationWithPeriodCount(evalA, 1),
                EvaluationWithPeriodCount(evalB, 1)
            ),
            periodLabel = "Nov/2026",
            startMillis = 0L,
            endMillis = 1000L,
            selectedPlotId = null,
            basis = ProductionBasis.NET_HEALTHY_NFS,
            sortOrder = ReportSortOrder.LOSS_DESC
        )

        assertEquals(3, report.footer.totalMoniliaseFruits)
        assertEquals(listOf("Roça Baixa"), report.footer.moniliaseAlertPlotNames)
        assertEquals(3, report.rows.first { it.plotId == 1L }.nMoniliase)
        assertEquals(185, report.footer.totalHealthyFruits)
        assertEquals(10, report.footer.totalPodridaoPardaFruits)
        assertEquals(0, report.footer.totalVassouraBruxaFruits)
        assertEquals(200.0, report.footer.totalBrutaArrobas, 1e-6)
        assertEquals(185.0, report.footer.totalLiquidaArrobas, 1e-6)
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
