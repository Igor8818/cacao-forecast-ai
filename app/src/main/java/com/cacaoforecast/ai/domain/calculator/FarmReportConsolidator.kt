package com.cacaoforecast.ai.domain.calculator

import com.cacaoforecast.ai.domain.model.EvaluationWithPeriodCount
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.FarmConsolidatedFooterUiModel
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.PlotReportRowUiModel
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder

object FarmReportConsolidator {

    fun buildReport(
        farm: Farm,
        allActivePlots: List<Plot>,
        deduplicatedEvaluations: List<EvaluationWithPeriodCount>,
        periodLabel: String,
        startMillis: Long,
        endMillis: Long,
        selectedPlotId: Long?,
        basis: ProductionBasis,
        sortOrder: ReportSortOrder
    ): FarmReportUiModel {
        val scopedEvaluations = if (selectedPlotId == null) {
            deduplicatedEvaluations
        } else {
            deduplicatedEvaluations.filter { it.evaluation.plotId == selectedPlotId }
        }

        val mappedRows = scopedEvaluations.map { item ->
            val eval = item.evaluation
            val producaoExibida = when (basis) {
                ProductionBasis.NET_HEALTHY_NFS -> eval.producaoLiquidaArrobas
                ProductionBasis.GROSS_TOTAL_NFT -> eval.producaoBrutaArrobas
            }
            val produtividadeExibida = when (basis) {
                ProductionBasis.NET_HEALTHY_NFS -> eval.produtividadeLiquidaPorHa
                ProductionBasis.GROSS_TOTAL_NFT -> eval.produtividadeBrutaPorHa
            }

            PlotReportRowUiModel(
                evaluationId = eval.id,
                plotId = eval.plotId,
                plotName = eval.snapshotPlotName,
                evaluationDateMillis = eval.dataMillis,
                evaluationsCountInPeriod = item.evaluationsCountInPeriod,
                areaHa = eval.snapshotAreaHa,
                npt = eval.snapshotNpt,
                producaoExibidaArrobas = producaoExibida,
                produtividadeExibidaPorHa = produtividadeExibida,
                producaoBrutaArrobas = eval.producaoBrutaArrobas,
                producaoLiquidaArrobas = eval.producaoLiquidaArrobas,
                perdasPct = eval.perdasPct,
                severity = eval.severity,
                dominantDisease = eval.dominantDisease,
                nMoniliase = eval.nMoniliase
            )
        }

        val sortedRows = when (sortOrder) {
            ReportSortOrder.LOSS_DESC -> mappedRows.sortedWith(
                compareByDescending<PlotReportRowUiModel> { it.perdasPct }
                    .thenBy { it.plotName.lowercase() }
            )
            ReportSortOrder.PRODUCTION_DESC -> mappedRows.sortedWith(
                compareByDescending<PlotReportRowUiModel> { it.producaoExibidaArrobas }
                    .thenBy { it.plotName.lowercase() }
            )
            ReportSortOrder.PLOT_NAME_ASC -> mappedRows.sortedBy { it.plotName.lowercase() }
        }

        val criticalHighlightRow = mappedRows
            .filter { it.perdasPct > 0.0 }
            .maxWithOrNull(
                compareBy<PlotReportRowUiModel> { it.perdasPct }
                    .thenBy { it.producaoBrutaArrobas - it.producaoLiquidaArrobas }
            )

        val scopedActivePlots = if (selectedPlotId == null) {
            allActivePlots.filter { it.ativo }
        } else {
            allActivePlots.filter { it.ativo && it.id == selectedPlotId }
        }
        val evaluatedPlotIds = mappedRows.map { it.plotId }.toSet()
        val pendingPlotNames = scopedActivePlots
            .filter { it.id !in evaluatedPlotIds }
            .map { it.nome }
            .sortedBy { it.lowercase() }

        val totalAreaHa = sortedRows.sumOf { it.areaHa }
        val totalNpt = sortedRows.sumOf { it.npt }
        val totalProducaoExibida = sortedRows.sumOf { it.producaoExibidaArrobas }
        val produtividadeConsolidada = if (totalAreaHa > 0.0) {
            totalProducaoExibida / totalAreaHa
        } else {
            0.0
        }

        val totalBruta = sortedRows.sumOf { it.producaoBrutaArrobas }
        val totalLiquida = sortedRows.sumOf { it.producaoLiquidaArrobas }
        val totalPerdaArrobas = (totalBruta - totalLiquida).coerceAtLeast(0.0)
        val perdasPonderadasPct = if (totalBruta > 0.0) {
            (totalPerdaArrobas / totalBruta) * 100.0
        } else {
            0.0
        }

        val totalMoniliaseFruits = sortedRows.sumOf { it.nMoniliase }
        val moniliaseAlertPlotNames = sortedRows
            .filter { it.nMoniliase > 0 }
            .map { it.plotName }
            .sortedBy { it.lowercase() }

        val footer = FarmConsolidatedFooterUiModel(
            totalPlotsEvaluated = sortedRows.size,
            totalActivePlotsInFarm = scopedActivePlots.size,
            pendingPlotNames = pendingPlotNames,
            totalAreaHa = totalAreaHa,
            totalNpt = totalNpt,
            totalProducaoExibidaArrobas = totalProducaoExibida,
            produtividadeConsolidadaPorHa = produtividadeConsolidada,
            totalPerdaArrobas = totalPerdaArrobas,
            perdasPonderadasPct = perdasPonderadasPct,
            severity = ForecastCalculator.classifySeverity(perdasPonderadasPct),
            totalMoniliaseFruits = totalMoniliaseFruits,
            moniliaseAlertPlotNames = moniliaseAlertPlotNames
        )

        return FarmReportUiModel(
            farm = farm,
            periodLabel = periodLabel,
            startMillis = startMillis,
            endMillis = endMillis,
            selectedPlotId = selectedPlotId,
            basis = basis,
            sortOrder = sortOrder,
            criticalHighlightRow = criticalHighlightRow,
            rows = sortedRows,
            footer = footer
        )
    }
}
