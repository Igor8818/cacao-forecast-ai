package com.cacaoforecast.ai.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.DominantDisease
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder
import com.cacaoforecast.ai.ui.components.KpiStatCard
import com.cacaoforecast.ai.ui.components.SeverityPill
import com.cacaoforecast.ai.ui.components.SolarCard
import com.cacaoforecast.ai.ui.theme.BoxMoniliaseColor
import com.cacaoforecast.ai.ui.theme.CacaoForestPrimary
import com.cacaoforecast.ai.ui.theme.CacaoGoldAccent
import com.cacaoforecast.ai.ui.theme.CacaoZebraRow
import com.cacaoforecast.ai.ui.theme.SeverityControlledGreen
import com.cacaoforecast.ai.ui.theme.SeverityCriticalBg
import com.cacaoforecast.ai.ui.theme.SeverityCriticalRed
import com.cacaoforecast.ai.ui.theme.SeverityWarningAmber
import com.cacaoforecast.ai.ui.theme.SeverityWarningBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabularReportContent(
    farms: List<Farm>,
    plots: List<Plot>,
    filters: ReportFilterState,
    report: FarmReportUiModel?,
    isGeneratingPdf: Boolean,
    onSelectFarm: (Long) -> Unit,
    onSelectPlotFilter: (Long?) -> Unit,
    onSelectPeriod: (Long, Long, String) -> Unit,
    onSelectBasis: (ProductionBasis) -> Unit,
    onSelectSortOrder: (ReportSortOrder) -> Unit,
    onOpenPlotHistory: (Long, Long) -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareGenericPdf: () -> Unit,
    onExportCsv: () -> Unit,
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
                        "Cadastre uma fazenda e realize amostragens para gerar relatórios consolidados.",
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
    var plotExpanded by remember { mutableStateOf(false) }
    var showPeriodDialog by remember { mutableStateOf(false) }

    val selectedFarm = report?.farm ?: farms.first()
    val selectedPlotName = plots.find { it.id == filters.selectedPlotId }?.nome ?: "Todas as Quadras"
    val ptBr = remember { Locale("pt", "BR") }
    val dayMonthFmt = remember { SimpleDateFormat("dd/MM", ptBr) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filtros principais
        ExposedDropdownMenuBox(
            expanded = farmExpanded,
            onExpandedChange = { farmExpanded = !farmExpanded }
        ) {
            OutlinedTextField(
                value = selectedFarm.nome,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showPeriodDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(filters.periodLabel, maxLines = 1)
            }

            ExposedDropdownMenuBox(
                expanded = plotExpanded,
                onExpandedChange = { plotExpanded = !plotExpanded },
                modifier = Modifier.weight(1.2f)
            ) {
                OutlinedTextField(
                    value = selectedPlotName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Quadras") },
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
                    DropdownMenuItem(
                        text = { Text("Todas as Quadras") },
                        onClick = {
                            onSelectPlotFilter(null)
                            plotExpanded = false
                        }
                    )
                    plots.forEach { plot ->
                        DropdownMenuItem(
                            text = { Text(plot.nome) },
                            onClick = {
                                onSelectPlotFilter(plot.id)
                                plotExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Toggle Critério: Safra Líquida Sadia (NFS) vs Potencial Bruto (NFT)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = filters.basis == ProductionBasis.NET_HEALTHY_NFS,
                onClick = { onSelectBasis(ProductionBasis.NET_HEALTHY_NFS) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Líquida Sadia (NFS)", maxLines = 1)
            }
            SegmentedButton(
                selected = filters.basis == ProductionBasis.GROSS_TOTAL_NFT,
                onClick = { onSelectBasis(ProductionBasis.GROSS_TOTAL_NFT) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Potencial Bruto (NFT)", maxLines = 1)
            }
        }

        if (report == null || report.rows.isEmpty()) {
            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Nenhuma amostragem concluída neste período para a fazenda selecionada.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Avaliações em rascunho (em andamento) não entram no relatório consolidado até a conclusão de NPF/NPF plantas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val f = report.footer
            val totalLossColor = when (f.severity) {
                DiseaseSeverity.CRITICAL -> SeverityCriticalRed
                DiseaseSeverity.WARNING -> SeverityWarningAmber
                DiseaseSeverity.CONTROLLED -> SeverityControlledGreen
            }

            // Faixa de 3 KPIs Executivos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiStatCard(
                    label = if (report.basis == ProductionBasis.NET_HEALTHY_NFS) "Safra Líquida" else "Potencial Bruto",
                    value = String.format(ptBr, "%.1f @", f.totalProducaoExibidaArrobas),
                    subtitle = "${f.totalPlotsEvaluated} de ${f.totalActivePlotsInFarm} roças",
                    accentColor = CacaoForestPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    label = "Produtividade",
                    value = String.format(ptBr, "%.1f @/ha", f.produtividadeConsolidadaPorHa),
                    subtitle = String.format(ptBr, "%.2f ha aval.", f.totalAreaHa),
                    accentColor = CacaoGoldAccent,
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    label = "Perda Ponderada",
                    value = String.format(ptBr, "%.1f%%", f.perdasPonderadasPct),
                    subtitle = String.format(ptBr, "-%.1f @ perdidas", f.totalPerdaArrobas),
                    accentColor = totalLossColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Card de Alerta Quarentenário de Monilíase (>= 1 fruto detectado)
            if (f.totalMoniliaseFruits > 0) {
                SolarCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = BoxMoniliaseColor.copy(alpha = 0.12f),
                    borderColor = BoxMoniliaseColor.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = BoxMoniliaseColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ALERTA QUARENTENÁRIO — Monilíase (${f.totalMoniliaseFruits} fruto(s))",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BoxMoniliaseColor
                            )
                            Text(
                                text = "Roças afetadas: ${f.moniliaseAlertPlotNames.joinToString(", ")}. Isolar área e acionar assistência técnica CEPLAC.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Card de Destaque Fitossanitário
            report.criticalHighlightRow?.let { crit ->
                SolarCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = SeverityCriticalBg,
                    borderColor = SeverityCriticalRed.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = SeverityCriticalRed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = String.format(
                                    ptBr,
                                    "Foco Prioritário: %s — %.1f%% perda",
                                    crit.plotName,
                                    crit.perdasPct
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SeverityCriticalRed
                            )
                            Text(
                                text = "Predomínio: ${dominantDiseaseLabel(crit.dominantDisease)} • Toque na linha abaixo para abrir o histórico",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Chips de Ordenação
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filters.sortOrder == ReportSortOrder.LOSS_DESC,
                    onClick = { onSelectSortOrder(ReportSortOrder.LOSS_DESC) },
                    label = { Text("Maior Perda (%)") }
                )
                FilterChip(
                    selected = filters.sortOrder == ReportSortOrder.PRODUCTION_DESC,
                    onClick = { onSelectSortOrder(ReportSortOrder.PRODUCTION_DESC) },
                    label = { Text("Maior Produção (@)") }
                )
                FilterChip(
                    selected = filters.sortOrder == ReportSortOrder.PLOT_NAME_ASC,
                    onClick = { onSelectSortOrder(ReportSortOrder.PLOT_NAME_ASC) },
                    label = { Text("Nome A–Z") }
                )
            }

            Text(
                text = "↔ Deslize lateralmente na tabela • Toque em uma roça para ver histórico",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tabela de 6 colunas oficiais + Rodapé Consolidado
            val hScroll = rememberScrollState()
            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(hScroll)
                ) {
                    val basisHeader = if (report.basis == ProductionBasis.NET_HEALTHY_NFS) {
                        "Prod. (@ Líq.)"
                    } else {
                        "Prod. (@ Bruta)"
                    }

                    // Header
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell("Roça", 156.dp, Color.White, FontWeight.Bold, TextAlign.Start)
                        TableCell("Área (ha)", 76.dp, Color.White, FontWeight.Bold)
                        TableCell("Npla", 70.dp, Color.White, FontWeight.Bold)
                        TableCell(basisHeader, 96.dp, Color.White, FontWeight.Bold)
                        TableCell("Produt. (@/ha)", 100.dp, Color.White, FontWeight.Bold)
                        TableCell("Perdas (%)", 132.dp, Color.White, FontWeight.Bold)
                    }

                    report.rows.forEachIndexed { idx, row ->
                        val rowBg = when (row.severity) {
                            DiseaseSeverity.CRITICAL -> SeverityCriticalBg
                            DiseaseSeverity.WARNING -> SeverityWarningBg
                            DiseaseSeverity.CONTROLLED -> if (idx % 2 == 1) CacaoZebraRow else Color.Transparent
                        }
                        val weight = if (row.severity == DiseaseSeverity.CRITICAL) FontWeight.Bold else FontWeight.Normal

                        Row(
                            modifier = Modifier
                                .background(rowBg)
                                .clickable { onOpenPlotHistory(report.farm.id, row.plotId) }
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.width(156.dp)) {
                                Text(
                                    text = row.plotName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (row.severity == DiseaseSeverity.CRITICAL) FontWeight.Bold else FontWeight.SemiBold
                                )
                                if (row.evaluationsCountInPeriod > 1) {
                                    Text(
                                        text = "Últ. ${dayMonthFmt.format(Date(row.evaluationDateMillis))} • ${row.evaluationsCountInPeriod} aval.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TableCell(String.format(ptBr, "%.2f", row.areaHa), 76.dp, MaterialTheme.colorScheme.onSurface, weight)
                            TableCell(row.npt.toString(), 70.dp, MaterialTheme.colorScheme.onSurface, weight)
                            TableCell(String.format(ptBr, "%.1f", row.producaoExibidaArrobas), 96.dp, MaterialTheme.colorScheme.onSurface, weight)
                            TableCell(String.format(ptBr, "%.1f", row.produtividadeExibidaPorHa), 100.dp, MaterialTheme.colorScheme.onSurface, weight)
                            Box(
                                modifier = Modifier.width(132.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                SeverityPill(severity = row.severity, lossPct = row.perdasPct)
                            }
                        }
                        HorizontalDivider()
                    }

                    // Rodapé fixo Total / Consolidado da Fazenda
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell("TOTAL FAZENDA", 156.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold, TextAlign.Start)
                        TableCell(String.format(ptBr, "%.2f", f.totalAreaHa), 76.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold)
                        TableCell(f.totalNpt.toString(), 70.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold)
                        TableCell(String.format(ptBr, "%.1f", f.totalProducaoExibidaArrobas), 96.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold)
                        TableCell(String.format(ptBr, "%.1f", f.produtividadeConsolidadaPorHa), 100.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold)
                        Box(
                            modifier = Modifier.width(132.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            SeverityPill(severity = f.severity, lossPct = f.perdasPonderadasPct)
                        }
                    }
                }
            }

            val coverageText = if (f.pendingPlotNames.isEmpty()) {
                "Cobertura: ${f.totalPlotsEvaluated} de ${f.totalActivePlotsInFarm} roças avaliadas no período."
            } else {
                "Cobertura: ${f.totalPlotsEvaluated} de ${f.totalActivePlotsInFarm} roças avaliadas (Pendentes: ${f.pendingPlotNames.joinToString(", ")})"
            }
            Text(
                text = coverageText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Barra limpa de Exportação
            Button(
                onClick = onShareWhatsApp,
                enabled = !isGeneratingPdf && report.rows.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar Relatório PDF no WhatsApp")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShareGenericPdf,
                    enabled = !isGeneratingPdf && report.rows.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvar PDF", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onExportCsv,
                    enabled = !isGeneratingPdf && report.rows.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Planilha (.csv)", maxLines = 1)
                }
            }
        }
    }

    if (showPeriodDialog) {
        PeriodPickerDialog(
            onDismiss = { showPeriodDialog = false },
            onSelectPeriod = { start, end, label ->
                onSelectPeriod(start, end, label)
                showPeriodDialog = false
            }
        )
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color,
    fontWeight: FontWeight,
    textAlign: TextAlign = TextAlign.End
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        color = color,
        fontWeight = fontWeight,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = textAlign,
        maxLines = 1
    )
}

private fun dominantDiseaseLabel(disease: DominantDisease): String = when (disease) {
    DominantDisease.NONE -> "Sem ocorrência"
    DominantDisease.PODRIDAO_PARDA -> "Podridão Parda"
    DominantDisease.MONILIASE -> "Monilíase"
    DominantDisease.VASSOURA_BRUXA -> "Vassoura de Bruxa"
}
