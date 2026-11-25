package com.cacaoforecast.ai.ml

import com.cacaoforecast.ai.domain.ml.DetectedFruitBox
import com.cacaoforecast.ai.domain.ml.FruitHealthCategory
import kotlin.math.max
import kotlin.math.min

object DualModelFusionProcessor {

    data class FusionBoxInput(
        val normLeft: Float,
        val normTop: Float,
        val normRight: Float,
        val normBottom: Float,
        val category: FruitHealthCategory,
        val confidence: Float
    )

    /**
     * Fusão em dois estágios CEPEC:
     * 1. Mantém 100% dos frutos encontrados pelo modelo de contagem (`countingBoxes`).
     * 2. Quando `diseaseBoxes` sobrepõe uma caixa (`IoU >= iouMinimo`), promove a doença.
     * 3. Doenças detectadas fora das caixas de contagem (`IoU < iouMinimo` e `confidence >= diseaseMinConfidence`)
     *    são adicionadas sem descartar frutos sadios.
     * 4. Para frutos sadios sem correspondência no modelo de doença, permite fallback opcional por recorte.
     */
    fun fuse(
        countingBoxes: List<FusionBoxInput>,
        diseaseBoxes: List<FusionBoxInput>,
        iouMinimo: Float = 0.20f,
        diseaseMinConfidence: Float = 0.25f,
        maxResults: Int = 100,
        cropFallbackClassifier: ((FusionBoxInput) -> FruitHealthCategory)? = null
    ): List<DetectedFruitBox> {
        if (countingBoxes.isEmpty()) {
            return diseaseBoxes
                .filter { it.confidence >= diseaseMinConfidence }
                .take(maxResults)
                .mapIndexed { idx, box -> box.toDetected("d_$idx") }
        }
        if (diseaseBoxes.isEmpty()) {
            return countingBoxes
                .take(maxResults)
                .mapIndexed { idx, box ->
                    val fallbackCat = if (box.category == FruitHealthCategory.SADIO && cropFallbackClassifier != null) {
                        cropFallbackClassifier(box)
                    } else {
                        box.category
                    }
                    box.copy(category = fallbackCat).toDetected("c_$idx")
                }
        }

        val diseaseUsed = BooleanArray(diseaseBoxes.size)
        val merged = countingBoxes.map { countBox ->
            var bestIdx = -1
            var bestIoU = iouMinimo
            diseaseBoxes.forEachIndexed { dIdx, dBox ->
                if (diseaseUsed[dIdx]) return@forEachIndexed
                val overlap = iouNorm(
                    countBox.normLeft, countBox.normTop, countBox.normRight, countBox.normBottom,
                    dBox.normLeft, dBox.normTop, dBox.normRight, dBox.normBottom
                )
                if (overlap >= bestIoU) {
                    bestIoU = overlap
                    bestIdx = dIdx
                }
            }
            if (bestIdx >= 0) {
                diseaseUsed[bestIdx] = true
                val match = diseaseBoxes[bestIdx]
                if (match.category != FruitHealthCategory.SADIO || countBox.category == FruitHealthCategory.SADIO) {
                    countBox.copy(
                        category = if (match.category != FruitHealthCategory.SADIO) match.category else countBox.category,
                        confidence = max(countBox.confidence, match.confidence)
                    )
                } else {
                    countBox
                }
            } else {
                val fallbackCat = if (countBox.category == FruitHealthCategory.SADIO && cropFallbackClassifier != null) {
                    cropFallbackClassifier(countBox)
                } else {
                    countBox.category
                }
                countBox.copy(category = fallbackCat)
            }
        }.toMutableList()

        diseaseBoxes.forEachIndexed { idx, dBox ->
            if (!diseaseUsed[idx] && dBox.confidence >= diseaseMinConfidence) {
                val collidesExisting = merged.any { ex ->
                    iouNorm(
                        ex.normLeft, ex.normTop, ex.normRight, ex.normBottom,
                        dBox.normLeft, dBox.normTop, dBox.normRight, dBox.normBottom
                    ) >= iouMinimo
                }
                if (!collidesExisting) {
                    merged.add(dBox)
                }
            }
        }

        return merged
            .take(maxResults.coerceAtLeast(1))
            .mapIndexed { idx, box -> box.toDetected("f_$idx") }
    }

    fun iouNorm(
        ax1: Float, ay1: Float, ax2: Float, ay2: Float,
        bx1: Float, by1: Float, bx2: Float, by2: Float
    ): Float {
        val interW = max(0f, min(ax2, bx2) - max(ax1, bx1))
        val interH = max(0f, min(ay2, by2) - max(ay1, by1))
        val inter = interW * interH
        if (inter <= 0f) return 0f
        val areaA = max(0f, ax2 - ax1) * max(0f, ay2 - ay1)
        val areaB = max(0f, bx2 - bx1) * max(0f, by2 - by1)
        val union = areaA + areaB - inter
        return if (union <= 0f) 0f else inter / union
    }

    private fun FusionBoxInput.toDetected(id: String): DetectedFruitBox = DetectedFruitBox(
        id = id,
        normLeft = normLeft.coerceIn(0f, 1f),
        normTop = normTop.coerceIn(0f, 1f),
        normRight = normRight.coerceIn(0f, 1f),
        normBottom = normBottom.coerceIn(0f, 1f),
        category = category,
        confidence = confidence,
        isManualOverride = false
    )
}
