package com.cacaoforecast.ai.ml

import com.cacaoforecast.ai.domain.ml.FruitHealthCategory
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class LetterboxGeometry(
    val inputSize: Int,
    val srcWidth: Int,
    val srcHeight: Int,
    val scale: Float,
    val padX: Float,
    val padY: Float
) {
    companion object {
        fun compute(srcWidth: Int, srcHeight: Int, inputSize: Int = 640): LetterboxGeometry {
            val safeW = srcWidth.coerceAtLeast(1)
            val safeH = srcHeight.coerceAtLeast(1)
            val scale = min(inputSize.toFloat() / safeW, inputSize.toFloat() / safeH)
            val newW = kotlin.math.round(safeW * scale).toInt().coerceAtLeast(1)
            val newH = kotlin.math.round(safeH * scale).toInt().coerceAtLeast(1)
            val padX = (inputSize - newW) / 2f
            val padY = (inputSize - newH) / 2f
            return LetterboxGeometry(
                inputSize = inputSize,
                srcWidth = safeW,
                srcHeight = safeH,
                scale = scale,
                padX = padX,
                padY = padY
            )
        }
    }
}

data class DecodedYoloBox(
    val normLeft: Float,
    val normTop: Float,
    val normRight: Float,
    val normBottom: Float,
    val confidence: Float,
    val classIndex: Int,
    val category: FruitHealthCategory,
    val tensorX1: Float,
    val tensorY1: Float,
    val tensorX2: Float,
    val tensorY2: Float,
    val probClass0: Float = 0f,
    val probClass1: Float = 0f,
    val probClass2: Float = 0f,
    val probClass3: Float = 0f
)

object YoloTfliteInterpreter {

    const val INPUT_SIZE = 640
    const val DEFAULT_ANCHORS = 8400

    /** Ordem canônica fixa das 4 classes do modelo CEPEC (`detector_doenca.tflite`). */
    val DISEASE_4CLASS_ORDER: List<FruitHealthCategory> = listOf(
        FruitHealthCategory.PODRIDAO_PARDA, // 0
        FruitHealthCategory.MONILIASE,      // 1
        FruitHealthCategory.SADIO,          // 2
        FruitHealthCategory.VASSOURA_BRUXA  // 3
    )

    fun mapClassIndexToCategory(classIndex: Int, numClasses: Int): FruitHealthCategory {
        if (numClasses == 4) {
            return DISEASE_4CLASS_ORDER.getOrElse(classIndex) { FruitHealthCategory.SADIO }
        }
        return FruitHealthCategory.SADIO
    }

