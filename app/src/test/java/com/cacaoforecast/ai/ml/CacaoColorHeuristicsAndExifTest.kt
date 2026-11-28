package com.cacaoforecast.ai.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.cacaoforecast.ai.domain.ml.DetectedFruitBox
import com.cacaoforecast.ai.domain.ml.FruitHealthCategory
import com.cacaoforecast.ai.domain.ml.PlantPhotoAnalysisResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CacaoColorHeuristicsAndExifTest {

    @Test
    fun `refinarClasseDoencaPorMorfologiaCor promove para Moniliase e Vassoura e preserva Sadio confiante`() {
        val w = 100
        val h = 100

        // 1. Esporos brancos (metade esquerda) + fundo escuro fosco (metade direita) -> Moniliase (1)
        val moniliasePixels = IntArray(w * h) { idx ->
            val x = idx % w
            if (x < w / 2) Color.rgb(230, 230, 230) else Color.rgb(50, 45, 45)
        }
        val refinedMoniliase = CacaoColorHeuristics.refinarClasseDoencaPorMorfologiaCor(
            classIdx = 0,
            probBest = 0.55f,
            probClass0 = 0.55f,
            pixels = moniliasePixels,
            width = w,
            height = h,
            x1Tensor = 10f,
            y1Tensor = 10f,
            x2Tensor = 90f,
            y2Tensor = 90f,
            probClass1 = 0.25f
        )
        assertEquals(1, refinedMoniliase)

        // 2. Tecido cinza-frio seco/necrotico -> VassouraBruxa (3)
        val vassouraPixels = IntArray(w * h) { Color.rgb(70, 68, 66) }
        val refinedVassoura = CacaoColorHeuristics.refinarClasseDoencaPorMorfologiaCor(
            classIdx = 0,
            probBest = 0.60f,
            probClass0 = 0.60f,
            pixels = vassouraPixels,
            width = w,
            height = h,
            x1Tensor = 10f,
            y1Tensor = 10f,
            x2Tensor = 90f,
            y2Tensor = 90f
        )
        assertEquals(3, refinedVassoura)

        // 3. Fruto Sadio com alta confianca (>= 0.78) nao e rebaixado
        val keptHealthy = CacaoColorHeuristics.refinarClasseDoencaPorMorfologiaCor(
            classIdx = 2,
            probBest = 0.85f,
            probClass0 = 0.05f,
            pixels = vassouraPixels,
            width = w,
            height = h,
            x1Tensor = 10f,
            y1Tensor = 10f,
            x2Tensor = 90f,
            y2Tensor = 90f
        )
        assertEquals(2, keptHealthy)

        // 4. Sadio incerto (< 0.78) competindo com Podridao Parda (0 >= 0.16) + cor marrom/ferrugem -> Podridao Parda (0)
        val podridaoPixels = IntArray(w * h) { Color.rgb(138, 70, 40) }
        val promotedPodridao = CacaoColorHeuristics.refinarClasseDoencaPorMorfologiaCor(
            classIdx = 2,
            probBest = 0.36f,
            probClass0 = 0.22f,
            pixels = podridaoPixels,
            width = w,
            height = h,
            x1Tensor = 10f,
            y1Tensor = 10f,
            x2Tensor = 90f,
            y2Tensor = 90f
        )
        assertEquals(0, promotedPodridao)

        // 5. Podridao Parda marrom-escura com probClass3 == 0 nao deve virar Vassoura
        val podridaoEscuraPixels = IntArray(w * h) { Color.rgb(90, 48, 28) }
        val keptPodridao = CacaoColorHeuristics.refinarClasseDoencaPorMorfologiaCor(
            classIdx = 0,
            probBest = 0.41f,
            probClass0 = 0.41f,
            pixels = podridaoEscuraPixels,
            width = w,
            height = h,
            x1Tensor = 10f,
            y1Tensor = 10f,
            x2Tensor = 90f,
            y2Tensor = 90f,
            probClass3 = 0f
        )
        assertEquals(0, keptPodridao)
    }

    @Test
    fun `classificarRecorteDoencaCacau classifica Moniliase Vassoura Podridao e Sadio por recorte`() {
        val rect = RectF(0f, 0f, 40f, 40f)

        // Moniliase: >= 24% esporos brancos + >= 12% escuro/marrom
        val bmpMoniliase = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        for (y in 0 until 40) {
            for (x in 0 until 40) {
                bmpMoniliase.setPixel(x, y, if (x % 2 == 0) Color.rgb(235, 235, 235) else Color.rgb(40, 35, 35))
            }
        }
        assertEquals(FruitHealthCategory.MONILIASE, CacaoColorHeuristics.classificarRecorteDoencaCacau(bmpMoniliase, rect))
        bmpMoniliase.recycle()

        // Vassoura de Bruxa: predominantemente escuro necrotico (>= 52%)
        val bmpVassoura = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(45, 42, 40))
        }
        assertEquals(FruitHealthCategory.VASSOURA_BRUXA, CacaoColorHeuristics.classificarRecorteDoencaCacau(bmpVassoura, rect))
        bmpVassoura.recycle()

        // Podridao Parda: marrom avermelhado (r > g*1.15, cmax 0.20..0.70)
        val bmpPodridao = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(145, 75, 45))
        }
        assertEquals(FruitHealthCategory.PODRIDAO_PARDA, CacaoColorHeuristics.classificarRecorteDoencaCacau(bmpPodridao, rect))
        bmpPodridao.recycle()

        // Podridao Parda mista: ~38% marrom + ~38% escuro necrotico + 24% verde residual (sem falso sadio)
        val bmpPodridaoMista = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        for (y in 0 until 40) {
            for (x in 0 until 40) {
                val c = when ((x + y) % 8) {
                    0, 1, 2 -> Color.rgb(140, 72, 42) // marrom lesionado
                    3, 4, 5 -> Color.rgb(45, 35, 30)  // necrose escura
                    else -> Color.rgb(110, 130, 50)   // casca residual
                }
                bmpPodridaoMista.setPixel(x, y, c)
            }
        }
        assertEquals(FruitHealthCategory.PODRIDAO_PARDA, CacaoColorHeuristics.classificarRecorteDoencaCacau(bmpPodridaoMista, rect))
        bmpPodridaoMista.recycle()

        // Sadio: verde/amarelo vibrante
        val bmpSadio = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(210, 190, 40))
        }
        assertEquals(FruitHealthCategory.SADIO, CacaoColorHeuristics.classificarRecorteDoencaCacau(bmpSadio, rect))
        bmpSadio.recycle()
    }

    @Test
    fun `BitmapExifLoader aplica rotacao EXIF 90 graus e respeita maxDim`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempJpg = File(context.cacheDir, "exif_test_rot90.jpg").apply { delete() }
        try {
            val orig = Bitmap.createBitmap(120, 60, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.GREEN)
            }
            FileOutputStream(tempJpg).use { fos ->
                orig.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            orig.recycle()

            ExifInterface(tempJpg.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }

            val loaded = BitmapExifLoader.loadOrientedBitmap(context, Uri.fromFile(tempJpg), maxDim = 1600)
            assertNotNull(loaded)
            assertEquals(60, loaded!!.width)
            assertEquals(120, loaded.height)
            loaded.recycle()
        } finally {
            tempJpg.delete()
        }
    }

    @Test
    fun `modelos TFLite oficiais existem em assets com tamanhos exatos e cabecalho TFL3`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val countBytes = context.assets.open(TfliteFruitAnalysisEngine.ASSET_COUNT_MODEL).use { it.readBytes() }
        val diseaseBytes = context.assets.open(TfliteFruitAnalysisEngine.ASSET_DISEASE_MODEL).use { it.readBytes() }

        assertEquals(37_839_248, countBytes.size)
        assertEquals(10_511_948, diseaseBytes.size)
        assertEquals("TFL3", String(countBytes.copyOfRange(4, 8), Charsets.US_ASCII))
        assertEquals("TFL3", String(diseaseBytes.copyOfRange(4, 8), Charsets.US_ASCII))
    }

    @Test
    fun `contratos FruitHealthCategory e PlantPhotoAnalysisResult respeitam Seção 2_4`() {
        assertFalse(FruitHealthCategory.SADIO.isDiseased)
        assertTrue(FruitHealthCategory.PODRIDAO_PARDA.isDiseased)
        assertTrue(FruitHealthCategory.MONILIASE.isDiseased)
        assertTrue(FruitHealthCategory.VASSOURA_BRUXA.isDiseased)

        val result = PlantPhotoAnalysisResult(
            boxes = listOf(
                DetectedFruitBox("a", 0.1f, 0.1f, 0.2f, 0.2f, FruitHealthCategory.SADIO, 0.9f),
                DetectedFruitBox("b", 0.3f, 0.3f, 0.4f, 0.4f, FruitHealthCategory.MONILIASE, 0.8f)
            ),
            countHealthy = 3,
            countDiseased = 2,
            countPodridaoParda = 1,
            countMoniliase = 1,
            countVassouraBruxa = 0,
            imageWidthPx = 1600,
            imageHeightPx = 1200,
            inferenceTimeMs = 240L
        )
        assertEquals(5, result.countTotal)
    }
}
