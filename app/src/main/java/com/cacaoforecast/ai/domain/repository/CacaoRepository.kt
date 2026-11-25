package com.cacaoforecast.ai.domain.repository

import com.cacaoforecast.ai.domain.model.EvaluationSummary
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder
import com.cacaoforecast.ai.domain.model.SamplingSessionDraft
import kotlinx.coroutines.flow.Flow

interface CacaoRepository {
    // CRUD Fazendas e Roças/Quadras
    fun observeActiveFarms(): Flow<List<Farm>>
    fun observeFarmById(farmId: Long): Flow<Farm?>
    suspend fun getFarmById(farmId: Long): Farm?
    suspend fun saveFarm(farm: Farm): Result<Long>
    suspend fun archiveFarm(farmId: Long): Result<Unit>

    fun observeActivePlotsByFarm(farmId: Long): Flow<List<Plot>>
    suspend fun getPlotById(plotId: Long): Plot?
    suspend fun savePlot(plot: Plot): Result<Long>
    suspend fun archivePlot(plotId: Long): Result<Unit>

    // Fluxo de Amostragem Resiliente (1..NPF)
    suspend fun findInProgressSession(plotId: Long): SamplingSessionDraft?
    suspend fun findAnyInProgressSession(): SamplingSessionDraft?
    suspend fun getSessionDraftByEvaluationId(evaluationId: Long): SamplingSessionDraft?
    suspend fun startNewSamplingSession(plot: Plot): Result<SamplingSessionDraft>
    suspend fun savePlantSample(
        evaluationId: Long,
        plantIndex: Int,
        nfs: Int,
        nfd: Int,
        nPodridao: Int,
        nMoniliase: Int,
        nVassoura: Int,
        manualEdited: Boolean,
        tempPhotoPath: String?
    ): Result<SamplingSessionDraft>
    suspend fun finalizeSamplingSession(evaluationId: Long): Result<Long>
    suspend fun discardSamplingSession(evaluationId: Long): Result<Unit>

    // Histórico & Produção Real
    fun observeCompletedEvaluationsByPlot(plotId: Long): Flow<List<EvaluationSummary>>
    suspend fun getEvaluationById(evaluationId: Long): EvaluationSummary?
    suspend fun updateRealProduction(evaluationId: Long, realArrobas: Double?): Result<Unit>

    // Relatório Consolidado (sempre via SQLite, nunca memória volátil)
    fun observeFarmReport(
        farmId: Long,
        startMillis: Long,
        endMillis: Long,
        selectedPlotId: Long?,
        basis: ProductionBasis,
        sortOrder: ReportSortOrder
    ): Flow<FarmReportUiModel>
}
