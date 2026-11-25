package com.cacaoforecast.ai.domain.calculator

import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.DominantDisease
import com.cacaoforecast.ai.domain.model.EvaluationStatus
import com.cacaoforecast.ai.domain.model.EvaluationSummary
import com.cacaoforecast.ai.domain.model.ProductionBasis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CumulativeStatsCalculatorTest {

    @Test
    fun `ordena cronologicamente calcula delta de perdas totais e erro relativo medio`() {
        val eval2 = summary(
            id = 2L,
            dataMillis = 2_000L,
            bruta = 120.0,
            liquida = 108.0,
            perdasPct = 10.0,
            real = 100.0, // erro relativo liquida: |108 - 100| / 100 = 8%
            nMoniliase = 0
        )
        val eval1 = summary(
            id = 1L,
            dataMillis = 1_000L,
            bruta = 100.0,
            liquida = 80.0,
            perdasPct = 20.0,
            real = 100.0, // erro relativo liquida: |80 - 100| / 100 = 20%
            nMoniliase = 2
        )
        val eval3 = summary(
            id = 3L,
            dataMillis = 3_000L,
            bruta = 90.0,
            liquida = 85.5,
            perdasPct = 5.0,
            real = null,
            nMoniliase = 0
        )

        val netReport = CumulativeStatsCalculator.build(
            plotId = 10L,
            evaluations = listOf(eval3, eval1, eval2),
            basis = ProductionBasis.NET_HEALTHY_NFS
        )

        assertEquals(listOf(1L, 2L, 3L), netReport.points.map { it.evaluationId })
        assertNull(netReport.points[0].deltaPerdasPctVsAnterior)
        assertEquals(-10.0, netReport.points[1].deltaPerdasPctVsAnterior!!, 1e-6)
        assertEquals(-5.0, netReport.points[2].deltaPerdasPctVsAnterior!!, 1e-6)
        assertEquals(-5.0, netReport.latestLossDeltaPct!!, 1e-6)
        assertEquals(2, netReport.points[0].nMoniliase)

        assertEquals(80.0 + 108.0 + 85.5, netReport.totalEstimadaArrobas, 1e-6)
        assertEquals(200.0, netReport.totalRealArrobas, 1e-6)
        assertEquals(2, netReport.evaluationsWithRealCount)
        assertEquals(14.0, netReport.meanRelativeErrorPct!!, 1e-6) // media(20%, 8%)

        val grossReport = CumulativeStatsCalculator.build(
            plotId = 10L,
            evaluations = listOf(eval1, eval2),
            basis = ProductionBasis.GROSS_TOTAL_NFT
        )
        assertEquals(220.0, grossReport.totalEstimadaArrobas, 1e-6)
        assertEquals(10.0, grossReport.meanRelativeErrorPct!!, 1e-6) // media(0%, 20%)
    }

    @Test
    fun `protege contra lista vazia e producao real zero sem NaN`() {
        val empty = CumulativeStatsCalculator.build(null, emptyList(), ProductionBasis.NET_HEALTHY_NFS)
        assertTrue(empty.points.isEmpty())
        assertEquals(0.0, empty.totalEstimadaArrobas, 1e-9)
        assertEquals(0.0, empty.totalRealArrobas, 1e-9)
        assertNull(empty.meanRelativeErrorPct)
        assertNull(empty.latestLossDeltaPct)

        val zeroReal = summary(
            id = 1L,
            dataMillis = 1_000L,
            bruta = 50.0,
            liquida = 45.0,
            perdasPct = 10.0,
            real = 0.0,
            nMoniliase = 0
        )
        val report = CumulativeStatsCalculator.build(1L, listOf(zeroReal), ProductionBasis.NET_HEALTHY_NFS)
        assertEquals(1, report.evaluationsWithRealCount)
        assertNull(report.meanRelativeErrorPct)
    }

    private fun summary(
        id: Long,
        dataMillis: Long,
        bruta: Double,
        liquida: Double,
        perdasPct: Double,
        real: Double?,
        nMoniliase: Int
    ): EvaluationSummary = EvaluationSummary(
        id = id,
        farmId = 1L,
        plotId = 10L,
        status = EvaluationStatus.COMPLETED,
        snapshotPlotName = "Roça Teste",
        snapshotAreaHa = 2.0,
        snapshotNpt = 1000,
        snapshotFa = 250.0,
        snapshotNpf = 20,
        nPlantasAmostradas = 20,
        nfs = 90,
        nfd = 10,
        nPodridao = 10 - nMoniliase,
        nMoniliase = nMoniliase,
        nVassoura = 0,
        producaoBrutaArrobas = bruta,
        producaoLiquidaArrobas = liquida,
        produtividadeBrutaPorHa = bruta / 2.0,
        produtividadeLiquidaPorHa = liquida / 2.0,
        perdasPct = perdasPct,
        severity = DiseaseSeverity.WARNING,
        dominantDisease = if (nMoniliase > 0) DominantDisease.MONILIASE else DominantDisease.PODRIDAO_PARDA,
        producaoRealArrobas = real,
        producaoRealEmMillis = real?.let { dataMillis + 100L },
        iniciadoEmMillis = dataMillis - 100L,
        dataMillis = dataMillis
    )
}
