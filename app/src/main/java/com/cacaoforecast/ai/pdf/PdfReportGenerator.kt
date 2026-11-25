package com.cacaoforecast.ai.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.DominantDisease
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.ProductionBasis
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfPageSlice(
    val pageIndex: Int,
    val startRowInclusive: Int,
    val endRowExclusive: Int,
    val renderFooter: Boolean
)

object PdfPaginationMath {

    /**
     * Calcula fatias de linhas por página sem cortar linhas ao meio e garantindo
     * espaço reservado para rodapé consolidado e Disclaimer obrigatório em todas as páginas.
     */
    fun computePageSlices(
        rowCount: Int,
        firstPageCapacity: Int,
        nextPageCapacity: Int,
        footerRowEquivalent: Int = 4
    ): List<PdfPageSlice> {
        require(rowCount >= 0) { "rowCount deve ser >= 0" }
        require(firstPageCapacity >= 1) { "firstPageCapacity deve ser >= 1" }
        require(nextPageCapacity >= 1) { "nextPageCapacity deve ser >= 1" }
        require(footerRowEquivalent >= 1) { "footerRowEquivalent deve ser >= 1" }

        if (rowCount == 0) {
            return listOf(
                PdfPageSlice(
                    pageIndex = 0,
                    startRowInclusive = 0,
                    endRowExclusive = 0,
                    renderFooter = true
                )
            )
        }

        val slices = mutableListOf<PdfPageSlice>()
        var cursor = 0
        var pageIndex = 0

        while (cursor < rowCount) {
            val capacity = if (pageIndex == 0) firstPageCapacity else nextPageCapacity
            val remaining = rowCount - cursor
            val take = minOf(capacity, remaining)
            val end = cursor + take
            val isLastDataPage = end == rowCount
            val fitsFooterHere = isLastDataPage && (capacity - take >= footerRowEquivalent)

            slices.add(
                PdfPageSlice(
                    pageIndex = pageIndex,
                    startRowInclusive = cursor,
                    endRowExclusive = end,
                    renderFooter = fitsFooterHere
                )
            )
            cursor = end
            pageIndex++
        }

        if (slices.none { it.renderFooter }) {
            slices.add(
                PdfPageSlice(
                    pageIndex = pageIndex,
                    startRowInclusive = rowCount,
                    endRowExclusive = rowCount,
                    renderFooter = true
                )
            )
        }
        return slices
    }
}

class PdfReportGenerator(private val context: Context) {

    companion object {
        const val DISCLAIMER_TEXT =
            "Disclaimer: Aplicativo em desenvolvimento. Não nos responsabilizamos pelos resultados."

        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN_X = 32f
        private const val CONTENT_WIDTH = PAGE_WIDTH - 64f
        private const val FOOTER_DISCLAIMER_Y = 816f
        private const val ROW_HEIGHT = 24f
        private const val HEADER_HEIGHT = 24f
    }

    fun generate(report: FarmReportUiModel): Result<File> = runCatching {
        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        val slug = report.farm.nome
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "fazenda" }
        val fileName = "relatorio_${slug}_${System.currentTimeMillis()}.pdf"
        val outFile = File(reportsDir, fileName)

        val ptBr = Locale("pt", "BR")
        val hasAlert = report.criticalHighlightRow != null
        val hasMoniliaseAlert = report.footer.totalMoniliaseFruits > 0
        val firstPageTableTop = 156f + (if (hasAlert) 54f else 0f) + (if (hasMoniliaseAlert) 46f else 0f)
        val nextPageTableTop = 78f
        val maxTableBottom = FOOTER_DISCLAIMER_Y - 26f

        val firstCapacity = ((maxTableBottom - firstPageTableTop - HEADER_HEIGHT) / ROW_HEIGHT).toInt().coerceAtLeast(5)
        val nextCapacity = ((maxTableBottom - nextPageTableTop - HEADER_HEIGHT) / ROW_HEIGHT).toInt().coerceAtLeast(10)

        val slices = PdfPaginationMath.computePageSlices(
            rowCount = report.rows.size,
            firstPageCapacity = firstCapacity,
            nextPageCapacity = nextCapacity,
            footerRowEquivalent = 4
        )
        val totalPages = slices.size

        val doc = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(27, 77, 46)
            textSize = 16f
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
        val headerBgPaint = Paint().apply { color = Color.rgb(27, 77, 46) }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val criticalBgPaint = Paint().apply { color = Color.rgb(253, 236, 234) }
        val warningBgPaint = Paint().apply { color = Color.rgb(255, 244, 224) }
        val totalBgPaint = Paint().apply { color = Color.rgb(231, 236, 230) }
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
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", ptBr)
        val generatedFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", ptBr)

