package com.cacaoforecast.ai.ml

import android.graphics.Bitmap
import android.graphics.RectF
import com.cacaoforecast.ai.domain.ml.FruitHealthCategory

object CacaoColorHeuristics {

    /**
     * Refinamento morfológico e colorimétrico para o modelo CEPEC de 4 classes
     * (0=PodridaoParda, 1=Moniliase, 2=Sadio, 3=VassouraBruxa).
     */
    fun refinarClasseDoencaPorMorfologiaCor(
        classIdx: Int,
        probBest: Float,
        probClass0: Float,
        pixels: IntArray,
        width: Int,
        height: Int,
        x1Tensor: Float,
        y1Tensor: Float,
        x2Tensor: Float,
        y2Tensor: Float,
        probClass1: Float = 0f,
        probClass3: Float = 0f
    ): Int {
        val sadioComCompeticao =
            classIdx == 2 && (probBest < 0.78f && (probClass0 >= 0.16f || probClass1 >= 0.16f || probClass3 >= 0.14f))
        if (classIdx != 0 && classIdx != 1 && !sadioComCompeticao) {
            return classIdx
        }
        val bw = x2Tensor - x1Tensor
        val bh = y2Tensor - y1Tensor
        if (bw < 4f || bh < 4f || width <= 0 || height <= 0) return classIdx

        val startX = (x1Tensor + bw * 0.15f).toInt().coerceIn(0, width - 1)
        val endX = (x2Tensor - bw * 0.15f).toInt().coerceIn(startX + 1, width)
        val startY = (y1Tensor + bh * 0.15f).toInt().coerceIn(0, height - 1)
        val endY = (y2Tensor - bh * 0.15f).toInt().coerceIn(startY + 1, height)

        var rSum = 0f
        var bSum = 0f
        var satSum = 0f
        var valSum = 0f
        var darkMatteCount = 0
        var rustCount = 0
        var whiteSporeCount = 0
        var count = 0

        val stepY = ((endY - startY) / 24).coerceAtLeast(1)
        val stepX = ((endX - startX) / 24).coerceAtLeast(1)

        var y = startY
        while (y < endY) {
            val rowOffset = y * width
            var x = startX
            while (x < endX) {
                val px = pixels[rowOffset + x]
                val r = ((px shr 16) and 0xFF) / 255f
                val g = ((px shr 8) and 0xFF) / 255f
                val b = (px and 0xFF) / 255f
                val cmax = maxOf(r, g, b)
                val cmin = minOf(r, g, b)
                val sat = if (cmax > 0.001f) (cmax - cmin) / cmax else 0f
                val rb = r - b
                if (cmax < 0.42f || (cmax < 0.58f && sat < 0.38f && rb < 0.22f)) {
                    darkMatteCount++
                }
                if (r > g * 1.15f && cmax in 0.22f..0.62f && sat >= 0.30f && rb >= 0.14f) {
                    rustCount++
                }
                if (cmax > 0.72f && sat < 0.22f) {
                    whiteSporeCount++
                }
                rSum += r
                bSum += b
                satSum += sat
                valSum += cmax
                count++
                x += stepX
            }
            y += stepY
        }

        if (count == 0) return classIdx
        val rMean = rSum / count
        val bMean = bSum / count
        val satMean = satSum / count
        val valMean = valSum / count
        val rbDiff = rMean - bMean
        val darkMatteRatio = darkMatteCount.toFloat() / count.toFloat()
        val rustRatio = rustCount.toFloat() / count.toFloat()
        val whiteSporeRatio = whiteSporeCount.toFloat() / count.toFloat()

        if (whiteSporeRatio >= 0.24f && darkMatteRatio >= 0.12f && (classIdx == 0 || classIdx == 1 || probClass1 >= 0.15f)) {
            return 1
        }

        val isColdGrayDry = rbDiff < 0.145f && satMean < 0.265f && valMean < 0.68f
        val isDarkBrownDry = valMean < 0.46f && satMean < 0.45f && rbDiff < 0.24f
        val isPredominantlyNecroticMatte = darkMatteRatio >= 0.45f && valMean < 0.62f && rbDiff < 0.22f
        val isRustMummified = rustRatio >= 0.26f && darkMatteRatio >= 0.24f && valMean < 0.62f

        return when (classIdx) {
            0 -> if (isColdGrayDry || (probClass3 >= 0.08f && (isDarkBrownDry || isPredominantlyNecroticMatte || isRustMummified))) 3 else 0
            1 -> if ((isColdGrayDry || isPredominantlyNecroticMatte) && whiteSporeRatio < 0.12f && probBest < 0.65f) 3 else 1
            else -> {
                val maxDiseaseProb = maxOf(probClass0, probClass1, probClass3)
                val maxDiseaseIdx = when {
                    probClass1 >= probClass0 && probClass1 >= probClass3 -> 1
                    probClass3 >= probClass0 && probClass3 >= probClass1 -> 3
                    else -> 0
                }
                when {
                    probClass1 >= 0.16f && whiteSporeRatio >= 0.20f && (darkMatteRatio >= 0.10f || rustRatio >= 0.10f) -> 1
                    (rbDiff < 0.10f && satMean < 0.20f && valMean < 0.58f) ||
                        (darkMatteRatio >= 0.55f && valMean < 0.52f && rbDiff < 0.18f) ||
                        (isRustMummified && probClass3 >= 0.14f) -> 3
                    probClass0 >= 0.16f && (rustRatio >= 0.20f || (darkMatteRatio >= 0.30f && rbDiff >= 0.12f)) -> 0
                    maxDiseaseProb >= 0.24f && maxDiseaseProb >= probBest * 0.65f -> maxDiseaseIdx
                    else -> classIdx
                }
            }
        }
    }

