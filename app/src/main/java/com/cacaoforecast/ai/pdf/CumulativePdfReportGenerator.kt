package com.cacaoforecast.ai.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.cacaoforecast.ai.domain.model.CumulativePlotReport
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.ui.theme.BrandIdentity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CumulativePdfReportGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN_X = 32f
        private const val CONTENT_WIDTH = PAGE_WIDTH - 64f
        private const val FOOTER_DISCLAIMER_Y = 816f
    }

    fun generate(
        farm: Farm,
        plot: Plot,
        periodLabel: String,
        report: CumulativePlotReport
    ): Result<File> = runCatching {
        require(report.points.isNotEmpty()) {
            "Não há avaliações concluídas para gerar o PDF cumulativo."
        }
        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        val slug = "${farm.nome}_${plot.nome}"
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "quadra" }
        val fileName = "${BrandIdentity.FILE_PREFIX}_cumulativo_${slug}_${System.currentTimeMillis()}.pdf"
        val outFile = File(reportsDir, fileName)

        val ptBr = Locale("pt", "BR")
        val fullDateFmt = SimpleDateFormat("dd/MM/yyyy", ptBr)
        val generatedFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", ptBr)

        val doc = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 69, 43)
            textSize = 15.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.5f
        }
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 8f
        }
        val gridPaint = Paint().apply {
            color = Color.rgb(215, 222, 216)
            strokeWidth = 0.8f
        }
        val boxBorderPaint = Paint().apply {
            color = Color.rgb(100, 110, 102)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val estLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 69, 43)
            strokeWidth = 2.4f
        }
        val realLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(178, 106, 0)
            strokeWidth = 2.4f
        }
        val headerBgPaint = Paint().apply { color = Color.rgb(20, 69, 43) }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.7f
        }
        val disclaimerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val basisLabel = when (report.basis) {
            ProductionBasis.NET_HEALTHY_NFS -> "Safra Líquida Sadia — NFS"
            ProductionBasis.GROSS_TOTAL_NFT -> "Potencial Bruto — NFT (Fórmula CEPLAC)"
        }

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            var y = 38f
            canvas.drawText(BrandIdentity.PDF_CUMULATIVE_TITLE, MARGIN_X, y, titlePaint)
            y += 18f
            canvas.drawText(
                "Fazenda: ${farm.nome}   |   Quadra: ${plot.nome}   |   Período: $periodLabel",
                MARGIN_X,
                y,
                boldPaint
            )
            y += 14f
            canvas.drawText(
                "Critério: $basisLabel   |   Gerado em: ${generatedFmt.format(Date())}",
                MARGIN_X,
                y,
                subtitlePaint
            )
            y += 16f

            val realSummary = if (report.evaluationsWithRealCount > 0) {
                String.format(ptBr, "%.1f @ (%d aval.)", report.totalRealArrobas, report.evaluationsWithRealCount)
            } else {
                "—"
            }
            val errSummary = report.meanRelativeErrorPct?.let {
                String.format(ptBr, "%.1f%%", it)
            } ?: "—"

            canvas.drawText(
                String.format(
                    ptBr,
                    "Total Estimado: %.1f @ (%d aval.)   •   Total Real: %s   •   Erro Médio: %s",
                    report.totalEstimadaArrobas,
                    report.points.size,
                    realSummary,
                    errSummary
                ),
                MARGIN_X,
                y,
                boldPaint
            )
            y += 16f

            // Caixa do gráfico vetorial (Produção (@) x Data)
            val chartBoxLeft = MARGIN_X
            val chartBoxTop = y
            val chartBoxWidth = CONTENT_WIDTH
            val chartBoxHeight = 250f
            canvas.drawRect(chartBoxLeft, chartBoxTop, chartBoxLeft + chartBoxWidth, chartBoxTop + chartBoxHeight, boxBorderPaint)

            // Cabeçalho interno + legenda do gráfico no padrão do fluxograma
            canvas.drawText("Produção (@)", chartBoxLeft + 10f, chartBoxTop + 16f, boldPaint)
            val legendY = chartBoxTop + 16f
            canvas.drawLine(chartBoxLeft + 160f, legendY - 3f, chartBoxLeft + 182f, legendY - 3f, estLinePaint)
            canvas.drawText("ProdEstimada (@)", chartBoxLeft + 188f, legendY, textPaint)
            canvas.drawLine(chartBoxLeft + 310f, legendY - 3f, chartBoxLeft + 332f, legendY - 3f, realLinePaint)
            canvas.drawText("ProdReal (@)", chartBoxLeft + 338f, legendY, textPaint)

            val plotLeft = chartBoxLeft + 46f
            val plotRight = chartBoxLeft + chartBoxWidth - 20f
            val plotTop = chartBoxTop + 34f
            val plotBottom = chartBoxTop + chartBoxHeight - 52f
            val plotW = (plotRight - plotLeft).coerceAtLeast(10f)
            val plotH = (plotBottom - plotTop).coerceAtLeast(10f)

            val rawMax = report.points.maxOfOrNull { maxOf(it.estimadaArrobas, it.realArrobas ?: 0.0) } ?: 10.0
            val maxY = if (rawMax <= 0.0) 10.0 else rawMax * 1.15

            val steps = 4
            for (i in 0..steps) {
                val frac = i / steps.toFloat()
                val gy = plotBottom - plotH * frac
                canvas.drawLine(plotLeft, gy, plotRight, gy, gridPaint)
                canvas.drawText(String.format(ptBr, "%.0f", maxY * frac), chartBoxLeft + 10f, gy + 3f, axisPaint)
            }

            val pts = report.points
            val count = pts.size
            fun xAt(index: Int): Float =
                if (count <= 1) plotLeft + plotW / 2f
                else plotLeft + (index.toFloat() / (count - 1).toFloat()) * plotW

            fun yAt(value: Double): Float {
                val ratio = (value / maxY).toFloat().coerceIn(0f, 1f)
                return plotBottom - plotH * ratio
            }

            // Série Estimada
            pts.forEachIndexed { idx, pt ->
                val x = xAt(idx)
                val py = yAt(pt.estimadaArrobas)
                if (idx > 0) {
                    canvas.drawLine(xAt(idx - 1), yAt(pts[idx - 1].estimadaArrobas), x, py, estLinePaint)
                }
                canvas.drawCircle(x, py, 3.2f, estLinePaint)
            }

            // Série Real
            var prevRealPos: Pair<Float, Float>? = null
            pts.forEachIndexed { idx, pt ->
                val real = pt.realArrobas ?: return@forEachIndexed
                val x = xAt(idx)
                val py = yAt(real)
                prevRealPos?.let { (px, ppy) ->
                    canvas.drawLine(px, ppy, x, py, realLinePaint)
                }
                canvas.drawCircle(x, py, 3.2f, realLinePaint)
                prevRealPos = x to py
            }

            // Datas no eixo X rotacionadas
            val labelStep = ((count - 1) / 5).coerceAtLeast(1)
            pts.forEachIndexed { idx, pt ->
                if (idx == 0 || idx == count - 1 || idx % labelStep == 0) {
                    val x = xAt(idx)
                    val dateLabel = fullDateFmt.format(Date(pt.dataMillis))
                    canvas.save()
                    canvas.rotate(-32f, x, plotBottom + 32f)
                    canvas.drawText(dateLabel, x - 18f, plotBottom + 32f, axisPaint)
                    canvas.restore()
                }
            }

            y = chartBoxTop + chartBoxHeight + 22f

            // Tabela resumida das avaliações (até 12 mais recentes)
            val tablePts = pts.takeLast(12).asReversed()
            val colX = floatArrayOf(
                MARGIN_X + 8f,
                MARGIN_X + 150f,
                MARGIN_X + 290f,
                MARGIN_X + 430f
            )
            val headerH = 22f
            val rowH = 21f
            canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + headerH, headerBgPaint)
            canvas.drawText("Data", colX[0], y + 15f, headerTextPaint)
            canvas.drawText("ProdEstimada (@)", colX[1], y + 15f, headerTextPaint)
            canvas.drawText("ProdReal (@)", colX[2], y + 15f, headerTextPaint)
            canvas.drawText("Perdas (%)", colX[3], y + 15f, headerTextPaint)
            y += headerH

            tablePts.forEach { pt ->
                if (y + rowH > FOOTER_DISCLAIMER_Y - 22f) return@forEach
                val baseY = y + 14f
                canvas.drawText(fullDateFmt.format(Date(pt.dataMillis)), colX[0], baseY, textPaint)
                canvas.drawText(String.format(ptBr, "%.1f", pt.estimadaArrobas), colX[1], baseY, textPaint)
                canvas.drawText(
                    pt.realArrobas?.let { String.format(ptBr, "%.1f", it) } ?: "—",
                    colX[2],
                    baseY,
                    textPaint
                )
                canvas.drawText(String.format(ptBr, "%.1f%%", pt.perdasPct), colX[3], baseY, textPaint)
                canvas.drawLine(MARGIN_X, y + rowH, MARGIN_X + CONTENT_WIDTH, y + rowH, linePaint)
                y += rowH
            }

            // Disclaimer obrigatório fixo no rodapé
            canvas.drawLine(MARGIN_X, FOOTER_DISCLAIMER_Y - 14f, MARGIN_X + CONTENT_WIDTH, FOOTER_DISCLAIMER_Y - 14f, linePaint)
            canvas.drawText(PdfReportGenerator.DISCLAIMER_TEXT, MARGIN_X, FOOTER_DISCLAIMER_Y, disclaimerPaint)
            canvas.drawText("Página 1/1", MARGIN_X + CONTENT_WIDTH - 56f, FOOTER_DISCLAIMER_Y, subtitlePaint)

            doc.finishPage(page)
            FileOutputStream(outFile).use { fos -> doc.writeTo(fos) }
        } finally {
            doc.close()
        }

        outFile
    }
}