        try {
            slices.forEach { slice ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, slice.pageIndex + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                var y = 36f
                if (slice.pageIndex == 0) {
                    canvas.drawText("CacaoForecastAI • Relatório Consolidado de Safra", MARGIN_X, y, titlePaint)
                    y += 18f
                    val locationPart = report.farm.municipioUf?.let { " • $it" } ?: ""
                    val ownerPart = report.farm.proprietario?.let { " • Resp.: $it" } ?: ""
                    canvas.drawText("Fazenda: ${report.farm.nome}$locationPart$ownerPart", MARGIN_X, y, boldPaint)
                    y += 14f
                    canvas.drawText("Período: ${report.periodLabel}   |   Gerado em: ${generatedFmt.format(Date())}", MARGIN_X, y, subtitlePaint)
                    y += 14f
                    canvas.drawText("Critério exibido na coluna Produção (@): $basisLabel", MARGIN_X, y, boldPaint)
                    y += 14f

                    report.criticalHighlightRow?.let { crit ->
                        canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 40f, criticalBgPaint)
                        val alertTitlePaint = Paint(boldPaint).apply { color = Color.rgb(198, 40, 40) }
                        canvas.drawText(
                            "Foco Fitossanitário Prioritário: ${crit.plotName} — ${String.format(ptBr, "%.1f%%", crit.perdasPct)} de perda",
                            MARGIN_X + 8f,
                            y + 15f,
                            alertTitlePaint
                        )
                        canvas.drawText(
                            "Predomínio: ${dominantDiseaseLabel(crit.dominantDisease)} • Produção exibida: ${String.format(ptBr, "%.1f @", crit.producaoExibidaArrobas)}",
                            MARGIN_X + 8f,
                            y + 30f,
                            textPaint
                        )
                        y += 48f
                    }

                    if (hasMoniliaseAlert) {
                        val moniliaseBgPaint = Paint().apply { color = Color.rgb(245, 229, 252) }
                        val moniliaseTitlePaint = Paint(boldPaint).apply { color = Color.rgb(142, 0, 173) }
                        canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 38f, moniliaseBgPaint)
                        canvas.drawText(
                            "ALERTA QUARENTENÁRIO: Suspeita de Monilíase (${report.footer.totalMoniliaseFruits} fruto(s))",
                            MARGIN_X + 8f,
                            y + 14f,
                            moniliaseTitlePaint
                        )
                        val affected = report.footer.moniliaseAlertPlotNames.joinToString(", ").take(56)
                        canvas.drawText(
                            "Roças afetadas: $affected — Isolar área e acionar assistência técnica CEPLAC.",
                            MARGIN_X + 8f,
                            y + 29f,
                            textPaint
                        )
                        y += 44f
                    }
                } else {
                    canvas.drawText("Fazenda: ${report.farm.nome} • ${report.periodLabel} (continuação)", MARGIN_X, y, boldPaint)
                    y += 14f
                    canvas.drawText("Critério: $basisLabel", MARGIN_X, y, subtitlePaint)
                    y += 12f
                }

                // Cabeçalho das 6 colunas oficiais
                val colX = floatArrayOf(
                    MARGIN_X + 6f,   // Roça
                    MARGIN_X + 170f, // Área
                    MARGIN_X + 235f, // Npla
                    MARGIN_X + 305f, // Produção (@)
                    MARGIN_X + 395f, // Produtividade (@/ha)
                    MARGIN_X + 485f  // Perdas (%)
                )