    /**
     * Decodifica matriz raw YOLO `[1, 4+C, 8400]` (transposta) ou `[1, 8400, 4+C]`,
     * aplica NMS multiclasse (`0.45f` mesma classe / `0.55f` entre classes), desfaz o letterbox
     * e retorna caixas normalizadas `[0f..1f]` relativas à imagem orientada original.
     */
    fun decodeAndNms(
        outputMatrix: Array<FloatArray>,
        numClasses: Int,
        transposed: Boolean = true,
        geometry: LetterboxGeometry,
        scoreThreshold: Float,
        maxResults: Int = 100,
        minBoxNorm: Float = 0.004f,
        letterboxPixels: IntArray? = null,
        applyDiseaseColorRefinement: Boolean = false
    ): List<DecodedYoloBox> {
        val numAnchors = if (transposed) {
            outputMatrix.firstOrNull()?.size ?: 0
        } else {
            outputMatrix.size
        }
        if (numAnchors <= 0 || numClasses <= 0) return emptyList()

        val candidates = decodeYoloRawCandidates(
            out = outputMatrix,
            numAnchors = numAnchors,
            numClasses = numClasses,
            transposed = transposed,
            inputWidth = geometry.inputSize,
            inputHeight = geometry.inputSize,
            scoreThreshold = scoreThreshold,
            minBoxNorm = minBoxNorm
        )
        if (candidates.isEmpty()) return emptyList()

        val nmsBoxes = nmsMultiClasse(
            boxes = candidates,
            iouMesmaClasse = 0.45f,
            iouEntreClasses = 0.55f
        ).take(maxResults.coerceAtLeast(1))

        val imgW = geometry.srcWidth.toFloat()
        val imgH = geometry.srcHeight.toFloat()
        val outList = ArrayList<DecodedYoloBox>(nmsBoxes.size)

        for (box in nmsBoxes) {
            val x1Tensor = (box[0] - box[2] / 2f) * geometry.inputSize
            val y1Tensor = (box[1] - box[3] / 2f) * geometry.inputSize
            val x2Tensor = (box[0] + box[2] / 2f) * geometry.inputSize
            val y2Tensor = (box[1] + box[3] / 2f) * geometry.inputSize

            val leftPx = ((x1Tensor - geometry.padX) / geometry.scale).coerceIn(0f, imgW)
            val topPx = ((y1Tensor - geometry.padY) / geometry.scale).coerceIn(0f, imgH)
            val rightPx = ((x2Tensor - geometry.padX) / geometry.scale).coerceIn(0f, imgW)
            val bottomPx = ((y2Tensor - geometry.padY) / geometry.scale).coerceIn(0f, imgH)
            if (rightPx <= leftPx || bottomPx <= topPx) continue

            val rawClassIdx = box[5].toInt()
            val probBest = box[4]
            val p0 = if (box.size > 6) box[6] else 0f
            val p1 = if (box.size > 7) box[7] else 0f
            val p2 = if (box.size > 8) box[8] else 0f
            val p3 = if (box.size > 9) box[9] else 0f

            val refinedIdx = if (applyDiseaseColorRefinement && numClasses == 4 && letterboxPixels != null) {
                CacaoColorHeuristics.refinarClasseDoencaPorMorfologiaCor(
                    classIdx = rawClassIdx,
                    probBest = probBest,
                    probClass0 = p0,
                    pixels = letterboxPixels,
                    width = geometry.inputSize,
                    height = geometry.inputSize,
                    x1Tensor = x1Tensor,
                    y1Tensor = y1Tensor,
                    x2Tensor = x2Tensor,
                    y2Tensor = y2Tensor,
                    probClass1 = p1,
                    probClass3 = p3
                )
            } else {
                rawClassIdx
            }

            outList.add(
                DecodedYoloBox(
                    normLeft = (leftPx / imgW).coerceIn(0f, 1f),
                    normTop = (topPx / imgH).coerceIn(0f, 1f),
                    normRight = (rightPx / imgW).coerceIn(0f, 1f),
                    normBottom = (bottomPx / imgH).coerceIn(0f, 1f),
                    confidence = probBest,
                    classIndex = refinedIdx,
                    category = mapClassIndexToCategory(refinedIdx, numClasses),
                    tensorX1 = x1Tensor,
                    tensorY1 = y1Tensor,
                    tensorX2 = x2Tensor,
                    tensorY2 = y2Tensor,
                    probClass0 = p0,
                    probClass1 = p1,
                    probClass2 = p2,
                    probClass3 = p3
                )
            )
        }
        return outList
    }

