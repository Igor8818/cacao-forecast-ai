package com.cacaoforecast.ai.pdf

import android.app.Application
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.test.core.app.ApplicationProvider
import com.cacaoforecast.ai.domain.model.CumulativeEvaluationPoint
import com.cacaoforecast.ai.domain.model.CumulativePlotReport
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    shadows = [CumulativePdfReportGeneratorTest.RecordingShadowPdfDocument::class]
)
class CumulativePdfReportGeneratorTest {

    @Implements(PdfDocument::class)
    class RecordingShadowPdfDocument {
        private val pagesText = mutableListOf<MutableList<String>>()
        private var closed = false

        @Implementation
        fun __constructor__() {
            lastRecordedPages = pagesText
        }

        @Implementation
        fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
            check(!closed) { "document is closed!" }
            val texts = mutableListOf<String>()
            pagesText.add(texts)
            val recordingCanvas = object : Canvas() {
                override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
                    texts.add(text)
                }
            }
            val ctor = PdfDocument.Page::class.java.getDeclaredConstructor(
                Canvas::class.java,
                PdfDocument.PageInfo::class.java
            )
            ctor.isAccessible = true
            return ctor.newInstance(recordingCanvas, pageInfo)
        }

        @Implementation
        fun finishPage(page: PdfDocument.Page) {
            check(!closed) { "document is closed!" }
        }

        @Implementation
        fun writeTo(out: OutputStream) {
            check(!closed) { "document is closed!" }
            out.write("%PDF-1.4\n%%EOF\n".toByteArray(Charsets.US_ASCII))
        }

        @Implementation
        fun close() {
            closed = true
        }

        companion object {
            var lastRecordedPages: List<List<String>> = emptyList()
        }
    }

    @Test
    fun `CumulativePdfReportGenerator gera PDF com titulo Producao, legenda e Disclaimer obrigatorio`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val farm = Farm(id = 1L, nome = "Sempre Viva", municipioUf = "Ilhéus/BA")
        val plot = Plot(id = 10L, fazendaId = 1L, nome = "Topo do Morro", areaHa = 3.0, npt = 2500, fa = 300.0)
        val report = CumulativePlotReport(
            plotId = 10L,
            basis = ProductionBasis.GROSS_TOTAL_NFT,
            points = listOf(
                CumulativeEvaluationPoint(1L, 1_767_261_600_000L, 105.0, 95.0, 4.8, null, 0),
                CumulativeEvaluationPoint(2L, 1_769_940_000_000L, 190.0, 180.0, 5.3, 0.5, 0),
                CumulativeEvaluationPoint(3L, 1_772_359_200_000L, 300.0, 290.0, 5.0, -0.3, 0)
            ),
            totalEstimadaArrobas = 595.0,
            totalRealArrobas = 565.0,
            evaluationsWithRealCount = 3,
            meanRelativeErrorPct = 5.2,
            latestLossDeltaPct = -0.3
        )

        val generator = CumulativePdfReportGenerator(context)
        val result = generator.generate(
            farm = farm,
            plot = plot,
            periodLabel = "Todo o Histórico",
            report = report
        )
        assertTrue("Falha ao gerar PDF cumulativo: ${result.exceptionOrNull()?.stackTraceToString()}", result.isSuccess)
        val file = result.getOrThrow()
        try {
            assertTrue(file.exists())
            assertEquals("reports", file.parentFile?.name)
            assertEquals(File(context.cacheDir, "reports").absolutePath, file.parentFile?.absolutePath)
            assertTrue(file.readText(Charsets.US_ASCII).startsWith("%PDF-"))

            val pageTexts = RecordingShadowPdfDocument.lastRecordedPages.single()
            assertTrue(pageTexts.any { it.contains("Produção") })
            assertTrue(pageTexts.any { it.contains("Sempre Viva") && it.contains("Topo do Morro") })
            assertTrue(pageTexts.contains("ProdEstimada (@)"))
            assertTrue(pageTexts.contains("ProdReal (@)"))
            assertTrue(pageTexts.contains(PdfReportGenerator.DISCLAIMER_TEXT))
        } finally {
            file.delete()
        }
    }
}