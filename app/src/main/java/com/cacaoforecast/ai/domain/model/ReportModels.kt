package com.cacaoforecast.ai.domain.model

enum class ProductionBasis {
    NET_HEALTHY_NFS,
    GROSS_TOTAL_NFT
}

enum class ReportSortOrder {
    LOSS_DESC,
    PRODUCTION_DESC,
    PLOT_NAME_ASC
}

data class PlotReportRowUiModel(
    val evaluationId: Long,
    val plotId: Long,
    val plotName: String,
    val evaluationDateMillis: Long,
    val evaluationsCountInPeriod: Int,
    val areaHa: Double,
    val npt: Int,
    val producaoExibidaArrobas: Double,
    val produtividadeExibidaPorHa: Double,
    val producaoBrutaArrobas: Double,
    val producaoLiquidaArrobas: Double,
    val perdasPct: Double,
    val severity: DiseaseSeverity,
    val dominantDisease: DominantDisease,
    val nMoniliase: Int = 0,
    val nfs: Int = 0,
    val nfd: Int = 0,
    val nPodridaoParda: Int = 0,
    val nVassouraBruxa: Int = 0
)

data class FarmConsolidatedFooterUiModel(
    val totalPlotsEvaluated: Int,
    val totalActivePlotsInFarm: Int,
    val pendingPlotNames: List<String>,
    val totalAreaHa: Double,
    val totalNpt: Int,
    val totalProducaoExibidaArrobas: Double,
    val produtividadeConsolidadaPorHa: Double,
    val totalPerdaArrobas: Double,
    val perdasPonderadasPct: Double,
    val severity: DiseaseSeverity,
    val totalMoniliaseFruits: Int = 0,
    val moniliaseAlertPlotNames: List<String> = emptyList(),
    val totalBrutaArrobas: Double = 0.0,
    val totalLiquidaArrobas: Double = 0.0,
    val totalHealthyFruits: Int = 0,
    val totalPodridaoPardaFruits: Int = 0,
    val totalVassouraBruxaFruits: Int = 0
)

data class FarmReportUiModel(
    val farm: Farm,
    val periodLabel: String,
    val startMillis: Long,
    val endMillis: Long,
    val selectedPlotId: Long?,
    val basis: ProductionBasis,
    val sortOrder: ReportSortOrder,
    val criticalHighlightRow: PlotReportRowUiModel?,
    val rows: List<PlotReportRowUiModel>,
    val footer: FarmConsolidatedFooterUiModel
)

data class CumulativeEvaluationPoint(
    val evaluationId: Long,
    val dataMillis: Long,
    val estimadaArrobas: Double,
    val realArrobas: Double?,
    val perdasPct: Double,
    val deltaPerdasPctVsAnterior: Double?,
    val nMoniliase: Int
)

data class CumulativePlotReport(
    val plotId: Long?,
    val basis: ProductionBasis,
    val points: List<CumulativeEvaluationPoint>,
    val totalEstimadaArrobas: Double,
    val totalRealArrobas: Double,
    val evaluationsWithRealCount: Int,
    val meanRelativeErrorPct: Double?,
    val latestLossDeltaPct: Double?
)
