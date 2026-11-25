package com.cacaoforecast.ai.ui.reports

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.cacaoforecast.ai.ui.theme.SeverityControlledBg
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
    onShareWhatsApp: () -> Unit,
    onShareGenericPdf: () -> Unit,
    onNavigateToFarms: () -> Unit
) {
    if (farms.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Nenhuma fazenda cadastrada", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Cadastre uma fazenda e realize amostragens para gerar relatórios consolidados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onNavigateToFarms, modifier = Modifier.height(52.dp)) {
                    Text("Cadastrar Fazenda")
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
    val ptBr = Locale("pt", "BR")
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
                modifier = Modifier.weight(1f).height(54.dp)
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
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
            // Card de Destaque Fitossanitário
            report.criticalHighlightRow?.let { crit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SeverityCriticalBg)
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
                                text = "Predomínio: ${dominantDiseaseLabel(crit.dominantDisease)}",
                                style = MaterialTheme.typography.bodyMedium
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

            // Tabela de 6 colunas oficiais + Rodapé Consolidado
            val hScroll = rememberScrollState()
            Card(modifier = Modifier.fillMaxWidth()) {
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
                        TableCell("Roça", 150.dp, Color.White, FontWeight.Bold, TextAlign.Start)
                        TableCell("Área (ha)", 76.dp, Color.White, FontWeight.Bold)
                        TableCell("Npla", 70.dp, Color.White, FontWeight.Bold)
                        TableCell(basisHeader, 96.dp, Color.White, FontWeight.Bold)
                        TableCell("Produt. (@/ha)", 100.dp, Color.White, FontWeight.Bold)
                        TableCell("Perdas (%)", 84.dp, Color.White, FontWeight.Bold)
                    }

                    report.rows.forEach { row ->
                        val rowBg = when (row.severity) {
                            DiseaseSeverity.CRITICAL -> SeverityCriticalBg
                            DiseaseSeverity.WARNING -> SeverityWarningBg
                            DiseaseSeverity.CONTROLLED -> Color.Transparent
                        }
                        val lossColor = when (row.severity) {
                            DiseaseSeverity.CRITICAL -> SeverityCriticalRed
                            DiseaseSeverity.WARNING -> SeverityWarningAmber
                            DiseaseSeverity.CONTROLLED -> SeverityControlledGreen
                        }
                        val weight = if (row.severity == DiseaseSeverity.CRITICAL) FontWeight.Bold else FontWeight.Normal

                        Row(
                            modifier = Modifier
                                .background(rowBg)
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.width(150.dp)) {
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
                            TableCell(String.format(ptBr, "%.1f%%", row.perdasPct), 84.dp, lossColor, FontWeight.Bold)
                        }
                        HorizontalDivider()
                    }

                    // Rodapé fixo Total / Consolidado da Fazenda
                    val f = report.footer
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell("TOTAL FAZENDA", 150.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold, TextAlign.Start)
                        TableCell(String.format(ptBr, "%.2f", f.totalAreaHa), 76.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold)
                        TableCell(f.totalNpt.toString(), 70.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold)
                        TableCell(String.format(ptBr, "%.1f", f.totalProducaoExibidaArrobas), 96.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold)
                        TableCell(String.format(ptBr, "%.1f", f.produtividadeConsolidadaPorHa), 100.dp, MaterialTheme.colorScheme.onSurface, FontWeight.Bold)
                        val totalLossColor = when (f.severity) {
                            DiseaseSeverity.CRITICAL -> SeverityCriticalRed
                            DiseaseSeverity.WARNING -> SeverityWarningAmber
                            DiseaseSeverity.CONTROLLED -> SeverityControlledGreen
                        }
                        TableCell(String.format(ptBr, "%.1f%%", f.perdasPonderadasPct), 84.dp, totalLossColor, FontWeight.Bold)
                    }
                }
            }

            val coverageText = if (report.footer.pendingPlotNames.isEmpty()) {
                "Cobertura: ${report.footer.totalPlotsEvaluated} de ${report.footer.totalActivePlotsInFarm} roças avaliadas no período."
            } else {
                "Cobertura: ${report.footer.totalPlotsEvaluated} de ${report.footer.totalActivePlotsInFarm} roças avaliadas (Pendentes: ${report.footer.pendingPlotNames.joinToString(", ")})"
            }
            Text(
                text = coverageText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Botões de PDF
            Button(
                onClick = onShareWhatsApp,
                enabled = !isGeneratingPdf && report.rows.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar PDF no WhatsApp")
            }
            OutlinedButton(
                onClick = onShareGenericPdf,
                enabled = !isGeneratingPdf && report.rows.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartilhar / Salvar PDF")
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
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
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
