package com.cacaoforecast.ai.data.demo

import androidx.room.withTransaction
import com.cacaoforecast.ai.data.local.CacaoDatabase
import com.cacaoforecast.ai.data.local.entity.EvaluationEntity
import com.cacaoforecast.ai.data.local.entity.FarmEntity
import com.cacaoforecast.ai.data.local.entity.PlotEntity
import com.cacaoforecast.ai.domain.calculator.ForecastCalculator
import com.cacaoforecast.ai.domain.model.EvaluationStatus
import kotlinx.coroutines.flow.first
import java.util.Calendar

class DemoSeedManager(
    private val databaseProvider: () -> CacaoDatabase
) {
    /**
     * Popula a fazenda demonstrativa "Sempre Viva" com os exatos cenários do fluxograma oficial,
     * substituindo apenas uma eventual "Sempre Viva" ativa anterior sem afetar outras fazendas.
     */
    suspend fun seedSempreVivaDemo(referenceNowMillis: Long = System.currentTimeMillis()): Result<Unit> = runCatching {
        val db = databaseProvider()
        val activeFarms = db.farmDao().observeActiveFarms().first()
        val existingDemoFarms = activeFarms.filter { it.nome.equals("Sempre Viva", ignoreCase = true) }

        db.withTransaction {
            existingDemoFarms.forEach { old ->
                db.plotDao().softDeleteByFarm(old.id, referenceNowMillis)
                db.farmDao().softDelete(old.id, referenceNowMillis)
            }

            val farmId = db.farmDao().insert(
                FarmEntity(
                    nome = "Sempre Viva",
                    municipioUf = "Ilhéus/BA",
                    proprietario = "Demonstração Executiva",
                    ativo = 1,
                    criadoEm = referenceNowMillis,
                    atualizadoEm = referenceNowMillis
                )
            )

            // 1. Topo do Morro: 3 ha, NPT=2500, FA=300, NPF=20 -> 300,0 @ bruta, 100,0 @/ha, 5,0% perda
            val topoId = db.plotDao().insert(
                PlotEntity(
                    fazendaId = farmId,
                    nome = "Topo do Morro",
                    areaHa = 3.0,
                    npt = 2500,
                    fa = 300.0,
                    npf = 20,
                    ativo = 1,
                    criadoEm = referenceNowMillis,
                    atualizadoEm = referenceNowMillis
                )
            )

            val historyPoints = listOf(
                DemoEvalPoint(monthsAgo = 5, nfs = 240, nfd = 12, podridao = 12, vassoura = 0, realArrobas = 95.0),
                DemoEvalPoint(monthsAgo = 4, nfs = 432, nfd = 24, podridao = 18, vassoura = 6, realArrobas = 180.0),
                DemoEvalPoint(monthsAgo = 3, nfs = 114, nfd = 6, podridao = 6, vassoura = 0, realArrobas = 48.0),
                DemoEvalPoint(monthsAgo = 2, nfs = 126, nfd = 6, podridao = 4, vassoura = 2, realArrobas = 52.0),
                DemoEvalPoint(monthsAgo = 1, nfs = 132, nfd = 8, podridao = 6, vassoura = 2, realArrobas = 56.0)
            )

            historyPoints.forEach { pt ->
                val ts = firstDayOfMonthsAgo(referenceNowMillis, pt.monthsAgo)
                insertCompletedEvaluation(
                    db = db,
                    farmId = farmId,
                    plotId = topoId,
                    plotName = "Topo do Morro",
                    areaHa = 3.0,
                    npt = 2500,
                    fa = 300.0,
                    npf = 20,
                    nfs = pt.nfs,
                    nfd = pt.nfd,
                    nPodridao = pt.podridao,
                    nMoniliase = 0,
                    nVassoura = pt.vassoura,
                    timestampMillis = ts,
                    realArrobas = pt.realArrobas
                )
            }

            insertCompletedEvaluation(
                db = db,
                farmId = farmId,
                plotId = topoId,
                plotName = "Topo do Morro",
                areaHa = 3.0,
                npt = 2500,
                fa = 300.0,
                npf = 20,
                nfs = 684,
                nfd = 36,
                nPodridao = 28,
                nMoniliase = 0,
                nVassoura = 8,
                timestampMillis = referenceNowMillis,
                realArrobas = 290.0
            )

            // 2. Jatium: 5 ha, NPT=4000, FA=250, NPF=20 -> 400,0 @ bruta, 80,0 @/ha, 0,0% perda
            val jatiumId = db.plotDao().insert(
                PlotEntity(
                    fazendaId = farmId,
                    nome = "Jatium",
                    areaHa = 5.0,
                    npt = 4000,
                    fa = 250.0,
                    npf = 20,
                    ativo = 1,
                    criadoEm = referenceNowMillis,
                    atualizadoEm = referenceNowMillis
                )
            )
            insertCompletedEvaluation(
                db = db,
                farmId = farmId,
                plotId = jatiumId,
                plotName = "Jatium",
                areaHa = 5.0,
                npt = 4000,
                fa = 250.0,
                npf = 20,
                nfs = 500,
                nfd = 0,
                nPodridao = 0,
                nMoniliase = 0,
                nVassoura = 0,
                timestampMillis = referenceNowMillis,
                realArrobas = null
            )

            // 3. Peroto do Rio: 1 ha, NPT=600, FA=300, NPF=20 -> 80,0 @ bruta, 80,0 @/ha, 8,0% perda
            val perotoId = db.plotDao().insert(
                PlotEntity(
                    fazendaId = farmId,
                    nome = "Peroto do Rio",
                    areaHa = 1.0,
                    npt = 600,
                    fa = 300.0,
                    npf = 20,
                    ativo = 1,
                    criadoEm = referenceNowMillis,
                    atualizadoEm = referenceNowMillis
                )
            )
            insertCompletedEvaluation(
                db = db,
                farmId = farmId,
                plotId = perotoId,
                plotName = "Peroto do Rio",
                areaHa = 1.0,
                npt = 600,
                fa = 300.0,
                npf = 20,
                nfs = 736,
                nfd = 64,
                nPodridao = 50,
                nMoniliase = 0,
                nVassoura = 14,
                timestampMillis = referenceNowMillis,
                realArrobas = null
            )
        }
    }

    private suspend fun insertCompletedEvaluation(
        db: CacaoDatabase,
        farmId: Long,
        plotId: Long,
        plotName: String,
        areaHa: Double,
        npt: Int,
        fa: Double,
        npf: Int,
        nfs: Int,
        nfd: Int,
        nPodridao: Int,
        nMoniliase: Int,
        nVassoura: Int,
        timestampMillis: Long,
        realArrobas: Double?
    ) {
        val metrics = ForecastCalculator.calculate(
            nfs = nfs,
            nfd = nfd,
            nPodridao = nPodridao,
            nMoniliase = nMoniliase,
            nVassoura = nVassoura,
            npf = npf,
            npt = npt,
            fa = fa,
            areaHa = areaHa
        )
        db.evaluationDao().insertEvaluation(
            EvaluationEntity(
                fazendaId = farmId,
                quadraId = plotId,
                status = EvaluationStatus.COMPLETED.name,
                snapshotQuadraNome = plotName,
                snapshotAreaHa = areaHa,
                snapshotNpt = npt,
                snapshotFa = fa,
                snapshotNpf = npf,
                nPlantasAmostradas = npf,
                nFrutosSadios = nfs,
                nFrutosDoentes = nfd,
                nPodridaoParda = nPodridao,
                nMoniliase = nMoniliase,
                nVassouraBruxa = nVassoura,
                producaoEstimada = metrics.producaoBrutaArrobas,
                producaoEstimadaLiquida = metrics.producaoLiquidaArrobas,
                produtividade = metrics.produtividadeBrutaPorHa,
                produtividadeLiquida = metrics.produtividadeLiquidaPorHa,
                perdasPct = metrics.perdasPct,
                producaoReal = realArrobas,
                producaoRealEm = realArrobas?.let { timestampMillis },
                iniciadoEm = timestampMillis,
                data = timestampMillis
            )
        )
    }

    private fun firstDayOfMonthsAgo(referenceNowMillis: Long, monthsAgo: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceNowMillis
            add(Calendar.MONTH, -monthsAgo)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private data class DemoEvalPoint(
        val monthsAgo: Int,
        val nfs: Int,
        val nfd: Int,
        val podridao: Int,
        val vassoura: Int,
        val realArrobas: Double
    )
}