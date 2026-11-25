package com.cacaoforecast.ai.domain.model

enum class EvaluationStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class DiseaseSeverity {
    CONTROLLED, // < 10.0%
    WARNING,    // 10.0% .. 19.999%
    CRITICAL    // >= 20.0%
}

enum class DominantDisease {
    NONE,
    PODRIDAO_PARDA,
    MONILIASE,
    VASSOURA_BRUXA
}

data class Farm(
    val id: Long = 0L,
    val nome: String,
    val municipioUf: String? = null,
    val proprietario: String? = null,
    val ativo: Boolean = true
)

data class Plot(
    val id: Long = 0L,
    val fazendaId: Long,
    val nome: String,
    val areaHa: Double,
    val npt: Int,
    val fa: Double,
    val npf: Int = 20,
    val ativo: Boolean = true
)

data class ComputedMetrics(
    val producaoBrutaArrobas: Double,
    val producaoLiquidaArrobas: Double,
    val produtividadeBrutaPorHa: Double,
    val produtividadeLiquidaPorHa: Double,
    val perdaArrobas: Double,
    val perdasPct: Double,
    val severity: DiseaseSeverity,
    val dominantDisease: DominantDisease
)

data class PlantSample(
    val id: Long = 0L,
    val evaluationId: Long,
    val plantIndex: Int,
    val nfs: Int,
    val nfd: Int,
    val nPodridao: Int = 0,
    val nMoniliase: Int = 0,
    val nVassoura: Int = 0,
    val manualEdited: Boolean = false,
    val tempPhotoPath: String? = null,
    val capturedAtMillis: Long = 0L
)

data class SamplingSessionDraft(
    val evaluationId: Long,
    val farmId: Long,
    val farmName: String,
    val plotId: Long,
    val plotName: String,
    val snapshotAreaHa: Double,
    val snapshotNpt: Int,
    val snapshotFa: Double,
    val snapshotNpf: Int,
    val sampledCount: Int,
    val totalNfs: Int,
    val totalNfd: Int,
    val totalPodridao: Int,
    val totalMoniliase: Int,
    val totalVassoura: Int,
    val startedAtMillis: Long
) {
    val nextPlantIndex: Int get() = (sampledCount + 1).coerceAtMost(snapshotNpf)
    val isReadyToFinalize: Boolean get() = sampledCount == snapshotNpf
}

data class EvaluationSummary(
    val id: Long,
    val farmId: Long,
    val plotId: Long,
    val status: EvaluationStatus,
    val snapshotPlotName: String,
    val snapshotAreaHa: Double,
    val snapshotNpt: Int,
    val snapshotFa: Double,
    val snapshotNpf: Int,
    val nPlantasAmostradas: Int,
    val nfs: Int,
    val nfd: Int,
    val nPodridao: Int,
    val nMoniliase: Int,
    val nVassoura: Int,
    val producaoBrutaArrobas: Double,
    val producaoLiquidaArrobas: Double,
    val produtividadeBrutaPorHa: Double,
    val produtividadeLiquidaPorHa: Double,
    val perdasPct: Double,
    val severity: DiseaseSeverity,
    val dominantDisease: DominantDisease,
    val producaoRealArrobas: Double?,
    val producaoRealEmMillis: Long?,
    val iniciadoEmMillis: Long,
    val dataMillis: Long
)

data class EvaluationWithPeriodCount(
    val evaluation: EvaluationSummary,
    val evaluationsCountInPeriod: Int
)
