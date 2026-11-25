package com.cacaoforecast.ai.domain.calculator

import com.cacaoforecast.ai.domain.model.ComputedMetrics
import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.DominantDisease

object ForecastCalculator {

    const val MIN_NPF = 1
    const val MAX_NPF = 500
    const val WARNING_LOSS_THRESHOLD_PCT = 10.0
    const val CRITICAL_LOSS_THRESHOLD_PCT = 20.0

    fun calculate(
        nfs: Int,
        nfd: Int,
        nPodridao: Int = 0,
        nMoniliase: Int = 0,
        nVassoura: Int = 0,
        npf: Int,
        npt: Int,
        fa: Double,
        areaHa: Double
    ): ComputedMetrics {
        require(nfs >= 0) { "NFS deve ser >= 0" }
        require(nfd >= 0) { "NFD deve ser >= 0" }
        require(nPodridao >= 0) { "nPodridao deve ser >= 0" }
        require(nMoniliase >= 0) { "nMoniliase deve ser >= 0" }
        require(nVassoura >= 0) { "nVassoura deve ser >= 0" }
        require(npf in MIN_NPF..MAX_NPF) { "NPF deve estar entre $MIN_NPF e $MAX_NPF" }
        require(npt > 0) { "NPT deve ser > 0" }
        require(fa > 0.0 && fa.isFinite()) { "FA deve ser > 0" }
        require(areaHa > 0.0 && areaHa.isFinite()) { "areaHa deve ser > 0" }

        val nft = nfs + nfd
        val dominant = resolveDominantDisease(nPodridao, nMoniliase, nVassoura)

        if (nft == 0) {
            return ComputedMetrics(
                producaoBrutaArrobas = 0.0,
                producaoLiquidaArrobas = 0.0,
                produtividadeBrutaPorHa = 0.0,
                produtividadeLiquidaPorHa = 0.0,
                perdaArrobas = 0.0,
                perdasPct = 0.0,
                severity = DiseaseSeverity.CONTROLLED,
                dominantDisease = dominant
            )
        }

        val producaoBruta = ((nft.toDouble() / npf.toDouble()) * npt.toDouble()) / fa
        val producaoLiquida = ((nfs.toDouble() / npf.toDouble()) * npt.toDouble()) / fa
        val perdaArrobas = (producaoBruta - producaoLiquida).coerceAtLeast(0.0)
        val produtividadeBruta = producaoBruta / areaHa
        val produtividadeLiquida = producaoLiquida / areaHa
        val perdasPct = (nfd.toDouble() / nft.toDouble()) * 100.0

        return ComputedMetrics(
            producaoBrutaArrobas = producaoBruta,
            producaoLiquidaArrobas = producaoLiquida,
            produtividadeBrutaPorHa = produtividadeBruta,
            produtividadeLiquidaPorHa = produtividadeLiquida,
            perdaArrobas = perdaArrobas,
            perdasPct = perdasPct,
            severity = classifySeverity(perdasPct),
            dominantDisease = dominant
        )
    }

    fun classifySeverity(perdasPct: Double): DiseaseSeverity {
        return when {
            perdasPct >= CRITICAL_LOSS_THRESHOLD_PCT -> DiseaseSeverity.CRITICAL
            perdasPct >= WARNING_LOSS_THRESHOLD_PCT -> DiseaseSeverity.WARNING
            else -> DiseaseSeverity.CONTROLLED
        }
    }

    fun resolveDominantDisease(
        nPodridao: Int,
        nMoniliase: Int,
        nVassoura: Int
    ): DominantDisease {
        val maxCount = maxOf(nPodridao, nMoniliase, nVassoura)
        if (maxCount <= 0) return DominantDisease.NONE
        return when {
            nMoniliase == maxCount -> DominantDisease.MONILIASE
            nVassoura == maxCount -> DominantDisease.VASSOURA_BRUXA
            else -> DominantDisease.PODRIDAO_PARDA
        }
    }
}