    /**
     * Classificação conservadora assistida por recorte quando o detector de doenças não casou
     * com um fruto encontrado pelo modelo de contagem geral.
     */
    fun classificarRecorteDoencaCacau(bmp: Bitmap, rect: RectF): FruitHealthCategory {
        if (bmp.isRecycled) return FruitHealthCategory.SADIO
        val w = bmp.width
        val h = bmp.height
        if (w <= 0 || h <= 0) return FruitHealthCategory.SADIO
        val x1 = rect.left.coerceIn(0f, (w - 1).toFloat())
        val y1 = rect.top.coerceIn(0f, (h - 1).toFloat())
        val x2 = rect.right.coerceIn(x1 + 1f, w.toFloat())
        val y2 = rect.bottom.coerceIn(y1 + 1f, h.toFloat())
        val bw = x2 - x1
        val bh = y2 - y1
        if (bw < 4f || bh < 4f) return FruitHealthCategory.SADIO

        val startX = (x1 + bw * 0.2f).toInt().coerceIn(0, w - 1)
        val endX = (x2 - bw * 0.2f).toInt().coerceIn(startX + 1, w)
        val startY = (y1 + bh * 0.2f).toInt().coerceIn(0, h - 1)
        val endY = (y2 - bh * 0.2f).toInt().coerceIn(startY + 1, h)
        val stepY = ((endY - startY) / 16).coerceAtLeast(1)
        val stepX = ((endX - startX) / 16).coerceAtLeast(1)

        var darkCount = 0
        var whiteSporeCount = 0
        var brownCount = 0
        var healthyGreenYellowCount = 0
        var count = 0

        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val px = bmp.getPixel(x, y)
                val r = ((px shr 16) and 0xFF) / 255f
                val g = ((px shr 8) and 0xFF) / 255f
                val b = (px and 0xFF) / 255f
                val cmax = maxOf(r, g, b)
                val cmin = minOf(r, g, b)
                val sat = if (cmax > 0.001f) (cmax - cmin) / cmax else 0f
                val rb = r - b

                if (cmax < 0.36f || (cmax < 0.50f && sat < 0.28f && rb < 0.16f)) darkCount++
                if (cmax > 0.74f && sat < 0.20f) whiteSporeCount++
                if (r > g * 1.15f && cmax in 0.20f..0.70f && rb >= 0.14f && sat >= 0.26f) brownCount++
                if (g >= r * 0.88f && cmax >= 0.32f && (g - b) >= 0.12f) healthyGreenYellowCount++
                count++
                x += stepX
            }
            y += stepY
        }
        if (count == 0) return FruitHealthCategory.SADIO
        val darkRatio = darkCount.toFloat() / count
        val sporeRatio = whiteSporeCount.toFloat() / count
        val brownRatio = brownCount.toFloat() / count
        val healthyRatio = healthyGreenYellowCount.toFloat() / count

        return when {
            sporeRatio >= 0.24f && (darkRatio >= 0.12f || brownRatio >= 0.12f) -> FruitHealthCategory.MONILIASE
            darkRatio >= 0.52f && brownRatio < 0.25f -> FruitHealthCategory.VASSOURA_BRUXA
            (brownRatio >= 0.42f && darkRatio < 0.35f) ||
                (brownRatio >= 0.22f && (brownRatio + darkRatio) >= 0.55f && healthyRatio < 0.35f) -> FruitHealthCategory.PODRIDAO_PARDA
            darkRatio >= 0.55f -> FruitHealthCategory.VASSOURA_BRUXA
            else -> FruitHealthCategory.SADIO
        }
    }
}