                val hasRowsOnPage = slice.endRowExclusive > slice.startRowInclusive
                if (hasRowsOnPage || slice.pageIndex == 0) {
                    canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + HEADER_HEIGHT, headerBgPaint)
                    val hy = y + 16f
                    canvas.drawText("Roça", colX[0], hy, headerTextPaint)
                    canvas.drawText("Área (ha)", colX[1], hy, headerTextPaint)
                    canvas.drawText("Npla", colX[2], hy, headerTextPaint)
                    canvas.drawText("Prod. (@)", colX[3], hy, headerTextPaint)
                    canvas.drawText("Produt. (@/ha)", colX[4], hy, headerTextPaint)
                    canvas.drawText("Perdas (%)", colX[5], hy, headerTextPaint)
                    y += HEADER_HEIGHT
                }

                for (rowIdx in slice.startRowInclusive until slice.endRowExclusive) {
                    val row = report.rows[rowIdx]
                    when (row.severity) {
                        DiseaseSeverity.CRITICAL -> canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + ROW_HEIGHT, criticalBgPaint)
                        DiseaseSeverity.WARNING -> canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + ROW_HEIGHT, warningBgPaint)
                        DiseaseSeverity.CONTROLLED -> Unit
                    }

                    val rowPaint = if (row.severity == DiseaseSeverity.CRITICAL) boldPaint else textPaint
                    val baseY = y + 15f
                    val nameLabel = if (row.evaluationsCountInPeriod > 1) {
                        "${row.plotName.take(22)} (${dateFmt.format(Date(row.evaluationDateMillis)).take(5)})"
                    } else {
                        row.plotName.take(28)
                    }
                    canvas.drawText(nameLabel, colX[0], baseY, rowPaint)
                    canvas.drawText(String.format(ptBr, "%.2f", row.areaHa), colX[1], baseY, rowPaint)
                    canvas.drawText(row.npt.toString(), colX[2], baseY, rowPaint)
                    canvas.drawText(String.format(ptBr, "%.1f", row.producaoExibidaArrobas), colX[3], baseY, rowPaint)
                    canvas.drawText(String.format(ptBr, "%.1f", row.produtividadeExibidaPorHa), colX[4], baseY, rowPaint)

                    val lossPaint = Paint(rowPaint).apply {
                        color = when (row.severity) {
                            DiseaseSeverity.CRITICAL -> Color.rgb(198, 40, 40)
                            DiseaseSeverity.WARNING -> Color.rgb(178, 106, 0)
                            DiseaseSeverity.CONTROLLED -> Color.rgb(30, 126, 52)
                        }
                    }
                    canvas.drawText(String.format(ptBr, "%.1f%%", row.perdasPct), colX[5], baseY, lossPaint)
                    canvas.drawLine(MARGIN_X, y + ROW_HEIGHT, MARGIN_X + CONTENT_WIDTH, y + ROW_HEIGHT, linePaint)
                    y += ROW_HEIGHT
                }

                if (slice.renderFooter) {
                    y += 6f
                    canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + ROW_HEIGHT + 4f, totalBgPaint)
                    val ty = y + 17f
                    val f = report.footer
                    canvas.drawText("TOTAL / CONSOLIDADO", colX[0], ty, boldPaint)
                    canvas.drawText(String.format(ptBr, "%.2f", f.totalAreaHa), colX[1], ty, boldPaint)
                    canvas.drawText(f.totalNpt.toString(), colX[2], ty, boldPaint)
                    canvas.drawText(String.format(ptBr, "%.1f", f.totalProducaoExibidaArrobas), colX[3], ty, boldPaint)
                    canvas.drawText(String.format(ptBr, "%.1f", f.produtividadeConsolidadaPorHa), colX[4], ty, boldPaint)
                    canvas.drawText(String.format(ptBr, "%.1f%%", f.perdasPonderadasPct), colX[5], ty, boldPaint)
                    y += ROW_HEIGHT + 18f

                    val pendingTxt = if (f.pendingPlotNames.isEmpty()) {
                        "Cobertura: ${f.totalPlotsEvaluated} de ${f.totalActivePlotsInFarm} roças avaliadas no período."
                    } else {
                        "Cobertura: ${f.totalPlotsEvaluated} de ${f.totalActivePlotsInFarm} roças avaliadas (Pendentes: ${f.pendingPlotNames.joinToString(", ")})"
                    }
                    canvas.drawText(pendingTxt.take(96), MARGIN_X, y, subtitlePaint)
                }

                // Disclaimer obrigatório e número de página fixos no rodapé de TODAS as páginas
                canvas.drawLine(MARGIN_X, FOOTER_DISCLAIMER_Y - 14f, MARGIN_X + CONTENT_WIDTH, FOOTER_DISCLAIMER_Y - 14f, linePaint)
                canvas.drawText(DISCLAIMER_TEXT, MARGIN_X, FOOTER_DISCLAIMER_Y, disclaimerPaint)
                canvas.drawText(
                    "Página ${slice.pageIndex + 1}/$totalPages",
                    MARGIN_X + CONTENT_WIDTH - 56f,
                    FOOTER_DISCLAIMER_Y,
                    subtitlePaint
                )

                doc.finishPage(page)
            }

            FileOutputStream(outFile).use { fos ->
                doc.writeTo(fos)
            }
        } finally {
            doc.close()
        }

        outFile
    }

    private fun dominantDiseaseLabel(disease: DominantDisease): String = when (disease) {
        DominantDisease.NONE -> "Sem ocorrência"
        DominantDisease.PODRIDAO_PARDA -> "Podridão Parda"
        DominantDisease.MONILIASE -> "Monilíase"
        DominantDisease.VASSOURA_BRUXA -> "Vassoura de Bruxa"
    }
}
