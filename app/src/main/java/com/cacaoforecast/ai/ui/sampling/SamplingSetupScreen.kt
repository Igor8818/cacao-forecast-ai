package com.cacaoforecast.ai.ui.sampling

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.cacaoforecast.ai.ui.theme.SeverityWarningAmber
import com.cacaoforecast.ai.ui.theme.SeverityWarningBg
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplingSetupScreen(
    viewModel: SamplingViewModel,
    preselectedFarmId: Long?,
    preselectedPlotId: Long?,
    onNavigateToFarms: () -> Unit,
    onOpenCamera: (Long) -> Unit
) {
    val farms by viewModel.farms.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(preselectedFarmId, preselectedPlotId) {
        viewModel.initializeSetup(preselectedFarmId, preselectedPlotId)
    }

    val selectedFarmId = uiState.selectedFarmId ?: farms.firstOrNull()?.id
    LaunchedEffect(farms, selectedFarmId) {
        if (uiState.selectedFarmId == null && selectedFarmId != null) {
            viewModel.selectFarm(selectedFarmId)
        }
    }

    val plotsFlow = remember(selectedFarmId) {
        if (selectedFarmId != null) viewModel.observePlotsForFarm(selectedFarmId) else flowOf(emptyList())
    }
    val plots by plotsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedPlot: Plot? = plots.find { it.id == uiState.selectedPlotId } ?: plots.firstOrNull()

    LaunchedEffect(plots, selectedPlot) {
        if (uiState.selectedPlotId == null && selectedPlot != null) {
            viewModel.selectPlot(selectedPlot.id)
        }
    }

    var farmDropdownExpanded by remember { mutableStateOf(false) }
    var plotDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amostragem em Campo", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (farms.isEmpty()) {
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
                    Text("Cadastre uma fazenda primeiro", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Para iniciar a contagem com IA, é necessário ter ao menos uma Fazenda e uma Roça / Quadra cadastradas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onNavigateToFarms,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Ir para Fazendas")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.pendingResumeDraft?.let { draft ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SeverityWarningBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = SeverityWarningAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sessão Pendente: ${draft.plotName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Retomar Amostragem (${draft.sampledCount}/${draft.snapshotNpf} plantas salvas) na ${draft.farmName}.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.resumeSession(draft.evaluationId) {
                                            onOpenCamera(draft.evaluationId)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retomar")
                                }
                                TextButton(
                                    onClick = { viewModel.discardPendingDraft(draft.evaluationId) },
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("Descartar Rascunho", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Seletor Fazenda
                val selectedFarm = farms.find { it.id == selectedFarmId }
                ExposedDropdownMenuBox(
                    expanded = farmDropdownExpanded,
                    onExpandedChange = { farmDropdownExpanded = !farmDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedFarm?.nome ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fazenda") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = farmDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = farmDropdownExpanded,
                        onDismissRequest = { farmDropdownExpanded = false }
                    ) {
                        farms.forEach { farm ->
                            DropdownMenuItem(
                                text = { Text(farm.nome) },
                                onClick = {
                                    viewModel.selectFarm(farm.id)
                                    farmDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (plots.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Esta fazenda ainda não possui Roças / Quadras.",
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedButton(
                                onClick = onNavigateToFarms,
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Cadastrar Roça / Quadra")
                            }
                        }
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = plotDropdownExpanded,
                        onExpandedChange = { plotDropdownExpanded = !plotDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPlot?.nome ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Roça / Quadra") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = plotDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = plotDropdownExpanded,
                            onDismissRequest = { plotDropdownExpanded = false }
                        ) {
                            plots.forEach { plot ->
                                DropdownMenuItem(
                                    text = { Text("${plot.nome} (${plot.npf} plantas)") },
                                    onClick = {
                                        viewModel.selectPlot(plot.id)
                                        plotDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    selectedPlot?.let { plot ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Parâmetros Congelados na Avaliação",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    String.format(
                                        Locale("pt", "BR"),
                                        "Área: %.2f ha • NPT: %d plantas • FA: %.0f frutos/@",
                                        plot.areaHa,
                                        plot.npt,
                                        plot.fa
                                    )
                                )
                                Text(
                                    "Meta obrigatória: ${plot.npf} plantas (1 foto por cacaueiro).",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        uiState.errorMessage?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.startNewSession(plot) { evaluationId ->
                                    onOpenCamera(evaluationId)
                                }
                            },
                            enabled = !uiState.isPreparingEngine,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (uiState.isPreparingEngine) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.height(22.dp).width(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Preparando IA CEPEC...")
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Iniciar Nova Amostragem (${plot.npf} plantas)")
                            }
                        }
                    }
                }
            }
        }
    }
}
