package com.cacaoforecast.ai.domain.calculator

import kotlin.math.roundToInt

data class CloneFaPreset(
    val label: String,
    val fa: Double
)

data class SpacingDensityPreset(
    val label: String,
    val plantsPerHa: Int
)

object PlotPresetHelper {

    val cloneFaPresets = listOf(
        CloneFaPreset("CCN-51 (220)", 220.0),
        CloneFaPreset("Clonal CEPEC (250)", 250.0),
        CloneFaPreset("Comum / Pará (290)", 290.0)
    )

    val spacingPresets = listOf(
        SpacingDensityPreset("3×3m (1.111/ha)", 1111),
        SpacingDensityPreset("3×2,5m (1.333/ha)", 1333),
        SpacingDensityPreset("Cabruca (650/ha)", 650)
    )

    val npfPresets = listOf(10, 20, 30)

    fun estimateNpt(areaHa: Double, plantsPerHa: Int): Int? {
        if (!areaHa.isFinite() || areaHa <= 0.0 || plantsPerHa <= 0) return null
        return (areaHa * plantsPerHa).roundToInt().coerceAtLeast(1)
    }
}
