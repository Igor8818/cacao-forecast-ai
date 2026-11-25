package com.cacaoforecast.ai.pdf

import android.content.Context
import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.DominantDisease
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.ui.theme.BrandIdentity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvReportExporter(private val context: Context) {

    fun buildCsvContent(report: FarmReportUiModel): String {
        val ptBr = Locale("pt", "BR")
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", ptBr)
        val basisLabel = when (report.basis) {
            ProductionBasis.NET_HEALTHY_NFS -> "Safra Liquida Sadia (NFS)"
            ProductionBasis.GROSS_TOTAL_NFT -> "Potencial Bruto (NFT)"
        }

        val sb = StringBuilder()
        // UTF-8 BOM para abertura correta com acentos no Excel pt-BR
        sb.append('\uFEFF')
        sb.append(
            listOf(
                "Fazenda",
                "Periodo",
                "Criterio",
                "Roca_Quadra",
                "Data_Avaliacao",
                "Avaliacoes_No_Periodo",
                "Area_ha",
                "NPT",
                "Producao_Exibida_Arrobas",
                "Produtividade_Arrobas_ha",
                "Producao_Bruta_NFT_Arrobas",
                "Producao_Liquida_NFS_Arrobas",
                "Perda_Arrobas",
                "Perdas_Pct",
                "Severidade",
                "Doenca_Predominante",
                "Frutos_Moniliase"
            ).joinToString(";")
        ).append("\r\n")

        report.rows.forEach { row ->
            val perdaArrobas = (row.producaoBrutaArrobas - row.producaoLiquidaArrobas).coerceAtLeast(0.0)
            sb.append(
                listOf(
                    escapeCsv(report.farm.nome),
                    escapeCsv(report.periodLabel),
                    escapeCsv(basisLabel),
                    escapeCsv(row.plotName),
                    dateFmt.format(Date(row.evaluationDateMillis)),
                    row.evaluationsCountInPeriod.toString(),
                    formatDecimal(row.areaHa),
                    row.npt.toString(),
                    formatDecimal(row.producaoExibidaArrobas),
                    formatDecimal(row.produtividadeExibidaPorHa),
                    formatDecimal(row.producaoBrutaArrobas),
                    formatDecimal(row.producaoLiquidaArrobas),
                    formatDecimal(perdaArrobas),
                    formatDecimal(row.perdasPct),
                    severityLabel(row.severity),
                    dominantDiseaseLabel(row.dominantDisease),
                    row.nMoniliase.toString()
                ).joinToString(";")
            ).append("\r\n")
        }

        val f = report.footer
        val totalBruta = report.rows.sumOf { it.producaoBrutaArrobas }
        val totalLiquida = report.rows.sumOf { it.producaoLiquidaArrobas }
        sb.append(
            listOf(
                escapeCsv(report.farm.nome),
                escapeCsv(report.periodLabel),
                escapeCsv(basisLabel),
                "TOTAL_CONSOLIDADO",
                "",
                "${f.totalPlotsEvaluated}/${f.totalActivePlotsInFarm}",
                formatDecimal(f.totalAreaHa),
                f.totalNpt.toString(),
                formatDecimal(f.totalProducaoExibidaArrobas),
                formatDecimal(f.produtividadeConsolidadaPorHa),
                formatDecimal(totalBruta),
                formatDecimal(totalLiquida),
                formatDecimal(f.totalPerdaArrobas),
                formatDecimal(f.perdasPonderadasPct),
                severityLabel(f.severity),
                "",
                f.totalMoniliaseFruits.toString()
            ).joinToString(";")
        ).append("\r\n")

        return sb.toString()
    }

    fun export(report: FarmReportUiModel): Result<File> = runCatching {
        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        val slug = report.farm.nome
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "fazenda" }
        val file = File(reportsDir, "${BrandIdentity.FILE_PREFIX}_${slug}_${System.currentTimeMillis()}.csv")
        file.writeText(buildCsvContent(report), Charsets.UTF_8)
        file
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(';') || value.contains('"') || value.contains('\n') || value.contains('\r')
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private fun formatDecimal(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private fun severityLabel(severity: DiseaseSeverity): String = when (severity) {
        DiseaseSeverity.CONTROLLED -> "Controlado"
        DiseaseSeverity.WARNING -> "Atencao"
        DiseaseSeverity.CRITICAL -> "Critico"
    }

    private fun dominantDiseaseLabel(disease: DominantDisease): String = when (disease) {
        DominantDisease.NONE -> "Sem ocorrencia"
        DominantDisease.PODRIDAO_PARDA -> "Podridao Parda"
        DominantDisease.MONILIASE -> "Moniliase"
        DominantDisease.VASSOURA_BRUXA -> "Vassoura de Bruxa"
    }
}
