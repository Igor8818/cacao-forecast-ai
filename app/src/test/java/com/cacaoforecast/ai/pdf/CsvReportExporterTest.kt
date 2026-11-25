package com.cacaoforecast.ai.pdf

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.DominantDisease
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.FarmConsolidatedFooterUiModel
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.PlotReportRowUiModel
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CsvReportExporterTest {

    @Test
    fun `exporta planilha CSV com BOM UTF8 escape RFC4180 e linha TOTAL_CONSOLIDADO em cacheDir reports`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val exporter = CsvReportExporter(context)

        val row = PlotReportRowUiModel(
            evaluationId = 1L,
            plotId = 10L,
            plotName = "Roça \"Sul\"; Baixada",
            evaluationDateMillis = 1_795_000_000_000L,
            evaluationsCountInPeriod = 2,
            areaHa = 3.5,
            npt = 3500,
            producaoExibidaArrobas = 140.0,
            produtividadeExibidaPorHa = 40.0,
            producaoBrutaArrobas = 175.0,
            producaoLiquidaArrobas = 140.0,
            perdasPct = 20.0,
            severity = DiseaseSeverity.CRITICAL,
            dominantDisease = DominantDisease.MONILIASE,
            nMoniliase = 4
        )
        val report = FarmReportUiModel(
            farm = Farm(id = 1L, nome = "Fazenda São José; Lote 1"),
            periodLabel = "Nov/2026",
            startMillis = 0L,
            endMillis = 1000L,
            selectedPlotId = null,
            basis = ProductionBasis.NET_HEALTHY_NFS,
            sortOrder = ReportSortOrder.LOSS_DESC,
            criticalHighlightRow = row,
            rows = listOf(row),
            footer = FarmConsolidatedFooterUiModel(
                totalPlotsEvaluated = 1,
                totalActivePlotsInFarm = 2,
                pendingPlotNames = listOf("Roça Pendente"),
                totalAreaHa = 3.5,
                totalNpt = 3500,
                totalProducaoExibidaArrobas = 140.0,
                produtividadeConsolidadaPorHa = 40.0,
                totalPerdaArrobas = 35.0,
                perdasPonderadasPct = 20.0,
                severity = DiseaseSeverity.CRITICAL,
                totalMoniliaseFruits = 4,
                moniliaseAlertPlotNames = listOf("Roça \"Sul\"; Baixada")
            )
        )

        val csv = exporter.buildCsvContent(report)
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("Fazenda;Periodo;Criterio;Roca_Quadra;"))
        assertTrue(csv.contains("\"Fazenda São José; Lote 1\""))
        assertTrue(csv.contains("\"Roça \"\"Sul\"\"; Baixada\""))
        assertTrue(csv.contains("TOTAL_CONSOLIDADO"))
        assertTrue(csv.contains(";1/2;3.50;3500;140.00;40.00;175.00;140.00;35.00;20.00;Critico;;4"))

        val res = exporter.export(report)
        assertTrue(res.isSuccess)
        val file = res.getOrThrow()
        try {
            assertTrue(file.exists())
            assertEquals(File(context.cacheDir, "reports").absolutePath, file.parentFile?.absolutePath)
            assertTrue(file.name.endsWith(".csv"))
            assertEquals(csv, file.readText(Charsets.UTF_8))
        } finally {
            file.delete()
        }
    }
}
