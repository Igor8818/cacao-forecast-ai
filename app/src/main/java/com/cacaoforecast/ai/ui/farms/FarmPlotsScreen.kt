package com.cacaoforecast.ai.ui.farms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.ui.components.BrandTopAppBar
import com.cacaoforecast.ai.ui.components.GlossaryHelpSheet
import com.cacaoforecast.ai.ui.components.KpiStatCard
import com.cacaoforecast.ai.ui.components.SolarCard
import com.cacaoforecast.ai.ui.theme.CacaoForestPrimary
import com.cacaoforecast.ai.ui.theme.CacaoPodSecondary
import com.cacaoforecast.ai.ui.theme.CacaoSurfaceVariant
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmPlotsScreen(
    farmId: Long,
    viewModel: FarmsViewModel,
    onBack: () -> Unit,
    onStartSampling: (Long, Long) -> Unit,
    onOpenHistory: (Long, Long) -> Unit
) {
    val farm by remember(farmId) { viewModel.observeFarm(farmId) }.collectAsStateWithLifecycle(initialValue = null)
    val plots by remember(farmId) { viewModel.observePlots(farmId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val ptBr = remember { Locale("pt", "BR") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPlot by remember { mutableStateOf<Plot?>(null) }
    var plotToArchive by remember { mutableStateOf<Plot?>(null) }
    var showGlossary by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current)
        viewModel.clearMessage()
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = "Roças / Quadras",
                subtitle = farm?.nome,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showGlossary = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Guia Rápido")
                    }
                }
            )
        },
        floatingActionButton = {
            if (plots.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Roça / Quadra")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (plots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Divida a fazenda em Roças / Quadras",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cada roça possui sua própria Área (ha), Total de Plantas (NPT), Frutos/@ (FA) e Meta de Fotos (NPF). Use nossos presets prontos de clone e espaçamento!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adicionar Roça / Quadra")
                        }
                    }
                }
            }
        } else {
            val totalArea = plots.sumOf { it.areaHa }
            val totalNpt = plots.sumOf { it.npt }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiStatCard(
                            label = "Áreas Ativas",
                            value = "${plots.size}",
                            subtitle = "roça(s)",
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            label = "Área Total",
                            value = String.format(ptBr, "%.2f ha", totalArea),
                            subtitle = "cadastrados",
                            accentColor = CacaoPodSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            label = "Cacaueiros",
                            value = "$totalNpt",
                            subtitle = "plantas",
                            accentColor = CacaoForestPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                items(plots, key = { it.id }) { plot ->
                    SolarCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plot.nome,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = { editingPlot = plot }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar Roça / Quadra")
                                }
                                IconButton(onClick = { plotToArchive = plot }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Arquivar Roça / Quadra",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ParamPill(String.format(ptBr, "%.2f ha", plot.areaHa))
                                ParamPill("${plot.npt} pés")
                                ParamPill(String.format(ptBr, "FA %.0f", plot.fa))
                                ParamPill("Meta: ${plot.npf} fotos")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onStartSampling(farmId, plot.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Amostrar", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { onOpenHistory(farmId, plot.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Histórico", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGlossary) {
        GlossaryHelpSheet(onDismiss = { showGlossary = false })
    }

    if (showCreateDialog || editingPlot != null) {
        PlotFormDialog(
            farmId = farmId,
            initialPlot = editingPlot,
            onDismiss = {
                showCreateDialog = false
                editingPlot = null
            },
            onSave = { plot ->
                viewModel.savePlot(plot) {
                    showCreateDialog = false
                    editingPlot = null
                }
            }
        )
    }

    plotToArchive?.let { target ->
        AlertDialog(
            onDismissRequest = { plotToArchive = null },
            title = { Text("Arquivar Roça / Quadra?") },
            text = {
                Text("\"${target.nome}\" será ocultada de novas amostragens. Avaliações já concluídas e relatórios anteriores permanecem intactos.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.archivePlot(target.id)
                        plotToArchive = null
                    }
                ) {
                    Text("Arquivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { plotToArchive = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ParamPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CacaoSurfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
