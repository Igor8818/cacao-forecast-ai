package com.cacaoforecast.ai.domain.calculator

import com.cacaoforecast.ai.domain.model.CumulativeEvaluationPoint
import com.cacaoforecast.ai.domain.model.CumulativePlotReport
import com.cacaoforecast.ai.domain.model.EvaluationSummary
import com.cacaoforecast.ai.domain.model.ProductionBasis
import kotlin.math.abs

object CumulativeStatsCalculator {

    fun build(
        plotId: Long?,
        evaluations: List<EvaluationSummary>,
        basis: ProductionBasis
    ): CumulativePlotReport {
        val ordered = evaluations.sortedWith(
            compareBy<EvaluationSummary> { it.dataMillis }.thenBy { it.id }
        )

        var previous: EvaluationSummary? = null
        val points = ordered.map { eval ->
            val estimada = when (basis) {
                ProductionBasis.NET_HEALTHY_NFS -> eval.producaoLiquidaArrobas
                ProductionBasis.GROSS_TOTAL_NFT -> eval.producaoBrutaArrobas
            }
            val deltaLoss = previous?.let { eval.perdasPct - it.perdasPct }
            previous = eval
            CumulativeEvaluationPoint(
                evaluationId = eval.id,
                dataMillis = eval.dataMillis,
                estimadaArrobas = estimada,
                realArrobas = eval.producaoRealArrobas,
                perdasPct = eval.perdasPct,
                deltaPerdasPctVsAnterior = deltaLoss,
                nMoniliase = eval.nMoniliase
            )
        }

        val totalEstimada = points.sumOf { it.estimadaArrobas }
        val withReal = points.filter { it.realArrobas != null }
        val totalReal = withReal.sumOf { it.realArrobas ?: 0.0 }
        val relErrors = withReal.mapNotNull { pt ->
            val real = pt.realArrobas ?: return@mapNotNull null
            if (real > 0.0) {
                (abs(pt.estimadaArrobas - real) / real) * 100.0
            } else {
                null
            }
        }
        val meanRelativeError = if (relErrors.isNotEmpty()) {
            relErrors.average()
        } else {
            null
        }

        return CumulativePlotReport(
            plotId = plotId,
            basis = basis,
            points = points,
            totalEstimadaArrobas = totalEstimada,
            totalRealArrobas = totalReal,
            evaluationsWithRealCount = withReal.size,
            meanRelativeErrorPct = meanRelativeError,
            latestLossDeltaPct = points.lastOrNull()?.deltaPerdasPctVsAnterior
        )
    }
}
