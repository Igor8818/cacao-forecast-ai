package com.cacaoforecast.ai.pdf

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.cacaoforecast.ai.R
import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.DominantDisease
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.FarmConsolidatedFooterUiModel
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.PlotReportRowUiModel
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    shadows = [
        PdfPaginationMathTest.RecordingShadowPdfDocument::class,
        PdfPaginationMathTest.ShadowAppFileProvider::class
    ]
)
class PdfPaginationMathTest {

    @Implements(FileProvider::class)
    class ShadowAppFileProvider {
        companion object {
            @Implementation
            @JvmStatic
            fun getUriForFile(context: Context, authority: String, file: File): Uri =
                Uri.parse("content://$authority/reports/${file.name}")
        }
    }

    @Implements(PdfDocument::class)
    class RecordingShadowPdfDocument {
        private val pagesText = mutableListOf<MutableList<String>>()
        private var currentPageIndex = -1
        private var closed = false

        @Implementation
        fun __constructor__() {
            lastRecordedPages = pagesText
        }

        @Implementation
        fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
            check(!closed) { "document is closed!" }
            val pageTexts = mutableListOf<String>()
            pagesText.add(pageTexts)
            currentPageIndex = pagesText.lastIndex
            val recordingCanvas = object : Canvas() {
                override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
                    pageTexts.add(text)
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
    fun `mais de 25 quadras paginam sem cortar linhas e com rodape unico na ultima pagina`() {
        val slices = PdfPaginationMath.computePageSlices(
            rowCount = 40,
            firstPageCapacity = 18,
            nextPageCapacity = 24,
            footerRowEquivalent = 4
        )

        assertTrue(slices.size >= 2)
        // Verifica contiguidade perfeita de 0..40 sem sobreposicao ou perda de linha
        var expectedStart = 0
        var totalRowsCovered = 0
        slices.forEach { slice ->
            assertEquals(expectedStart, slice.startRowInclusive)
            assertTrue(slice.endRowExclusive >= slice.startRowInclusive)
            totalRowsCovered += (slice.endRowExclusive - slice.startRowInclusive)
            expectedStart = slice.endRowExclusive
        }
        assertEquals(40, totalRowsCovered)
        assertEquals(1, slices.count { it.renderFooter })
        assertTrue(slices.last().renderFooter)
    }

    @Test
    fun `cria pagina extra para rodape quando a ultima pagina de dados fica cheia`() {
        // 18 linhas na pagina 1 (capacidade 18 -> sobra 0 espaco para rodape)
        val slices = PdfPaginationMath.computePageSlices(
            rowCount = 18,
            firstPageCapacity = 18,
            nextPageCapacity = 24,
            footerRowEquivalent = 4
        )

        assertEquals(2, slices.size)
        assertEquals(0, slices[0].startRowInclusive)
        assertEquals(18, slices[0].endRowExclusive)
        assertEquals(false, slices[0].renderFooter)
        assertEquals(18, slices[1].startRowInclusive)
        assertEquals(18, slices[1].endRowExclusive)
        assertEquals(true, slices[1].renderFooter)
    }

    @Test
    fun `suporta 75 quadras e tambem relatorio vazio com disclaimer`() {
        val many = PdfPaginationMath.computePageSlices(75, 16, 24, 4)
        assertEquals(4, many.size)
        assertEquals(75, many.sumOf { it.endRowExclusive - it.startRowInclusive })
        assertEquals(1, many.count { it.renderFooter })

        val empty = PdfPaginationMath.computePageSlices(0, 16, 24, 4)
        assertEquals(1, empty.size)
        assertTrue(empty.single().renderFooter)
    }

    @Test
    fun `PdfReportGenerator gera PDF multi-pagina em cacheDir reports com 6 colunas e disclaimer em todas as paginas`() {
        val expectedDisclaimer = "Disclaimer: Aplicativo em desenvolvimento. Não nos responsabilizamos pelos resultados."
        assertEquals(expectedDisclaimer, PdfReportGenerator.DISCLAIMER_TEXT)

        val context = ApplicationProvider.getApplicationContext<Application>()
        val rows = (1..30).map { idx ->
            PlotReportRowUiModel(
                evaluationId = idx.toLong(),
                plotId = idx.toLong(),
                plotName = "Roça $idx",
                evaluationDateMillis = 1_700_000_000_000L,
                evaluationsCountInPeriod = if (idx == 1) 2 else 1,
                areaHa = 2.0,
                npt = 1000,
                producaoExibidaArrobas = 40.0,
                produtividadeExibidaPorHa = 20.0,
                producaoBrutaArrobas = 50.0,
                producaoLiquidaArrobas = 40.0,
                perdasPct = if (idx == 1) 28.4 else 8.0,
                severity = if (idx == 1) DiseaseSeverity.CRITICAL else DiseaseSeverity.CONTROLLED,
                dominantDisease = if (idx == 1) DominantDisease.VASSOURA_BRUXA else DominantDisease.PODRIDAO_PARDA,
                nMoniliase = if (idx == 2) 2 else 0
            )
        }
        val report = FarmReportUiModel(
            farm = Farm(id = 1L, nome = "Fazenda PDF", municipioUf = "Itabuna/BA", proprietario = "Produtor Teste"),
            periodLabel = "Nov/2026",
            startMillis = 0L,
            endMillis = 1000L,
            selectedPlotId = null,
            basis = ProductionBasis.NET_HEALTHY_NFS,
            sortOrder = ReportSortOrder.LOSS_DESC,
            criticalHighlightRow = rows.first(),
            rows = rows,
            footer = FarmConsolidatedFooterUiModel(
                totalPlotsEvaluated = 30,
                totalActivePlotsInFarm = 30,
                pendingPlotNames = emptyList(),
                totalAreaHa = 60.0,
                totalNpt = 30_000,
                totalProducaoExibidaArrobas = 1200.0,
                produtividadeConsolidadaPorHa = 20.0,
                totalPerdaArrobas = 300.0,
                perdasPonderadasPct = 20.0,
                severity = DiseaseSeverity.CRITICAL,
                totalMoniliaseFruits = 2,
                moniliaseAlertPlotNames = listOf("Roça 2")
            )
        )

        val generator = PdfReportGenerator(context)
        val result = generator.generate(report)
        assertTrue("Falha ao gerar PDF: ${result.exceptionOrNull()?.stackTraceToString()}", result.isSuccess)
        val pdfFile = result.getOrThrow()
        try {
            assertTrue(pdfFile.exists())
            assertEquals("reports", pdfFile.parentFile?.name)
            assertEquals(File(context.cacheDir, "reports").absolutePath, pdfFile.parentFile?.absolutePath)
            assertTrue(pdfFile.readText(Charsets.US_ASCII).startsWith("%PDF-"))

            val pages = RecordingShadowPdfDocument.lastRecordedPages
            assertTrue("Esperava >= 2 paginas para 30 quadras", pages.size >= 2)
            pages.forEachIndexed { pageIdx, texts ->
                assertTrue(
                    "Pagina ${pageIdx + 1} nao contem o disclaimer obrigatorio",
                    texts.contains(expectedDisclaimer)
                )
            }
            val firstPageTexts = pages.first()
            assertTrue(firstPageTexts.any { it.contains("Safra Líquida Sadia — NFS") })
            assertTrue(firstPageTexts.any { it.contains("Foco Fitossanitário Prioritário") && it.contains("Roça 1") })
            assertTrue(firstPageTexts.any { it.contains("ALERTA QUARENTENÁRIO") && it.contains("Monilíase") })
            assertTrue(listOf("Roça", "Área (ha)", "Npla", "Prod. (@)", "Produt. (@/ha)", "Perdas (%)").all { it in firstPageTexts })
            assertTrue(pages.last().contains("TOTAL / CONSOLIDADO"))
        } finally {
            pdfFile.delete()
        }
    }

    @Test
    fun `ReportShareHelper envia para WhatsApp quando instalado e faz fallback para Share Sheet quando ausente`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        // Valida tambem que res/xml/file_paths.xml declara reports/, sessions/ e backups/
        val xmlPaths = mutableListOf<String>()
        context.resources.getXml(R.xml.file_paths).use { parser ->
            var event = parser.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    parser.getAttributeValue(null, "path")?.let { xmlPaths.add(it) }
                }
                event = parser.next()
            }
        }
        assertTrue(xmlPaths.containsAll(listOf("reports/", "sessions/", "backups/")))

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val pdfFile = File(reportsDir, "share_test.pdf").apply { writeText("%PDF-1.4\n") }
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        val shadowActivity = Shadows.shadowOf(activity)
        val shadowPm = Shadows.shadowOf(context.packageManager)

