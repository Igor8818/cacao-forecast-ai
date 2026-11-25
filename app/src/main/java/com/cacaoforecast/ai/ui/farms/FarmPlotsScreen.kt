package com.cacaoforecast.ai.ui.farms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPlot by remember { mutableStateOf<Plot?>(null) }
    var plotToArchive by remember { mutableStateOf<Plot?>(null) }

    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current)
        viewModel.clearMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Roças / Quadras", fontWeight = FontWeight.Bold)
                        farm?.nome?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Nenhuma Roça / Quadra cadastrada",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Informe Área (A), Total de Plantas (NPT), Frutos/@ (FA) e Meta de Fotos (NPF).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Roça / Quadra")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plots, key = { it.id }) { plot ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
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
                                    Text(
                                        text = String.format(
                                            Locale("pt", "BR"),
                                            "%.2f ha • %d plantas • FA %.0f frutos/@ • Meta: %d fotos",
                                            plot.areaHa,
                                            plot.npt,
                                            plot.fa,
                                            plot.npf
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onStartSampling(farmId, plot.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Amostrar")
                                }
                                OutlinedButton(
                                    onClick = { onOpenHistory(farmId, plot.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Histórico / Prod. Real")
                                }
                            }
                        }
                    }
                }
            }
        }
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
