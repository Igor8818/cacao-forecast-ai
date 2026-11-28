package com.cacaoforecast.ai.ui.reports

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cacaoforecast.ai.domain.model.CumulativeEvaluationPoint
import com.cacaoforecast.ai.domain.model.CumulativePlotReport
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.pdf.PdfReportGenerator
import com.cacaoforecast.ai.ui.components.KpiStatCard
import com.cacaoforecast.ai.ui.components.SolarCard
import com.cacaoforecast.ai.ui.theme.CacaoForestContainer
import com.cacaoforecast.ai.ui.theme.CacaoForestPrimary
import com.cacaoforecast.ai.ui.theme.CacaoGoldAccent
import com.cacaoforecast.ai.ui.theme.CacaoSelectedPlotBg
import com.cacaoforecast.ai.ui.theme.MoniliaseQuarantineBg
import com.cacaoforecast.ai.ui.theme.MoniliaseQuarantineText
import com.cacaoforecast.ai.ui.theme.SeverityControlledGreen
import com.cacaoforecast.ai.ui.theme.SeverityCriticalRed
import com.cacaoforecast.ai.ui.theme.SeverityWarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CumulativePlaceholderContent(
    farms: List<Farm>,
    selectedFarmId: Long?,
    plots: List<Plot>,
    selectedPlotId: Long?,
    cumulativePeriodLabel: String,
    basis: ProductionBasis,
    cumulativeReport: CumulativePlotReport,
    isGeneratingPdf: Boolean,
    onSelectFarm: (Long) -> Unit,
    onSelectPlot: (Long) -> Unit,
    onSelectPeriod: (Long?, Long?, String) -> Unit,
    onSelectBasis: (ProductionBasis) -> Unit,
    onOpenPlotHistory: (Long, Long) -> Unit,
    onShareCumulativeWhatsApp: () -> Unit,
    onShareCumulativeGenericPdf: () -> Unit
) {
    var farmExpanded by remember { mutableStateOf(false) }
    var plotExpanded by remember { mutableStateOf(false) }
    var showPeriodDialog by remember { mutableStateOf(false) }

    val activeFarm = farms.find { it.id == selectedFarmId } ?: farms.firstOrNull()
    val activePlot = plots.find { it.id == selectedPlotId } ?: plots.firstOrNull()
    val ptBr = remember { Locale("pt", "BR") }
    val dayMonthFmt = remember { SimpleDateFormat("dd/MM/yy", ptBr) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (activeFarm != null) {
            ExposedDropdownMenuBox(
                expanded = farmExpanded,
                onExpandedChange = { farmExpanded = !farmExpanded }
            ) {
                OutlinedTextField(
                    value = activeFarm.nome,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fazenda") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(farmExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = farmExpanded,
                    onDismissRequest = { farmExpanded = false }
                ) {
                    farms.forEach { farm ->
                        DropdownMenuItem(
                            text = { Text(farm.nome) },
                            onClick = {
                                onSelectFarm(farm.id)
                                farmExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (plots.isNotEmpty() && activePlot != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = plotExpanded,
                    onExpandedChange = { plotExpanded = !plotExpanded },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = activePlot.nome,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Roça / Quadra Específica", maxLines = 1, softWrap = false) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(plotExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = plotExpanded,
                        onDismissRequest = { plotExpanded = false }
                    ) {
                        plots.forEach { plot ->
                            DropdownMenuItem(
                                text = { Text(plot.nome) },
                                onClick = {
                                    onSelectPlot(plot.id)
                                    plotExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = cumulativePeriodLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Colheita", maxLines = 1, softWrap = false) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showPeriodDialog = true }
                    )
                }
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = basis == ProductionBasis.NET_HEALTHY_NFS,
                onClick = { onSelectBasis(ProductionBasis.NET_HEALTHY_NFS) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Líquida Sadia (NFS)", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SegmentedButton(
                selected = basis == ProductionBasis.GROSS_TOTAL_NFT,
                onClick = { onSelectBasis(ProductionBasis.GROSS_TOTAL_NFT) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Potencial Bruto (NFT)", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Text(
            text = if (basis == ProductionBasis.GROSS_TOTAL_NFT) {
                "Fórmula ativa (Potencial Total): Produção = ((NFT / NPF) × NPT) / FA"
            } else {
                "Fórmula ativa (Colheita Sadia): Produção = ((NFS / NPF) × NPT) / FA"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (cumulativeReport.points.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sem avaliações concluídas nesta Roça / Quadra",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Conclua amostragens em campo e lance a Produção Real (@) no histórico para visualizar a curva de calibração e evolução do manejo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Resumo de KPIs Cumulativos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiStatCard(
                    title = "Total Estimado",
                    value = String.format(ptBr, "%.1f @", cumulativeReport.totalEstimadaArrobas),
                    subtitle = "${cumulativeReport.points.size} aval.",
                    accentColor = CacaoForestPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    title = "Total Real",
                    value = if (cumulativeReport.evaluationsWithRealCount > 0) {
                        String.format(ptBr, "%.1f @", cumulativeReport.totalRealArrobas)
                    } else {
                        "—"
                    },
                    subtitle = "${cumulativeReport.evaluationsWithRealCount} lançada(s)",
                    accentColor = CacaoGoldAccent,
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    title = "Erro Médio",
                    value = cumulativeReport.meanRelativeErrorPct?.let {
                        String.format(ptBr, "%.1f%%", it)
                    } ?: "—",
                    subtitle = "vs. colhido",
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            if (cumulativeReport.evaluationsWithRealCount == 0) {
                SolarCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = CacaoSelectedPlotBg,
                    borderColor = CacaoForestContainer
                ) {
                    Text(
                        text = "Como informar a Produção Real (@)? Após colher e secar o cacau desta quadra, toque no botão \"Lançar / Editar Produção Real no Histórico\" abaixo para traçar a curva real ao lado da estimada.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Gráfico Temporal Estimada vs. Real
            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Evolução Temporal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Produção (@)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    EstimatedVsRealChart(
                        points = cumulativeReport.points,
                        dayMonthFmt = dayMonthFmt,
                        ptBr = ptBr,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(color = CacaoForestPrimary, label = "ProdEstimada (@)")
                        LegendDot(color = SeverityWarningAmber, label = "ProdReal (@)")
                    }
                }
            }

            // Linha do tempo comparativa (Antes vs. Depois do Manejo)
            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Comparativo de Manejo (Linha do Tempo)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    cumulativeReport.points.asReversed().forEachIndexed { idx, pt ->
                        if (idx > 0) HorizontalDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dayMonthFmt.format(Date(pt.dataMillis)),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(ptBr, "Perdas: %.1f%%", pt.perdasPct),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pt.perdasPct >= 20.0) SeverityCriticalRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = pt.realArrobas?.let { real ->
                                        String.format(ptBr, "Est.: %.1f @ • Real: %.1f @", pt.estimadaArrobas, real)
                                    } ?: String.format(ptBr, "Est.: %.1f @ • Real: pendente", pt.estimadaArrobas),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                pt.deltaPerdasPctVsAnterior?.let { delta ->
                                    val deltaColor = if (delta <= 0.0) SeverityControlledGreen else SeverityCriticalRed
                                    Text(
                                        text = String.format(ptBr, "Δ %+.1f p.p.", delta),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = deltaColor
                                    )
                                }
                            }
                            if (pt.nMoniliase > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MoniliaseQuarantineText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Alerta Quarentenário: ${pt.nMoniliase} fruto(s) com Monilíase",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MoniliaseQuarantineText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeFarm != null && activePlot != null) {
            if (cumulativeReport.points.isNotEmpty()) {
                Button(
                    onClick = onShareCumulativeWhatsApp,
                    enabled = !isGeneratingPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Gráfico PDF no WhatsApp")
                }

                OutlinedButton(
                    onClick = onShareCumulativeGenericPdf,
                    enabled = !isGeneratingPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar PDF Cumulativo")
                }
            }

            OutlinedButton(
                onClick = { onOpenPlotHistory(activeFarm.id, activePlot.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lançar / Editar Produção Real no Histórico")
            }

            Text(
                text = PdfReportGenerator.DISCLAIMER_TEXT,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPeriodDialog) {
        PeriodPickerDialog(
            currentPeriodLabel = cumulativePeriodLabel,
            allowAllHistory = true,
            onSelectAllHistory = {
                onSelectPeriod(null, null, "Todo o Histórico")
                showPeriodDialog = false
            },
            onDismiss = { showPeriodDialog = false },
            onSelectPeriod = { start, end, label ->
                onSelectPeriod(start, end, label)
                showPeriodDialog = false
            }
        )
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    com.cacaoforecast.ai.ui.components.KpiStatCard(
        label = title,
        value = value,
        subtitle = subtitle,
        accentColor = accentColor,
        modifier = modifier
    )
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EstimatedVsRealChart(
    points: List<CumulativeEvaluationPoint>,
    dayMonthFmt: SimpleDateFormat,
    ptBr: Locale,
    modifier: Modifier = Modifier
) {
    val maxY = remember(points) {
        val rawMax = points.maxOfOrNull { maxOf(it.estimadaArrobas, it.realArrobas ?: 0.0) } ?: 10.0
        if (rawMax <= 0.0) 10.0 else rawMax * 1.15
    }

    Canvas(modifier = modifier) {
        val leftPad = 92f
        val rightPad = 28f
        val topPad = 24f
        val bottomPad = 48f

        val chartW = (size.width - leftPad - rightPad).coerceAtLeast(10f)
        val chartH = (size.height - topPad - bottomPad).coerceAtLeast(10f)

        val axisTextPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 26f
            isAntiAlias = true
        }

        // Linhas horizontais de grade + eixo Y
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val frac = i / gridSteps.toFloat()
            val y = topPad + chartH * (1f - frac)
            val valAtLine = maxY * frac
            drawLine(
                color = Color.LightGray.copy(alpha = 0.7f),
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartW, y),
                strokeWidth = 1.5f
            )
            drawContext.canvas.nativeCanvas.drawText(
                String.format(ptBr, "%.0f@", valAtLine),
                8f,
                y + 9f,
                axisTextPaint
            )
        }

        val count = points.size
        fun xForIndex(idx: Int): Float =
            if (count <= 1) leftPad + chartW / 2f
            else leftPad + (idx.toFloat() / (count - 1).toFloat()) * chartW

        fun yForValue(v: Double): Float {
            val ratio = (v / maxY).toFloat().coerceIn(0f, 1f)
            return topPad + chartH * (1f - ratio)
        }

        // Série Estimada
        val estPath = Path()
        points.forEachIndexed { idx, pt ->
            val x = xForIndex(idx)
            val y = yForValue(pt.estimadaArrobas)
            if (idx == 0) estPath.moveTo(x, y) else estPath.lineTo(x, y)
        }
        if (count > 1) {
            drawPath(
                path = estPath,
                color = CacaoForestPrimary,
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        points.forEachIndexed { idx, pt ->
            val x = xForIndex(idx)
            val y = yForValue(pt.estimadaArrobas)
            drawCircle(color = CacaoForestPrimary, radius = 9f, center = Offset(x, y))
        }

        // Série Real (conecta pontos que possuem producaoReal != null)
        val realPath = Path()
        var startedReal = false
        points.forEachIndexed { idx, pt ->
            val real = pt.realArrobas ?: return@forEachIndexed
            val x = xForIndex(idx)
            val y = yForValue(real)
            if (!startedReal) {
                realPath.moveTo(x, y)
                startedReal = true
            } else {
                realPath.lineTo(x, y)
            }
        }
        if (points.count { it.realArrobas != null } > 1) {
            drawPath(
                path = realPath,
                color = SeverityWarningAmber,
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        points.forEachIndexed { idx, pt ->
            val real = pt.realArrobas ?: return@forEachIndexed
            val x = xForIndex(idx)
            val y = yForValue(real)
            drawCircle(color = SeverityWarningAmber, radius = 9f, center = Offset(x, y))
        }

        // Rótulos de data no eixo X (evita sobreposição quando há muitos pontos)
        val labelStep = ((count - 1) / 4).coerceAtLeast(1)
        points.forEachIndexed { idx, pt ->
            if (idx == 0 || idx == count - 1 || idx % labelStep == 0) {
                val x = xForIndex(idx)
                val label = dayMonthFmt.format(Date(pt.dataMillis))
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    (x - 42f).coerceIn(leftPad, (leftPad + chartW - 108f).coerceAtLeast(leftPad)),
                    topPad + chartH + 34f,
                    axisTextPaint
                )
            }
        }
    }
}
