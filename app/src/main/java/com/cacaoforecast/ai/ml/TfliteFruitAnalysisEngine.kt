package com.cacaoforecast.ai.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.cacaoforecast.ai.domain.ml.DetectedFruitBox
import com.cacaoforecast.ai.domain.ml.FruitAnalysisEngine
import com.cacaoforecast.ai.domain.ml.FruitHealthCategory
import com.cacaoforecast.ai.domain.ml.PlantPhotoAnalysisResult
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors

/**
 * Motor de inferência TFLite session-scoped em thread dedicada (`cacao-ml-worker`),
 * com buffer 640x640 compartilhado entre `detector.tflite` e `detector_doenca.tflite`.
 */
class TfliteFruitAnalysisEngine(
    private val context: Context
) : FruitAnalysisEngine {

    private val workerExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "cacao-ml-worker").apply { isDaemon = true }
    }
    private val workerDispatcher = workerExecutor.asCoroutineDispatcher()

    private var countInterpreter: Interpreter? = null
    private var diseaseInterpreter: Interpreter? = null

    private var letterboxBitmap: Bitmap? = null
    private var letterboxPixels: IntArray? = null
    private var sharedInputBuffer: ByteBuffer? = null
    private var sharedFloatView: FloatBuffer? = null
    private var sharedFloatArray: FloatArray? = null

    private var countOutput: Array<Array<FloatArray>>? = null
    private var diseaseOutput: Array<Array<FloatArray>>? = null

    private val letterboxPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    override suspend fun prepareSession(): Result<Unit> = withContext(workerDispatcher) {
        runCatching {
            if (countInterpreter != null && diseaseInterpreter != null) return@runCatching

            val numThreads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 4)
            countInterpreter = createInterpreter(ASSET_COUNT_MODEL, numThreads)
            diseaseInterpreter = createInterpreter(ASSET_DISEASE_MODEL, numThreads)

            val inputSize = YoloTfliteInterpreter.INPUT_SIZE
            val totalPixels = inputSize * inputSize
            val neededFloats = totalPixels * 3

            if (letterboxBitmap == null) {
                letterboxBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
            }
            if (letterboxPixels == null) {
                letterboxPixels = IntArray(totalPixels)
            }
            if (sharedInputBuffer == null) {
                val buf = ByteBuffer.allocateDirect(neededFloats * 4).order(ByteOrder.nativeOrder())
                sharedInputBuffer = buf
                sharedFloatView = buf.asFloatBuffer()
            }
            if (sharedFloatArray == null) {
                sharedFloatArray = FloatArray(neededFloats)
            }

            if (countOutput == null) {
                val outShape = countInterpreter!!.getOutputTensor(0).shape() // [1, 5, 8400]
                countOutput = Array(1) { Array(outShape[1]) { FloatArray(outShape[2]) } }
            }
            if (diseaseOutput == null) {
                val outShape = diseaseInterpreter!!.getOutputTensor(0).shape() // [1, 8, 8400]
                diseaseOutput = Array(1) { Array(outShape[1]) { FloatArray(outShape[2]) } }
            }
        }.onFailure {
            releaseInternal()
        }
    }

    override suspend fun analyzePhoto(
        photoUri: Uri,
        countThreshold: Float,
        diseaseThreshold: Float,
        maxResults: Int
    ): Result<PlantPhotoAnalysisResult> = withContext(workerDispatcher) {
        runCatching {
            if (countInterpreter == null || diseaseInterpreter == null) {
                prepareSession().getOrThrow()
            }

            val tStart = System.currentTimeMillis()
            val bitmap = BitmapExifLoader.loadOrientedBitmap(context, photoUri, maxDim = 1600)
                ?: error("Não foi possível carregar a foto capturada.")

            try {
                val imgW = bitmap.width
                val imgH = bitmap.height
                val inputSize = YoloTfliteInterpreter.INPUT_SIZE
                val geometry = LetterboxGeometry.compute(imgW, imgH, inputSize)

                val lbBitmap = letterboxBitmap!!
                val canvas = Canvas(lbBitmap)
                canvas.drawColor(Color.rgb(114, 114, 114))
                val newW = kotlin.math.round(imgW * geometry.scale).toInt().coerceAtLeast(1)
                val newH = kotlin.math.round(imgH * geometry.scale).toInt().coerceAtLeast(1)
                val destRect = RectF(
                    geometry.padX,
                    geometry.padY,
                    geometry.padX + newW,
                    geometry.padY + newH
                )
                canvas.drawBitmap(bitmap, null, destRect, letterboxPaint)

                val pixels = letterboxPixels!!
                lbBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

                val fArr = sharedFloatArray!!
                var outIdx = 0
                for (px in pixels) {
                    fArr[outIdx++] = FLOAT_NORM_LUT[(px shr 16) and 0xFF]
                    fArr[outIdx++] = FLOAT_NORM_LUT[(px shr 8) and 0xFF]
                    fArr[outIdx++] = FLOAT_NORM_LUT[px and 0xFF]
                }

                val inputBuf = sharedInputBuffer!!
                val floatView = sharedFloatView!!
                floatView.rewind()
                floatView.put(fArr)
                inputBuf.rewind()

                val outCount = countOutput!!
                countInterpreter!!.run(inputBuf, outCount)

                inputBuf.rewind()
                val outDisease = diseaseOutput!!
                diseaseInterpreter!!.run(inputBuf, outDisease)

                val countShape = countInterpreter!!.getOutputTensor(0).shape()
                val countTransposed = countShape[1] < countShape[2]
                val countClasses = (minOf(countShape[1], countShape[2]) - 4).coerceAtLeast(1)
                val decodedCount = YoloTfliteInterpreter.decodeAndNms(
                    outputMatrix = outCount[0],
                    numClasses = countClasses,
                    transposed = countTransposed,
                    geometry = geometry,
                    scoreThreshold = countThreshold,
                    maxResults = maxResults,
                    letterboxPixels = null,
                    applyDiseaseColorRefinement = false
                )

                val diseaseShape = diseaseInterpreter!!.getOutputTensor(0).shape()
                val diseaseTransposed = diseaseShape[1] < diseaseShape[2]
                val diseaseClasses = (minOf(diseaseShape[1], diseaseShape[2]) - 4).coerceAtLeast(1)
                val rawMaxDiseaseProbs = computeMaxDiseaseProbs(
                    matrix = outDisease[0],
                    numClasses = diseaseClasses,
                    transposed = diseaseTransposed
                )
                val decodedDisease = YoloTfliteInterpreter.decodeAndNms(
                    outputMatrix = outDisease[0],
                    numClasses = diseaseClasses,
                    transposed = diseaseTransposed,
                    geometry = geometry,
                    scoreThreshold = diseaseThreshold.coerceAtMost(0.16f),
                    maxResults = maxResults,
                    letterboxPixels = pixels,
                    applyDiseaseColorRefinement = true
                )

                val countingInputs = decodedCount.map {
                    DualModelFusionProcessor.FusionBoxInput(
                        normLeft = it.normLeft,
                        normTop = it.normTop,
                        normRight = it.normRight,
                        normBottom = it.normBottom,
                        category = FruitHealthCategory.SADIO,
                        confidence = it.confidence
                    )
                }
                val diseaseInputs = decodedDisease.map {
                    DualModelFusionProcessor.FusionBoxInput(
                        normLeft = it.normLeft,
                        normTop = it.normTop,
                        normRight = it.normRight,
                        normBottom = it.normBottom,
                        category = it.category,
                        confidence = it.confidence
                    )
                }

                val initialFused = DualModelFusionProcessor.fuse(
                    countingBoxes = countingInputs,
                    diseaseBoxes = diseaseInputs,
                    iouMinimo = 0.20f,
                    diseaseMinConfidence = diseaseThreshold,
                    maxResults = maxResults,
                    cropFallbackClassifier = { box ->
                        CacaoColorHeuristics.classificarRecorteDoencaCacau(
                            bitmap,
                            RectF(
                                box.normLeft * imgW,
                                box.normTop * imgH,
                                box.normRight * imgW,
                                box.normBottom * imgH
                            )
                        )
                    }
                )

                val (fusedBoxes, mosaicPromoted) = refineHealthyBoxesWithDiseaseMosaic(
                    bitmap = bitmap,
                    boxes = initialFused,
                    diseaseInterpreter = diseaseInterpreter!!,
                    outDisease = outDisease,
                    diseaseClasses = diseaseClasses,
                    diseaseTransposed = diseaseTransposed,
                    scoreThreshold = diseaseThreshold.coerceAtMost(0.16f)
                )

                val countHealthy = fusedBoxes.count { !it.category.isDiseased }
                val countPodridao = fusedBoxes.count { it.category == FruitHealthCategory.PODRIDAO_PARDA }
                val countMoniliase = fusedBoxes.count { it.category == FruitHealthCategory.MONILIASE }
                val countVassoura = fusedBoxes.count { it.category == FruitHealthCategory.VASSOURA_BRUXA }
                val countDiseased = countPodridao + countMoniliase + countVassoura
                val elapsed = (System.currentTimeMillis() - tStart).coerceAtLeast(1L)

                runCatching {
                    Log.i(
                        "CacaoML",
                        String.format(
                            java.util.Locale.US,
                            "img=%dx%d countDet=%d globalDisDet=%d rawMax=[PP=%.2f, MO=%.2f, SA=%.2f, VB=%.2f] mosaicPromoted=%d final=[SA=%d, PP=%d, MO=%d, VB=%d] ms=%d",
                            imgW,
                            imgH,
                            decodedCount.size,
                            decodedDisease.size,
                            rawMaxDiseaseProbs[0],
                            rawMaxDiseaseProbs[1],
                            rawMaxDiseaseProbs[2],
                            rawMaxDiseaseProbs[3],
                            mosaicPromoted,
                            countHealthy,
                            countPodridao,
                            countMoniliase,
                            countVassoura,
                            elapsed
                        )
                    )
                }

                PlantPhotoAnalysisResult(
                    boxes = fusedBoxes,
                    countHealthy = countHealthy,
                    countDiseased = countDiseased,
                    countPodridaoParda = countPodridao,
                    countMoniliase = countMoniliase,
                    countVassouraBruxa = countVassoura,
                    imageWidthPx = imgW,
                    imageHeightPx = imgH,
                    inferenceTimeMs = elapsed
                )
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    /**
     * 2º estágio em alta resolução (Mosaico 2×2): reavalia até 16 frutos ainda classificados como
     * `SADIO` ampliando cada recorte para quadrantes de 320×320 px sobre o `DirectByteBuffer`
     * compartilhado (zero alocação extra de memória), garantindo sensibilidade a lesões pequenas.
     */
    private fun refineHealthyBoxesWithDiseaseMosaic(
        bitmap: Bitmap,
        boxes: List<DetectedFruitBox>,
        diseaseInterpreter: Interpreter,
        outDisease: Array<Array<FloatArray>>,
        diseaseClasses: Int,
        diseaseTransposed: Boolean,
        scoreThreshold: Float
    ): Pair<List<DetectedFruitBox>, Int> {
        val candidateIndices = boxes.indices
            .filter { idx -> boxes[idx].category == FruitHealthCategory.SADIO }
            .sortedByDescending { idx ->
                val b = boxes[idx]
                (b.normRight - b.normLeft) * (b.normBottom - b.normTop)
            }
            .take(MAX_MOSAIC_CROPS)

        if (candidateIndices.isEmpty()) return boxes to 0

        val imgW = bitmap.width
        val imgH = bitmap.height
        val inputSize = YoloTfliteInterpreter.INPUT_SIZE
        val tileSize = inputSize / 2f
        val lbBitmap = letterboxBitmap ?: return boxes to 0
        val pixels = letterboxPixels ?: return boxes to 0
        val fArr = sharedFloatArray ?: return boxes to 0
        val floatView = sharedFloatView ?: return boxes to 0
        val inputBuf = sharedInputBuffer ?: return boxes to 0
        val canvasGeometry = LetterboxGeometry.compute(inputSize, inputSize, inputSize)
        val canvas = Canvas(lbBitmap)
        val mutable = boxes.toMutableList()
        var promoted = 0

        val srcRect = Rect()
        val destRect = RectF()
        data class SlotPlacement(
            val boxIndex: Int,
            val qMinX: Float,
            val qMinY: Float,
            val qMaxX: Float,
            val qMaxY: Float,
            val innerLeftNorm: Float,
            val innerTopNorm: Float,
            val innerRightNorm: Float,
            val innerBottomNorm: Float
        )

        for (batch in candidateIndices.chunked(4)) {
            canvas.drawColor(Color.rgb(114, 114, 114))
            val placements = ArrayList<SlotPlacement>(batch.size)

            batch.forEachIndexed { slot, boxIdx ->
                val b = mutable[boxIdx]
                val leftPx = b.normLeft * imgW
                val topPx = b.normTop * imgH
                val rightPx = b.normRight * imgW
                val bottomPx = b.normBottom * imgH
                val boxW = (rightPx - leftPx).coerceAtLeast(4f)
                val boxH = (bottomPx - topPx).coerceAtLeast(4f)
                val padSrcX = boxW * 0.25f
                val padSrcY = boxH * 0.25f

                val sLeft = kotlin.math.floor(leftPx - padSrcX).toInt().coerceIn(0, (imgW - 2).coerceAtLeast(0))
                val sTop = kotlin.math.floor(topPx - padSrcY).toInt().coerceIn(0, (imgH - 2).coerceAtLeast(0))
                val sRight = kotlin.math.ceil(rightPx + padSrcX).toInt().coerceIn((sLeft + 2).coerceAtMost(imgW), imgW)
                val sBottom = kotlin.math.ceil(bottomPx + padSrcY).toInt().coerceIn((sTop + 2).coerceAtMost(imgH), imgH)
                srcRect.set(sLeft, sTop, sRight, sBottom)

                val cropW = (sRight - sLeft).coerceAtLeast(1).toFloat()
                val cropH = (sBottom - sTop).coerceAtLeast(1).toFloat()
                val tileScale = minOf(tileSize / cropW, tileSize / cropH)
                val drawW = cropW * tileScale
                val drawH = cropH * tileScale
                val qx = (slot % 2) * tileSize
                val qy = (slot / 2) * tileSize
                val tilePadX = (tileSize - drawW) / 2f
                val tilePadY = (tileSize - drawH) / 2f

                destRect.set(qx + tilePadX, qy + tilePadY, qx + tilePadX + drawW, qy + tilePadY + drawH)
                canvas.drawBitmap(bitmap, srcRect, destRect, letterboxPaint)

                placements.add(
                    SlotPlacement(
                        boxIndex = boxIdx,
                        qMinX = qx / inputSize,
                        qMinY = qy / inputSize,
                        qMaxX = (qx + tileSize) / inputSize,
                        qMaxY = (qy + tileSize) / inputSize,
                        innerLeftNorm = (qx + tilePadX + (leftPx - sLeft) * tileScale) / inputSize,
                        innerTopNorm = (qy + tilePadY + (topPx - sTop) * tileScale) / inputSize,
                        innerRightNorm = (qx + tilePadX + (rightPx - sLeft) * tileScale) / inputSize,
                        innerBottomNorm = (qy + tilePadY + (bottomPx - sTop) * tileScale) / inputSize
                    )
                )
            }

            lbBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            var outIdx = 0
            for (px in pixels) {
                fArr[outIdx++] = FLOAT_NORM_LUT[(px shr 16) and 0xFF]
                fArr[outIdx++] = FLOAT_NORM_LUT[(px shr 8) and 0xFF]
                fArr[outIdx++] = FLOAT_NORM_LUT[px and 0xFF]
            }
            floatView.rewind()
            floatView.put(fArr)
            inputBuf.rewind()
            diseaseInterpreter.run(inputBuf, outDisease)

            val mosaicDetections = YoloTfliteInterpreter.decodeAndNms(
                outputMatrix = outDisease[0],
                numClasses = diseaseClasses,
                transposed = diseaseTransposed,
                geometry = canvasGeometry,
                scoreThreshold = scoreThreshold,
                maxResults = 32,
                letterboxPixels = pixels,
                applyDiseaseColorRefinement = true
            )

            for (slot in placements) {
                val bestDisease = mosaicDetections
                    .asSequence()
                    .filter { det -> det.category.isDiseased }
                    .filter { det ->
                        val cx = (det.normLeft + det.normRight) * 0.5f
                        val cy = (det.normTop + det.normBottom) * 0.5f
                        cx in slot.qMinX..slot.qMaxX && cy in slot.qMinY..slot.qMaxY
                    }
                    .filter { det ->
                        DualModelFusionProcessor.iouNorm(
                            det.normLeft, det.normTop, det.normRight, det.normBottom,
                            slot.innerLeftNorm, slot.innerTopNorm, slot.innerRightNorm, slot.innerBottomNorm
                        ) >= 0.12f
                    }
                    .maxByOrNull { it.confidence }

                if (bestDisease != null) {
                    val orig = mutable[slot.boxIndex]
                    mutable[slot.boxIndex] = orig.copy(
                        category = bestDisease.category,
                        confidence = maxOf(orig.confidence, bestDisease.confidence)
                    )
                    promoted++
                }
            }
        }
        return mutable to promoted
    }

    private fun computeMaxDiseaseProbs(
        matrix: Array<FloatArray>,
        numClasses: Int,
        transposed: Boolean
    ): FloatArray {
        val maxes = FloatArray(4)
        val numAnchors = if (transposed) matrix.firstOrNull()?.size ?: 0 else matrix.size
        val classes = numClasses.coerceAtMost(4)
        for (a in 0 until numAnchors) {
            for (c in 0 until classes) {
                val v = if (transposed) matrix[4 + c][a] else matrix[a][4 + c]
                if (v > maxes[c]) maxes[c] = v
            }
        }
        return maxes
    }

    override fun releaseSession() {
        workerExecutor.execute {
            releaseInternal()
        }
    }

    private fun releaseInternal() {
        runCatching { countInterpreter?.close() }
        runCatching { diseaseInterpreter?.close() }
        countInterpreter = null
        diseaseInterpreter = null
        letterboxBitmap?.recycle()
        letterboxBitmap = null
        letterboxPixels = null
        sharedInputBuffer = null
        sharedFloatView = null
        sharedFloatArray = null
        countOutput = null
        diseaseOutput = null
    }

    private fun createInterpreter(assetName: String, numThreads: Int): Interpreter {
        val modelBuffer = loadMappedAsset(assetName)
        return try {
            val opts = Interpreter.Options().apply {
                setNumThreads(numThreads)
                setUseXNNPACK(true)
            }
            Interpreter(modelBuffer, opts)
        } catch (_: Throwable) {
            val fallbackOpts = Interpreter.Options().apply {
                setNumThreads(numThreads)
            }
            Interpreter(modelBuffer, fallbackOpts)
        }
    }

    private fun loadMappedAsset(assetName: String): MappedByteBuffer {
        context.assets.openFd(assetName).use { afd ->
            FileInputStream(afd.fileDescriptor).use { fis ->
                return fis.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength
                )
            }
        }
    }

    companion object {
        const val ASSET_COUNT_MODEL = "detector.tflite"
        const val ASSET_DISEASE_MODEL = "detector_doenca.tflite"
        private const val MAX_MOSAIC_CROPS = 16
        private val FLOAT_NORM_LUT = FloatArray(256) { it / 255f }
    }
}
