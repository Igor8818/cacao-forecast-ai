package com.cacaoforecast.ai.ui.navigation

import android.net.Uri

object Routes {
    // Abas principais
    const val FARMS_LIST = "farms"
    const val SAMPLING_SETUP_PATTERN = "sampling/setup?farmId={farmId}&plotId={plotId}"
    const val REPORTS_HUB = "reports"

    // Fluxos secundarios
    const val FARM_PLOTS_PATTERN = "farms/{farmId}/plots"
    const val PLOT_HISTORY_PATTERN = "farms/{farmId}/plots/{plotId}/history"
    const val BACKUP_RESTORE = "backup"

    const val SAMPLING_CAMERA_PATTERN = "sampling/camera/{evaluationId}"
    const val PLANT_REVIEW_PATTERN = "sampling/review/{evaluationId}/{plantIndex}?photoUri={photoUri}"
    const val SAMPLING_SUMMARY_PATTERN = "sampling/summary/{evaluationId}"

    fun farmPlots(farmId: Long) = "farms/$farmId/plots"
    fun plotHistory(farmId: Long, plotId: Long) = "farms/$farmId/plots/$plotId/history"
    fun samplingSetup(farmId: Long? = null, plotId: Long? = null): String {
        val f = farmId ?: -1L
        val p = plotId ?: -1L
        return "sampling/setup?farmId=$f&plotId=$p"
    }

    fun samplingCamera(evaluationId: Long) = "sampling/camera/$evaluationId"
    fun plantReview(evaluationId: Long, plantIndex: Int, photoUri: String): String {
        return "sampling/review/$evaluationId/$plantIndex?photoUri=${Uri.encode(photoUri)}"
    }

    fun samplingSummary(evaluationId: Long) = "sampling/summary/$evaluationId"
}
