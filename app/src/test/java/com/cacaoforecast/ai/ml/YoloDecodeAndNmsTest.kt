package com.cacaoforecast.ai.ml

import com.cacaoforecast.ai.domain.ml.FruitHealthCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloDecodeAndNmsTest {

    @Test
    fun `decodifica tensor transposto 1x5x8400 de contagem desfazendo letterbox`() {
        // Imagem original 1600x1200 -> scale = 640/1600 = 0.4, newW=640, newH=480, padX=0, padY=80
        val geometry = LetterboxGeometry.compute(1600, 1200, 640)
        assertEquals(0.4f, geometry.scale, 1e-5f)
        assertEquals(0f, geometry.padX, 1e-5f)
        assertEquals(80f, geometry.padY, 1e-5f)

        val out = Array(5) { FloatArray(8400) }
        // Fruto no centro da imagem (cx=0.5, cy=0.5 em [0..1] no tensor 640x640)
        out[0][10] = 0.5f
        out[1][10] = 0.5f
        out[2][10] = 0.1f  // 64px no tensor -> 160px na imagem 1600px -> normW = 0.1
        out[3][10] = 0.15f // 96px no tensor -> 240px na imagem 1200px -> normH = 0.2
        out[4][10] = 0.85f

        val detections = YoloTfliteInterpreter.decodeAndNms(
            outputMatrix = out,
            numClasses = 1,
            transposed = true,
            geometry = geometry,
            scoreThreshold = 0.30f,
            maxResults = 100
        )

        assertEquals(1, detections.size)
        val box = detections.single()
        assertEquals(FruitHealthCategory.SADIO, box.category)
        assertEquals(0.85f, box.confidence, 1e-5f)
        assertEquals(0.45f, box.normLeft, 1e-4f)
        assertEquals(0.40f, box.normTop, 1e-4f)
        assertEquals(0.55f, box.normRight, 1e-4f)
        assertEquals(0.60f, box.normBottom, 1e-4f)
    }

    @Test
    fun `decodifica tensor 1x8x8400 com mapeamento canonico das 4 classes e aplica NMS`() {
        val geometry = LetterboxGeometry.compute(640, 640, 640)
        val out = Array(8) { FloatArray(8400) }

        // Anchor 1: PodridaoParda (classe 0) score 0.90
        out[0][1] = 0.25f; out[1][1] = 0.25f; out[2][1] = 0.12f; out[3][1] = 0.16f
        out[4][1] = 0.90f; out[5][1] = 0.02f; out[6][1] = 0.03f; out[7][1] = 0.01f

        // Anchor 2: Duplicata quase identica de PodridaoParda (deve ser suprimida pelo NMS)
        out[0][2] = 0.26f; out[1][2] = 0.25f; out[2][2] = 0.12f; out[3][2] = 0.16f
        out[4][2] = 0.72f

        // Anchor 3: Moniliase (classe 1) em outra posicao
        out[0][3] = 0.55f; out[1][3] = 0.50f; out[2][3] = 0.10f; out[3][3] = 0.15f
        out[5][3] = 0.80f

        // Anchor 4: Sadio (classe 2)
        out[0][4] = 0.75f; out[1][4] = 0.30f; out[2][4] = 0.10f; out[3][4] = 0.15f
        out[6][4] = 0.88f

        // Anchor 5: VassouraBruxa (classe 3)
        out[0][5] = 0.80f; out[1][5] = 0.75f; out[2][5] = 0.10f; out[3][5] = 0.14f
        out[7][5] = 0.77f

        val detections = YoloTfliteInterpreter.decodeAndNms(
            outputMatrix = out,
            numClasses = 4,
            transposed = true,
            geometry = geometry,
            scoreThreshold = 0.25f,
            maxResults = 100
        )

        assertEquals(4, detections.size)
        val categories = detections.map { it.category }.toSet()
        assertTrue(FruitHealthCategory.PODRIDAO_PARDA in categories)
        assertTrue(FruitHealthCategory.MONILIASE in categories)
        assertTrue(FruitHealthCategory.SADIO in categories)
        assertTrue(FruitHealthCategory.VASSOURA_BRUXA in categories)
    }

    @Test
    fun `permite mais de 25 deteccoes ate maxResults 100`() {
        val geometry = LetterboxGeometry.compute(640, 640, 640)
        val out = Array(5) { FloatArray(8400) }
        var anchor = 0
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                if (anchor >= 40) break
                out[0][anchor] = 0.08f + col * 0.11f
                out[1][anchor] = 0.08f + row * 0.11f
                out[2][anchor] = 0.05f
                out[3][anchor] = 0.05f
                out[4][anchor] = 0.80f
                anchor++
            }
        }

        val detections = YoloTfliteInterpreter.decodeAndNms(
            outputMatrix = out,
            numClasses = 1,
            transposed = true,
            geometry = geometry,
            scoreThreshold = 0.30f,
            maxResults = 100
        )

        assertEquals(40, detections.size)
    }
}
