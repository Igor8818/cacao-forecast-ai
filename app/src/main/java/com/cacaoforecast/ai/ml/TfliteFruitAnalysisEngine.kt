package com.cacaoforecast.ai.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
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
                val decodedDisease = YoloTfliteInterpreter.decodeAndNms(
                    outputMatrix = outDisease[0],
                    numClasses = diseaseClasses,
                    transposed = diseaseTransposed,
                    geometry = geometry,
                    scoreThreshold = diseaseThreshold.coerceAtMost(0.28f),
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

                val fusedBoxes = DualModelFusionProcessor.fuse(
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

                val countHealthy = fusedBoxes.count { !it.category.isDiseased }
                val countPodridao = fusedBoxes.count { it.category == FruitHealthCategory.PODRIDAO_PARDA }
                val countMoniliase = fusedBoxes.count { it.category == FruitHealthCategory.MONILIASE }
                val countVassoura = fusedBoxes.count { it.category == FruitHealthCategory.VASSOURA_BRUXA }
                val countDiseased = countPodridao + countMoniliase + countVassoura
                val elapsed = (System.currentTimeMillis() - tStart).coerceAtLeast(1L)

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
        private val FLOAT_NORM_LUT = FloatArray(256) { it / 255f }
    }
}
