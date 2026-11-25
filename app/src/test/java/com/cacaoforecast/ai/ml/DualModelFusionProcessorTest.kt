package com.cacaoforecast.ai.ml

import com.cacaoforecast.ai.domain.ml.FruitHealthCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class DualModelFusionProcessorTest {

    @Test
    fun `fusao preserva 100 por cento dos frutos de contagem e promove doenca sobreposta com IoU maior ou igual a 0_20`() {
        val counting = listOf(
            DualModelFusionProcessor.FusionBoxInput(0.10f, 0.10f, 0.30f, 0.30f, FruitHealthCategory.SADIO, 0.82f),
            DualModelFusionProcessor.FusionBoxInput(0.50f, 0.50f, 0.70f, 0.70f, FruitHealthCategory.SADIO, 0.75f)
        )
        val disease = listOf(
            // Sobrepoe fortemente caixa 1
            DualModelFusionProcessor.FusionBoxInput(0.11f, 0.11f, 0.29f, 0.29f, FruitHealthCategory.PODRIDAO_PARDA, 0.79f),
            // Doenca fora das caixas de contagem com confianca >= 0.25 -> deve ser adicionada
            DualModelFusionProcessor.FusionBoxInput(0.75f, 0.75f, 0.90f, 0.90f, FruitHealthCategory.MONILIASE, 0.65f),
            // Doenca abaixo da confianca minima -> ignorada
            DualModelFusionProcessor.FusionBoxInput(0.80f, 0.10f, 0.90f, 0.20f, FruitHealthCategory.VASSOURA_BRUXA, 0.18f)
        )

        val fused = DualModelFusionProcessor.fuse(
            countingBoxes = counting,
            diseaseBoxes = disease,
            iouMinimo = 0.20f,
            diseaseMinConfidence = 0.25f,
            maxResults = 100
        )

        assertEquals(3, fused.size)
        assertEquals(FruitHealthCategory.PODRIDAO_PARDA, fused[0].category)
        assertEquals(FruitHealthCategory.SADIO, fused[1].category)
        assertEquals(FruitHealthCategory.MONILIASE, fused[2].category)
    }

    @Test
    fun `fallback por recorte promove fruto sadio nao casado quando heuristica identifica vassoura`() {
        val counting = listOf(
            DualModelFusionProcessor.FusionBoxInput(0.20f, 0.20f, 0.40f, 0.40f, FruitHealthCategory.SADIO, 0.70f)
        )

        val fused = DualModelFusionProcessor.fuse(
            countingBoxes = counting,
            diseaseBoxes = emptyList(),
            cropFallbackClassifier = { FruitHealthCategory.VASSOURA_BRUXA }
        )

        assertEquals(1, fused.size)
        assertEquals(FruitHealthCategory.VASSOURA_BRUXA, fused.single().category)
    }
}
