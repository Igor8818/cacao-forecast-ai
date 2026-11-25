package com.cacaoforecast.ai.data.repository

import androidx.room.withTransaction
import com.cacaoforecast.ai.data.local.CacaoDatabase
import com.cacaoforecast.ai.data.local.entity.EvaluationEntity
import com.cacaoforecast.ai.data.local.entity.FarmEntity
import com.cacaoforecast.ai.data.local.entity.PlantSampleEntity
import com.cacaoforecast.ai.data.local.entity.PlotEntity
import com.cacaoforecast.ai.domain.calculator.FarmReportConsolidator
import com.cacaoforecast.ai.domain.calculator.ForecastCalculator
import com.cacaoforecast.ai.domain.model.EvaluationStatus
import com.cacaoforecast.ai.domain.model.EvaluationSummary
import com.cacaoforecast.ai.domain.model.EvaluationWithPeriodCount
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder
import com.cacaoforecast.ai.domain.model.SamplingSessionDraft
import com.cacaoforecast.ai.domain.repository.CacaoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCacaoRepository(
    private val databaseProvider: () -> CacaoDatabase,
    private val sessionDirProvider: (Long) -> File,
    private val databaseGeneration: Flow<Int> = flowOf(0),
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) : CacaoRepository {

    private val db: CacaoDatabase get() = databaseProvider()
    private val farmDao get() = db.farmDao()
    private val plotDao get() = db.plotDao()
    private val evaluationDao get() = db.evaluationDao()

    override fun observeActiveFarms(): Flow<List<Farm>> =
        databaseGeneration.flatMapLatest {
            farmDao.observeActiveFarms().map { list -> list.map { it.toDomain() } }
        }

    override fun observeFarmById(farmId: Long): Flow<Farm?> =
        databaseGeneration.flatMapLatest {
            farmDao.observeById(farmId).map { it?.toDomain() }
        }

    override suspend fun getFarmById(farmId: Long): Farm? =
        farmDao.getById(farmId)?.toDomain()

    override suspend fun saveFarm(farm: Farm): Result<Long> = runCatching {
        val cleanName = farm.nome.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome da fazenda." }
        val now = clockMillis()
        if (farm.id == 0L) {
            farmDao.insert(
                FarmEntity(
                    nome = cleanName,
                    municipioUf = farm.municipioUf?.trim()?.takeIf { it.isNotEmpty() },
                    proprietario = farm.proprietario?.trim()?.takeIf { it.isNotEmpty() },
                    ativo = 1,
                    criadoEm = now,
                    atualizadoEm = now
                )
            )
        } else {
            val existing = farmDao.getById(farm.id) ?: error("Fazenda não encontrada.")
            farmDao.update(
                existing.copy(
                    nome = cleanName,
                    municipioUf = farm.municipioUf?.trim()?.takeIf { it.isNotEmpty() },
                    proprietario = farm.proprietario?.trim()?.takeIf { it.isNotEmpty() },
                    atualizadoEm = now
                )
            )
            farm.id
        }
    }

    override suspend fun archiveFarm(farmId: Long): Result<Unit> = runCatching {
        val now = clockMillis()
        db.withTransaction {
            plotDao.softDeleteByFarm(farmId, now)
            farmDao.softDelete(farmId, now)
        }
    }

    override fun observeActivePlotsByFarm(farmId: Long): Flow<List<Plot>> =
        databaseGeneration.flatMapLatest {
            plotDao.observeActiveByFarm(farmId).map { list -> list.map { it.toDomain() } }
        }

    override suspend fun getPlotById(plotId: Long): Plot? =
        plotDao.getById(plotId)?.toDomain()

    override suspend fun savePlot(plot: Plot): Result<Long> = runCatching {
        val cleanName = plot.nome.trim()
        require(cleanName.isNotEmpty()) { "Informe o nome da Roça / Quadra." }
        require(plot.areaHa > 0.0 && plot.areaHa.isFinite()) { "Área (ha) deve ser maior que zero." }
        require(plot.npt > 0) { "NPT deve ser maior que zero." }
        require(plot.fa > 0.0 && plot.fa.isFinite()) { "FA deve ser maior que zero." }
        require(plot.npf in ForecastCalculator.MIN_NPF..ForecastCalculator.MAX_NPF) {
            "NPF deve estar entre ${ForecastCalculator.MIN_NPF} e ${ForecastCalculator.MAX_NPF}."
        }
        val now = clockMillis()
        if (plot.id == 0L) {
            plotDao.insert(
                PlotEntity(
                    fazendaId = plot.fazendaId,
                    nome = cleanName,
                    areaHa = plot.areaHa,
                    npt = plot.npt,
                    fa = plot.fa,
                    npf = plot.npf,
                    ativo = 1,
                    criadoEm = now,
                    atualizadoEm = now
                )
            )
        } else {
            val existing = plotDao.getById(plot.id) ?: error("Roça / Quadra não encontrada.")
            plotDao.update(
                existing.copy(
                    nome = cleanName,
                    areaHa = plot.areaHa,
                    npt = plot.npt,
                    fa = plot.fa,
                    npf = plot.npf,
                    atualizadoEm = now
                )
            )
            plot.id
        }
    }

    override suspend fun archivePlot(plotId: Long): Result<Unit> = runCatching {
        plotDao.softDelete(plotId, clockMillis())
    }

    override suspend fun findInProgressSession(plotId: Long): SamplingSessionDraft? {
        val entity = evaluationDao.findInProgressByPlot(plotId) ?: return null
        val farmName = farmDao.getById(entity.fazendaId)?.nome ?: "Fazenda"
        return entity.toDraft(farmName)
    }

    override suspend fun findAnyInProgressSession(): SamplingSessionDraft? {
        val entity = evaluationDao.findAnyInProgress() ?: return null
        val farmName = farmDao.getById(entity.fazendaId)?.nome ?: "Fazenda"
        return entity.toDraft(farmName)
    }

    override suspend fun getSessionDraftByEvaluationId(evaluationId: Long): SamplingSessionDraft? {
        val entity = evaluationDao.getEvaluationById(evaluationId) ?: return null
        val farmName = farmDao.getById(entity.fazendaId)?.nome ?: "Fazenda"
        return entity.toDraft(farmName)
    }

    override suspend fun startNewSamplingSession(plot: Plot): Result<SamplingSessionDraft> = runCatching {
        require(plot.areaHa > 0.0 && plot.npt > 0 && plot.fa > 0.0 && plot.npf in 1..500) {
            "Parâmetros da Roça / Quadra inválidos para iniciar amostragem."
        }
        val farm = farmDao.getById(plot.fazendaId) ?: error("Fazenda não encontrada.")
        val now = clockMillis()
        val evaluationId = db.withTransaction {
            val existingDraft = evaluationDao.findInProgressByPlot(plot.id)
            if (existingDraft != null) {
                evaluationDao.markCancelled(existingDraft.id)
                cleanupSessionFiles(existingDraft.id)
            }
            evaluationDao.insertEvaluation(
                EvaluationEntity(
                    fazendaId = plot.fazendaId,
                    quadraId = plot.id,
                    status = EvaluationStatus.IN_PROGRESS.name,
                    snapshotQuadraNome = plot.nome,
                    snapshotAreaHa = plot.areaHa,
                    snapshotNpt = plot.npt,
                    snapshotFa = plot.fa,
                    snapshotNpf = plot.npf,
                    iniciadoEm = now,
                    data = now
                )
            )
        }
        val created = evaluationDao.getEvaluationById(evaluationId) ?: error("Falha ao criar rascunho de sessão.")
        created.toDraft(farm.nome)
    }

    override suspend fun savePlantSample(
        evaluationId: Long,
        plantIndex: Int,
        nfs: Int,
        nfd: Int,
        nPodridao: Int,
        nMoniliase: Int,
        nVassoura: Int,
        manualEdited: Boolean,
        tempPhotoPath: String?
    ): Result<SamplingSessionDraft> = runCatching {
        require(plantIndex >= 1) { "Índice da planta inválido." }
        require(nfs >= 0 && nfd >= 0) { "Contadores devem ser >= 0." }
        val now = clockMillis()
        val updated = db.withTransaction {
            val evaluation = evaluationDao.getEvaluationById(evaluationId)
                ?: error("Sessão de amostragem não encontrada.")
            require(evaluation.status == EvaluationStatus.IN_PROGRESS.name) {
                "Sessão não está em andamento."
            }
            require(plantIndex <= evaluation.snapshotNpf) {
                "Índice da planta ($plantIndex) excede NPF (${evaluation.snapshotNpf})."
            }

            val existingSample = evaluationDao.getSampleByIndex(evaluationId, plantIndex)
            evaluationDao.upsertPlantSample(
                PlantSampleEntity(
                    id = existingSample?.id ?: 0L,
                    avaliacaoId = evaluationId,
                    indicePlanta = plantIndex,
                    nSadios = nfs,
                    nDoentes = nfd,
                    nPodridaoParda = nPodridao,
                    nMoniliase = nMoniliase,
                    nVassouraBruxa = nVassoura,
                    editadoManualmente = if (manualEdited) 1 else 0,
                    fotoTempPath = tempPhotoPath,
                    capturadoEm = now
                )
            )

            val allSamples = evaluationDao.getSamplesByEvaluation(evaluationId)
            val totalSadios = allSamples.sumOf { it.nSadios }
            val totalDoentes = allSamples.sumOf { it.nDoentes }
            val totalPodridao = allSamples.sumOf { it.nPodridaoParda }
            val totalMoniliase = allSamples.sumOf { it.nMoniliase }
            val totalVassoura = allSamples.sumOf { it.nVassouraBruxa }

            val nextEval = evaluation.copy(
                nPlantasAmostradas = allSamples.size,
                nFrutosSadios = totalSadios,
                nFrutosDoentes = totalDoentes,
                nPodridaoParda = totalPodridao,
                nMoniliase = totalMoniliase,
                nVassouraBruxa = totalVassoura
            )
            evaluationDao.updateEvaluation(nextEval)
            nextEval
        }
        val farmName = farmDao.getById(updated.fazendaId)?.nome ?: "Fazenda"
        updated.toDraft(farmName)
    }

    override suspend fun finalizeSamplingSession(evaluationId: Long): Result<Long> = runCatching {
        val now = clockMillis()
        db.withTransaction {
            val evaluation = evaluationDao.getEvaluationById(evaluationId)
                ?: error("Avaliação não encontrada.")
            require(evaluation.status == EvaluationStatus.IN_PROGRESS.name) {
                "Avaliação não está aberta para finalização."
            }
            val samples = evaluationDao.getSamplesByEvaluation(evaluationId)
            require(samples.size == evaluation.snapshotNpf) {
                "Amostragem incompleta: ${samples.size}/${evaluation.snapshotNpf} plantas."
            }

            val totalSadios = samples.sumOf { it.nSadios }
            val totalDoentes = samples.sumOf { it.nDoentes }
            val totalPodridao = samples.sumOf { it.nPodridaoParda }
            val totalMoniliase = samples.sumOf { it.nMoniliase }
            val totalVassoura = samples.sumOf { it.nVassouraBruxa }

            val metrics = ForecastCalculator.calculate(
                nfs = totalSadios,
                nfd = totalDoentes,
                nPodridao = totalPodridao,
                nMoniliase = totalMoniliase,
                nVassoura = totalVassoura,
                npf = evaluation.snapshotNpf,
                npt = evaluation.snapshotNpt,
                fa = evaluation.snapshotFa,
                areaHa = evaluation.snapshotAreaHa
            )

            evaluationDao.updateEvaluation(
                evaluation.copy(
                    status = EvaluationStatus.COMPLETED.name,
                    nPlantasAmostradas = samples.size,
                    nFrutosSadios = totalSadios,
                    nFrutosDoentes = totalDoentes,
                    nPodridaoParda = totalPodridao,
                    nMoniliase = totalMoniliase,
                    nVassouraBruxa = totalVassoura,
                    producaoEstimada = metrics.producaoBrutaArrobas,
                    producaoEstimadaLiquida = metrics.producaoLiquidaArrobas,
                    produtividade = metrics.produtividadeBrutaPorHa,
                    produtividadeLiquida = metrics.produtividadeLiquidaPorHa,
                    perdasPct = metrics.perdasPct,
                    data = now
                )
            )
            evaluationDao.clearTempPhotoPaths(evaluationId)
        }
        cleanupSessionFiles(evaluationId)
        evaluationId
    }

    override suspend fun discardSamplingSession(evaluationId: Long): Result<Unit> = runCatching {
        db.withTransaction {
            evaluationDao.markCancelled(evaluationId)
            evaluationDao.clearTempPhotoPaths(evaluationId)
        }
        cleanupSessionFiles(evaluationId)
    }

    override fun observeCompletedEvaluationsByPlot(plotId: Long): Flow<List<EvaluationSummary>> =
        databaseGeneration.flatMapLatest {
            evaluationDao.observeCompletedByPlot(plotId).map { list -> list.map { it.toSummary() } }
        }

    override suspend fun getEvaluationById(evaluationId: Long): EvaluationSummary? =
        evaluationDao.getEvaluationById(evaluationId)?.toSummary()

    override suspend fun updateRealProduction(evaluationId: Long, realArrobas: Double?): Result<Unit> = runCatching {
        if (realArrobas != null) {
            require(realArrobas >= 0.0 && realArrobas.isFinite()) { "Produção real deve ser >= 0." }
        }
        val existing = evaluationDao.getEvaluationById(evaluationId)
            ?: error("Avaliação não encontrada.")
        require(existing.status == EvaluationStatus.COMPLETED.name) {
            "Só é possível informar produção real em avaliações concluídas."
        }
        evaluationDao.updateRealProduction(evaluationId, realArrobas, clockMillis())
    }

    override fun observeFarmReport(
        farmId: Long,
        startMillis: Long,
        endMillis: Long,
        selectedPlotId: Long?,
        basis: ProductionBasis,
        sortOrder: ReportSortOrder,
        periodLabel: String?
    ): Flow<FarmReportUiModel> {
        val resolvedLabel = periodLabel?.takeIf { it.isNotBlank() } ?: formatPeriodLabel(startMillis, endMillis)
        return databaseGeneration.flatMapLatest {
            combine(
                farmDao.observeById(farmId),
                plotDao.observeActiveByFarm(farmId),
                evaluationDao.observeLatestCompletedPerPlotInPeriod(farmId, startMillis, endMillis, selectedPlotId)
            ) { farmEntity, plotEntities, evalTuples ->
                val farm = farmEntity?.toDomain() ?: Farm(id = farmId, nome = "Fazenda")
                val plots = plotEntities.map { it.toDomain() }
                val evals = evalTuples.map {
                    EvaluationWithPeriodCount(
                        evaluation = it.evaluation.toSummary(),
                        evaluationsCountInPeriod = it.qtdAvaliacoesNoPeriodo
                    )
                }
                FarmReportConsolidator.buildReport(
                    farm = farm,
                    allActivePlots = plots,
                    deduplicatedEvaluations = evals,
                    periodLabel = resolvedLabel,
                    startMillis = startMillis,
                    endMillis = endMillis,
                    selectedPlotId = selectedPlotId,
                    basis = basis,
                    sortOrder = sortOrder
                )
            }
        }
    }

    private fun cleanupSessionFiles(evaluationId: Long) {
        runCatching {
            val dir = sessionDirProvider(evaluationId)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    private fun formatPeriodLabel(startMillis: Long, endMillis: Long): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        return "${fmt.format(Date(startMillis))} – ${fmt.format(Date(endMillis))}"
    }

    private fun FarmEntity.toDomain() = Farm(
        id = id,
        nome = nome,
        municipioUf = municipioUf,
        proprietario = proprietario,
        ativo = ativo == 1
    )

    private fun PlotEntity.toDomain() = Plot(
        id = id,
        fazendaId = fazendaId,
        nome = nome,
        areaHa = areaHa,
        npt = npt,
        fa = fa,
        npf = npf,
        ativo = ativo == 1
    )

    private fun EvaluationEntity.toDraft(farmName: String) = SamplingSessionDraft(
        evaluationId = id,
        farmId = fazendaId,
        farmName = farmName,
        plotId = quadraId,
        plotName = snapshotQuadraNome,
        snapshotAreaHa = snapshotAreaHa,
        snapshotNpt = snapshotNpt,
        snapshotFa = snapshotFa,
        snapshotNpf = snapshotNpf,
        sampledCount = nPlantasAmostradas,
        totalNfs = nFrutosSadios,
        totalNfd = nFrutosDoentes,
        totalPodridao = nPodridaoParda,
        totalMoniliase = nMoniliase,
        totalVassoura = nVassouraBruxa,
        startedAtMillis = iniciadoEm
    )

    private fun EvaluationEntity.toSummary(): EvaluationSummary {
        val sev = ForecastCalculator.classifySeverity(perdasPct)
        val dom = ForecastCalculator.resolveDominantDisease(nPodridaoParda, nMoniliase, nVassouraBruxa)
        return EvaluationSummary(
            id = id,
            farmId = fazendaId,
            plotId = quadraId,
            status = EvaluationStatus.valueOf(status),
            snapshotPlotName = snapshotQuadraNome,
            snapshotAreaHa = snapshotAreaHa,
            snapshotNpt = snapshotNpt,
            snapshotFa = snapshotFa,
            snapshotNpf = snapshotNpf,
            nPlantasAmostradas = nPlantasAmostradas,
            nfs = nFrutosSadios,
            nfd = nFrutosDoentes,
            nPodridao = nPodridaoParda,
            nMoniliase = nMoniliase,
            nVassoura = nVassouraBruxa,
            producaoBrutaArrobas = producaoEstimada,
            producaoLiquidaArrobas = producaoEstimadaLiquida,
            produtividadeBrutaPorHa = produtividade,
            produtividadeLiquidaPorHa = produtividadeLiquida,
            perdasPct = perdasPct,
            severity = sev,
            dominantDisease = dom,
            producaoRealArrobas = producaoReal,
            producaoRealEmMillis = producaoRealEm,
            iniciadoEmMillis = iniciadoEm,
            dataMillis = data
        )
    }
}
