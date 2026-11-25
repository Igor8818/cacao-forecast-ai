package com.cacaoforecast.ai.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.Plot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CumulativePlaceholderContent(
    farms: List<Farm>,
    selectedFarmId: Long?,
    plots: List<Plot>,
    selectedPlotId: Long?,
    onSelectFarm: (Long) -> Unit,
    onSelectPlot: (Long) -> Unit,
    onOpenPlotHistory: (Long, Long) -> Unit
) {
    var farmExpanded by remember { mutableStateOf(false) }
    var plotExpanded by remember { mutableStateOf(false) }

    val activeFarm = farms.find { it.id == selectedFarmId } ?: farms.firstOrNull()
    val activePlot = plots.find { it.id == selectedPlotId } ?: plots.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
            ExposedDropdownMenuBox(
                expanded = plotExpanded,
                onExpandedChange = { plotExpanded = !plotExpanded }
            ) {
                OutlinedTextField(
                    value = activePlot.nome,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Roça / Quadra Específica") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(plotExpanded) },
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
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Relatório Cumulativo (Fase 2)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Esta visão comparará a evolução temporal entre ProdEstimada (@) e ProdReal (@) para a Roça / Quadra selecionada ao longo dos meses da safra.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Continue registrando a produção colhida no histórico de cada quadra para alimentar a curva de calibração.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (activeFarm != null && activePlot != null) {
            Button(
                onClick = { onOpenPlotHistory(activeFarm.id, activePlot.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lançar Produção Real no Histórico desta Quadra")
            }
        }
    }
}
