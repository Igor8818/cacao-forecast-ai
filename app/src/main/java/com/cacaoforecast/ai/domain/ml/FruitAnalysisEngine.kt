package com.cacaoforecast.ai.domain.ml

import android.net.Uri

enum class FruitHealthCategory(val isDiseased: Boolean) {
    SADIO(isDiseased = false),
    PODRIDAO_PARDA(isDiseased = true),
    MONILIASE(isDiseased = true),
    VASSOURA_BRUXA(isDiseased = true)
}

data class DetectedFruitBox(
    val id: String,
    val normLeft: Float,   // [0f..1f] relativo ao bitmap orientado
    val normTop: Float,    // [0f..1f]
    val normRight: Float,  // [0f..1f]
    val normBottom: Float, // [0f..1f]
    val category: FruitHealthCategory,
    val confidence: Float,
    val isManualOverride: Boolean = false
)

data class PlantPhotoAnalysisResult(
    val boxes: List<DetectedFruitBox>,
    val countHealthy: Int,        // NFS
    val countDiseased: Int,       // NFD (soma das 3 doenças)
    val countPodridaoParda: Int,
    val countMoniliase: Int,
    val countVassouraBruxa: Int,
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val inferenceTimeMs: Long
) {
    val countTotal: Int get() = countHealthy + countDiseased // NFT
}

interface FruitAnalysisEngine {
    /** Pré-aquece os 2 modelos TFLite na thread dedicada antes da planta 1/NPF. */
    suspend fun prepareSession(): Result<Unit>

    /** Analisa uma foto capturada executando contagem + doenças + fusão IoU em passo único. */
    suspend fun analyzePhoto(
        photoUri: Uri,
        countThreshold: Float = 0.30f,
        diseaseThreshold: Float = 0.25f,
        maxResults: Int = 100
    ): Result<PlantPhotoAnalysisResult>

    /** Libera Interpreters e buffers diretos ao concluir ou sair da sessão de amostragem. */
    fun releaseSession()
}