        try {
            // 1. Sem WhatsApp instalado -> fallback para Intent.ACTION_CHOOSER
            while (shadowActivity.nextStartedActivity != null) { /* drain */ }
            ReportShareHelper.sharePdfToWhatsApp(activity, pdfFile, "Legenda Teste")
            val fallbackIntent = shadowActivity.nextStartedActivity
            assertNotNull(fallbackIntent)
            assertEquals(Intent.ACTION_CHOOSER, fallbackIntent.action)
            val wrapped = fallbackIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            assertNotNull(wrapped)
            assertEquals("application/pdf", wrapped!!.type)
            assertTrue((wrapped.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)

            // 2. Com com.whatsapp instalado -> intent direta para com.whatsapp
            val component = android.content.ComponentName("com.whatsapp", "com.whatsapp.ShareActivity")
            shadowPm.addActivityIfNotPresent(component)
            shadowPm.addIntentFilterForActivity(
                component,
                IntentFilter(Intent.ACTION_SEND).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addDataType("application/pdf")
                }
            )

            while (shadowActivity.nextStartedActivity != null) { /* drain */ }
            ReportShareHelper.sharePdfToWhatsApp(activity, pdfFile, "Legenda WhatsApp")
            val whatsappIntent = shadowActivity.nextStartedActivity
            assertNotNull(whatsappIntent)
            assertEquals(Intent.ACTION_SEND, whatsappIntent.action)
            assertEquals("com.whatsapp", whatsappIntent.`package`)
            assertEquals("application/pdf", whatsappIntent.type)
            assertEquals("Legenda WhatsApp", whatsappIntent.getStringExtra(Intent.EXTRA_TEXT))
            assertTrue((whatsappIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        } finally {
            pdfFile.delete()
        }
    }
}
