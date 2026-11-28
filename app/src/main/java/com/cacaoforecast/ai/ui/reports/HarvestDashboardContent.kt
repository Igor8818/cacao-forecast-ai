package com.cacaoforecast.ai.ui.reports

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.PlotReportRowUiModel
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.pdf.PdfReportGenerator
import com.cacaoforecast.ai.ui.components.KpiStatCard
import com.cacaoforecast.ai.ui.components.SeverityPill
import com.cacaoforecast.ai.ui.components.SolarCard
import com.cacaoforecast.ai.ui.theme.BoxHealthyColor
import com.cacaoforecast.ai.ui.theme.BoxMoniliaseColor
import com.cacaoforecast.ai.ui.theme.BoxPodridaoColor
import com.cacaoforecast.ai.ui.theme.BoxVassouraColor
import com.cacaoforecast.ai.ui.theme.CacaoForestContainer
import com.cacaoforecast.ai.ui.theme.CacaoForestPrimary
import com.cacaoforecast.ai.ui.theme.CacaoGoldAccent
import com.cacaoforecast.ai.ui.theme.CacaoPodSecondary
import com.cacaoforecast.ai.ui.theme.CacaoSurfaceVariant
import com.cacaoforecast.ai.ui.theme.MoniliaseQuarantineBg
import com.cacaoforecast.ai.ui.theme.MoniliaseQuarantineBorder
import com.cacaoforecast.ai.ui.theme.MoniliaseQuarantineText
import com.cacaoforecast.ai.ui.theme.PodridaoMiniBg
import com.cacaoforecast.ai.ui.theme.PodridaoMiniText
import com.cacaoforecast.ai.ui.theme.SeverityControlledBg
import com.cacaoforecast.ai.ui.theme.SeverityControlledGreen
import com.cacaoforecast.ai.ui.theme.SeverityCriticalBg
import com.cacaoforecast.ai.ui.theme.SeverityCriticalRed
import com.cacaoforecast.ai.ui.theme.SeverityWarningBg
import com.cacaoforecast.ai.ui.theme.VassouraMiniBg
import com.cacaoforecast.ai.ui.theme.VassouraMiniText
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestDashboardContent(
    farms: List<Farm>,
    filters: ReportFilterState,
    report: FarmReportUiModel?,
    onSelectFarm: (Long) -> Unit,
    onSelectPeriod: (Long, Long, String) -> Unit,
    onSelectBasis: (ProductionBasis) -> Unit,
    onOpenPlotHistory: (Long, Long) -> Unit,
    onNavigateToFarms: () -> Unit
) {
    if (farms.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Nenhuma fazenda cadastrada", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Cadastre uma fazenda e realize amostragens para acompanhar o painel gerencial da safra.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onNavigateToFarms,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Cadastrar Fazenda")
                    }
                }
            }
        }
        return
    }

    var farmExpanded by remember { mutableStateOf(false) }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var pricePerArrobaText by rememberSaveable { mutableStateOf("350") }

    val selectedFarm = report?.farm ?: farms.first()
    val ptBr = remember { Locale("pt", "BR") }
    val currencyFmt = remember(ptBr) {
        NumberFormat.getCurrencyInstance(ptBr).apply { maximumFractionDigits = 0 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = farmExpanded,
                onExpandedChange = { farmExpanded = !farmExpanded },
                modifier = Modifier.weight(1.25f)
            ) {
                OutlinedTextField(
                    value = selectedFarm.nome,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fazenda", maxLines = 1, softWrap = false) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(farmExpanded) },
                    singleLine = true,
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

            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = filters.periodLabel,
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

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = filters.basis == ProductionBasis.NET_HEALTHY_NFS,
                onClick = { onSelectBasis(ProductionBasis.NET_HEALTHY_NFS) },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) {
                Text("Líquida Sadia (NFS)", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SegmentedButton(
                selected = filters.basis == ProductionBasis.GROSS_TOTAL_NFT,
                onClick = { onSelectBasis(ProductionBasis.GROSS_TOTAL_NFT) },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) {
                Text("Potencial Bruto (NFT)", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (report == null || report.rows.isEmpty()) {
            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Sem amostragens concluídas neste mês",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Conclua pelo menos 1 avaliação em ${selectedFarm.nome} (ou altere o período) para visualizar os gráficos de produção, sanidade e projeção comercial.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val f = report.footer
            val netArrobas = f.totalLiquidaArrobas
            val grossArrobas = f.totalBrutaArrobas.coerceAtLeast(netArrobas)
            val lossArrobas = f.totalPerdaArrobas
            val healthySharePct = if (grossArrobas > 0.0) {
                ((netArrobas / grossArrobas) * 100.0).coerceIn(0.0, 100.0)
            } else {
                100.0
            }

            // Card 1: Panorama da Safra & Aproveitamento
            HarvestOverviewCard(
                basis = report.basis,
                displayedArrobas = f.totalProducaoExibidaArrobas,
                netArrobas = netArrobas,
                lossArrobas = lossArrobas,
                lossPct = f.perdasPonderadasPct,
                productivityPorHa = f.produtividadeConsolidadaPorHa,
                evaluatedPlots = f.totalPlotsEvaluated,
                activePlots = f.totalActivePlotsInFarm,
                healthySharePct = healthySharePct,
                ptBr = ptBr
            )

            // Card 2: Produção & Produtividade por Quadra (Barras Horizontais)
            PlotProductionBarsCard(
                farmId = report.farm.id,
                rows = report.rows,
                onOpenPlotHistory = onOpenPlotHistory,
                ptBr = ptBr
            )

            // Card 3: Radiografia Fitossanitária (Donut) + Ações de Manejo Específico
            PhytosanitaryDonutAndManagementCard(
                healthyFruits = f.totalHealthyFruits,
                podridaoFruits = f.totalPodridaoPardaFruits,
                vassouraFruits = f.totalVassouraBruxaFruits,
                moniliaseFruits = f.totalMoniliaseFruits,
                moniliasePlotNames = f.moniliaseAlertPlotNames,
                ptBr = ptBr
            )

            // Card 4: Simulador de Comercialização & Mão de Obra
            CommercializationAndLaborCard(
                pricePerArrobaText = pricePerArrobaText,
                onPriceChange = { raw ->
                    pricePerArrobaText = raw.filter { it.isDigit() || it == ',' || it == '.' }
                },
                netArrobas = netArrobas,
                grossArrobas = grossArrobas,
                lossArrobas = lossArrobas,
                currencyFmt = currencyFmt,
                ptBr = ptBr
            )

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
            currentPeriodLabel = filters.periodLabel,
            onDismiss = { showPeriodDialog = false },
            onSelectPeriod = { start, end, label ->
                onSelectPeriod(start, end, label)
                showPeriodDialog = false
            }
        )
    }
}

@Composable
private fun HarvestOverviewCard(
    basis: ProductionBasis,
    displayedArrobas: Double,
    netArrobas: Double,
    lossArrobas: Double,
    lossPct: Double,
    productivityPorHa: Double,
    evaluatedPlots: Int,
    activePlots: Int,
    healthySharePct: Double,
    ptBr: Locale
) {
    val activeKg = displayedArrobas * 15.0
    val bags60Kg = activeKg / 60.0
    val kpiTitle = if (basis == ProductionBasis.NET_HEALTHY_NFS) "Safra Líquida" else "Potencial Bruto"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiStatCard(
                label = kpiTitle,
                value = String.format(ptBr, "%.1f @", displayedArrobas),
                subtitle = String.format(ptBr, "%.0f kg • %.1f sc", activeKg, bags60Kg),
                accentColor = CacaoForestPrimary,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                label = "Perda Sanitária",
                value = String.format(ptBr, "%.1f @", lossArrobas),
                subtitle = String.format(ptBr, "%.1f%% do potencial", lossPct),
                accentColor = if (lossPct >= 20.0) SeverityCriticalRed else CacaoPodSecondary,
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                label = "Produtividade",
                value = String.format(ptBr, "%.1f @/ha", productivityPorHa),
                subtitle = "$evaluatedPlots de $activePlots roças",
                accentColor = CacaoGoldAccent,
                modifier = Modifier.weight(1f)
            )
        }

        SolarCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Aproveitamento da Lavoura",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        String.format(ptBr, "%.1f%% colheita aproveitável", healthySharePct),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SeverityControlledGreen
                    )
                }

                val healthyFraction = (healthySharePct / 100.0).toFloat().coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(SeverityCriticalRed.copy(alpha = 0.85f))
                ) {
                    if (healthyFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(healthyFraction)
                                .height(14.dp)
                                .background(SeverityControlledGreen)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        String.format(ptBr, "Sadia aproveitável: %.1f @", netArrobas),
                        style = MaterialTheme.typography.labelSmall,
                        color = SeverityControlledGreen
                    )
                    Text(
                        String.format(ptBr, "Perda por doenças: %.1f @", lossArrobas),
                        style = MaterialTheme.typography.labelSmall,
                        color = SeverityCriticalRed
                    )
                }
            }
        }
    }
}