    internal fun decodeYoloRawCandidates(
        out: Array<FloatArray>,
        numAnchors: Int,
        numClasses: Int,
        transposed: Boolean,
        inputWidth: Int,
        inputHeight: Int,
        scoreThreshold: Float,
        minBoxNorm: Float = 0.004f
    ): List<FloatArray> {
        if (numAnchors <= 0 || numClasses <= 0) return emptyList()

        fun readVal(coordOrClass: Int, anchor: Int): Float =
            if (transposed) out[coordOrClass][anchor] else out[anchor][coordOrClass]

        val lastIdx = numAnchors - 1
        val midIdx = numAnchors / 2
        val sampleIndices = LinkedHashSet<Int>(68).apply {
            add(0)
            add(midIdx)
            add(lastIdx)
            val step = max(1, numAnchors / 64)
            var idx = 0
            while (idx < numAnchors) {
                add(idx)
                idx += step
            }
        }

        var maxSampleCoord = 0f
        var minSampleScore = Float.MAX_VALUE
        var maxSampleScore = -Float.MAX_VALUE
        for (idx in sampleIndices) {
            for (coord in 0..3) {
                val cv = readVal(coord, idx)
                if (cv > maxSampleCoord) maxSampleCoord = cv
            }
            for (c in 0 until numClasses) {
                val v = readVal(4 + c, idx)
                if (v < minSampleScore) minSampleScore = v
                if (v > maxSampleScore) maxSampleScore = v
            }
        }
        val coordsArePixels = maxSampleCoord > 2.0f
        val scaleX = if (coordsArePixels) 1f / inputWidth.coerceAtLeast(1).toFloat() else 1f
        val scaleY = if (coordsArePixels) 1f / inputHeight.coerceAtLeast(1).toFloat() else 1f
        val needsSigmoid = minSampleScore < -0.001f || maxSampleScore > 1.001f

        val candidates = mutableListOf<FloatArray>()
        for (i in 0 until numAnchors) {
            var rawScore = -Float.MAX_VALUE
            var bestClass = 0
            for (c in 0 until numClasses) {
                val s = readVal(4 + c, i)
                if (s > rawScore) {
                    rawScore = s
                    bestClass = c
                }
            }
            val prob = if (needsSigmoid) sigmoidFn(rawScore) else rawScore.coerceIn(0f, 1f)
            if (prob < scoreThreshold) continue

            val normCx = readVal(0, i) * scaleX
            val normCy = readVal(1, i) * scaleY
            val normW = readVal(2, i) * scaleX
            val normH = readVal(3, i) * scaleY

            if (normW > minBoxNorm && normH > minBoxNorm &&
                normW < 0.98f && normH < 0.98f &&
                normCx > 0.002f && normCy > 0.002f &&
                normCx < 0.998f && normCy < 0.998f
            ) {
                if (numClasses == 4) {
                    val p0 = if (needsSigmoid) sigmoidFn(readVal(4, i)) else readVal(4, i).coerceIn(0f, 1f)
                    val p1 = if (needsSigmoid) sigmoidFn(readVal(5, i)) else readVal(5, i).coerceIn(0f, 1f)
                    val p2 = if (needsSigmoid) sigmoidFn(readVal(6, i)) else readVal(6, i).coerceIn(0f, 1f)
                    val p3 = if (needsSigmoid) sigmoidFn(readVal(7, i)) else readVal(7, i).coerceIn(0f, 1f)
                    candidates.add(floatArrayOf(normCx, normCy, normW, normH, prob, bestClass.toFloat(), p0, p1, p2, p3))
                } else {
                    val rawClass0 = readVal(4, i)
                    val p0 = if (needsSigmoid) sigmoidFn(rawClass0) else rawClass0.coerceIn(0f, 1f)
                    candidates.add(floatArrayOf(normCx, normCy, normW, normH, prob, bestClass.toFloat(), p0))
                }
            }
        }
        return candidates
    }

    internal fun nmsMultiClasse(
        boxes: List<FloatArray>,
        iouMesmaClasse: Float = 0.45f,
        iouEntreClasses: Float = 0.55f
    ): List<FloatArray> {
        val sorted = boxes.sortedByDescending { it[4] }.toMutableList()
        val result = mutableListOf<FloatArray>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)
            val bestCls = best[5].toInt()
            sorted.removeAll { other ->
                val thresh = if (other[5].toInt() == bestCls) iouMesmaClasse else iouEntreClasses
                iouCaixas(best, other) > thresh
            }
        }
        return result
    }

    private fun iouCaixas(a: FloatArray, b: FloatArray): Float {
        val ax1 = a[0] - a[2] / 2f
        val ay1 = a[1] - a[3] / 2f
        val ax2 = a[0] + a[2] / 2f
        val ay2 = a[1] + a[3] / 2f
        val bx1 = b[0] - b[2] / 2f
        val by1 = b[1] - b[3] / 2f
        val bx2 = b[0] + b[2] / 2f
        val by2 = b[1] + b[3] / 2f
        val inter = max(0f, min(ax2, bx2) - max(ax1, bx1)) * max(0f, min(ay2, by2) - max(ay1, by1))
        val union = (ax2 - ax1) * (ay2 - ay1) + (bx2 - bx1) * (by2 - by1) - inter
        return if (union <= 0f) 0f else inter / union
    }

    private fun sigmoidFn(x: Float): Float = (1.0 / (1.0 + exp(-x.toDouble()))).toFloat()
}