@Composable
private fun PlotProductionBarsCard(
    farmId: Long,
    rows: List<PlotReportRowUiModel>,
    onOpenPlotHistory: (Long, Long) -> Unit,
    ptBr: Locale
) {
    val ordered = remember(rows) {
        rows.sortedByDescending { it.producaoExibidaArrobas }
    }
    val maxArrobas = remember(ordered) {
        ordered.maxOfOrNull { it.producaoExibidaArrobas }?.coerceAtLeast(1.0) ?: 1.0
    }

    SolarCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    "Produção por Quadra / Roça",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Toque em uma roça para abrir o histórico e informar colheita real",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ordered.forEachIndexed { idx, row ->
                if (idx > 0) HorizontalDivider(color = CacaoSurfaceVariant)
                val barFraction = (row.producaoExibidaArrobas / maxArrobas).toFloat().coerceIn(0.03f, 1f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenPlotHistory(farmId, row.plotId) }
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.plotName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SeverityPill(severity = row.severity, lossPct = row.perdasPct)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(CacaoSurfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(CacaoForestPrimary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(ptBr, "%.1f @ • %.1f @/ha", row.producaoExibidaArrobas, row.produtividadeExibidaPorHa),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CacaoForestPrimary
                        )
                        Text(
                            text = String.format(ptBr, "%.2f ha • %d pés", row.areaHa, row.npt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhytosanitaryDonutAndManagementCard(
    healthyFruits: Int,
    podridaoFruits: Int,
    vassouraFruits: Int,
    moniliaseFruits: Int,
    moniliasePlotNames: List<String>,
    ptBr: Locale
) {
    val totalFruits = healthyFruits + podridaoFruits + vassouraFruits + moniliaseFruits
    val healthyPct = if (totalFruits > 0) (healthyFruits * 100.0 / totalFruits) else 100.0

    val slices = remember(healthyFruits, podridaoFruits, vassouraFruits, moniliaseFruits) {
        listOf(
            Triple("Sadios", healthyFruits, BoxHealthyColor),
            Triple("Podridão Parda", podridaoFruits, BoxPodridaoColor),
            Triple("Vassoura de Bruxa", vassouraFruits, BoxVassouraColor),
            Triple("Monilíase", moniliaseFruits, BoxMoniliaseColor)
        )
    }

    SolarCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Radiografia Fitossanitária & Manejo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(126.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(112.dp)) {
                        val strokeW = 24.dp.toPx()
                        val inset = strokeW / 2f
                        val arcSize = Size(size.width - strokeW, size.height - strokeW)
                        val topLeft = Offset(inset, inset)
                        if (totalFruits <= 0) {
                            drawArc(
                                color = CacaoSurfaceVariant,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW)
                            )
                        } else {
                            var startAngle = -90f
                            slices.forEach { (_, count, color) ->
                                if (count > 0) {
                                    val sweep = (count.toFloat() / totalFruits.toFloat()) * 360f
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(ptBr, "%.0f%%", healthyPct),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CacaoForestPrimary
                        )
                        Text(
                            text = "Sanidade",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slices.forEach { (label, count, color) ->
                        val pct = if (totalFruits > 0) (count * 100.0 / totalFruits) else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = String.format(ptBr, "%d (%.1f%%)", count, pct),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Base amostrada: $totalFruits frutos avaliados",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = CacaoSurfaceVariant)

            Text(
                "Ações Recomendadas de Manejo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            val hasAnyDisease = podridaoFruits > 0 || vassouraFruits > 0 || moniliaseFruits > 0
            if (moniliaseFruits > 0) {
                ManagementNoticeBox(
                    title = "ALERTA QUARENTENÁRIO • Monilíase ($moniliaseFruits frutos)",
                    body = "Isolar imediatamente (${moniliasePlotNames.joinToString(", ").ifBlank { "quadra afetada" }}), evitar transporte de frutos suspeitos e acionar CEPLAC/ADAB.",
                    bgColor = MoniliaseQuarantineBg,
                    fgColor = MoniliaseQuarantineText,
                    borderColor = MoniliaseQuarantineBorder
                )
            }
            if (podridaoFruits > 0) {
                ManagementNoticeBox(
                    title = "Podridão Parda ($podridaoFruits frutos)",
                    body = "Priorizar colheita quinzenal dos frutos maduros, remoção de mumificados e aplicação direcionada de fungicida cúprico nas quadras críticas.",
                    bgColor = PodridaoMiniBg,
                    fgColor = PodridaoMiniText
                )
            }
            if (vassouraFruits > 0) {
                ManagementNoticeBox(
                    title = "Vassoura-de-Bruxa ($vassouraFruits frutos)",
                    body = "Executar poda fitossanitária de vassouras secas e retirada de frutos petrificados antes dos períodos chuvosos para cortar a fonte de inóculo.",
                    bgColor = VassouraMiniBg,
                    fgColor = VassouraMiniText
                )
            }
            if (!hasAnyDisease) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SeverityControlledBg
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SeverityControlledGreen
                        )
                        Text(
                            "Nenhuma incidência de doença detectada nas amostras deste período. Mantenha o monitoramento preventivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CacaoForestPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementNoticeBox(
    title: String,
    body: String,
    bgColor: Color,
    fgColor: Color,
    borderColor: Color = fgColor.copy(alpha = 0.25f)
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = fgColor
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = fgColor
                )
            }
        }
    }
}

@Composable
private fun CommercializationAndLaborCard(
    pricePerArrobaText: String,
    onPriceChange: (String) -> Unit,
    netArrobas: Double,
    grossArrobas: Double,
    lossArrobas: Double,
    currencyFmt: NumberFormat,
    ptBr: Locale
) {
    val price = pricePerArrobaText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val netRevenue = netArrobas * price
    val grossRevenue = grossArrobas * price
    val avoidedLossValue = lossArrobas * price
    val dryAlmondsKg = netArrobas * 15.0
    val bags60Kg = dryAlmondsKg / 60.0

    SolarCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = CacaoForestContainer.copy(alpha = 0.32f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Projeção Comercial & Logística",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Simulação de apoio à comercialização antecipada, contratação de mão de obra e orçamento de manejo:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = pricePerArrobaText,
                onValueChange = onPriceChange,
                label = { Text("Cotação de referência (R$ / @)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Receita Líquida (NFS)", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = currencyFmt.format(netRevenue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CacaoForestPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(ptBr, "Bruta: %s", currencyFmt.format(grossRevenue)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SeverityCriticalBg,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Impacto por Doenças", style = MaterialTheme.typography.labelSmall, color = SeverityCriticalRed)
                        Text(
                            text = currencyFmt.format(avoidedLossValue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SeverityCriticalRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(ptBr, "%.1f @ perdidas", lossArrobas),
                            style = MaterialTheme.typography.labelSmall,
                            color = SeverityCriticalRed,
                            maxLines = 1
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SeverityWarningBg
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Volume p/ Colheita & Mão de Obra:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format(ptBr, "%,.0f kg (%.1f sacas 60kg)", dryAlmondsKg, bags60Kg),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CacaoPodSecondary
                    )
                }
            }
        }
    }
}
