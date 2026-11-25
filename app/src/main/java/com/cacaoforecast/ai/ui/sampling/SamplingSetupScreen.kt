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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.cacaoforecast.ai.ui.components.SolarCard
import com.cacaoforecast.ai.ui.theme.CacaoCardBorder
import com.cacaoforecast.ai.ui.theme.CacaoForestContainer
import com.cacaoforecast.ai.ui.theme.CacaoForestPrimary
import com.cacaoforecast.ai.ui.theme.CacaoSurfaceCard
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
    val ptBr = remember { Locale("pt", "BR") }

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
    var showGlossary by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = "Amostragem em Campo",
                subtitle = "Selecione a fazenda e a roça",
                actions = {
                    IconButton(onClick = { showGlossary = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Guia Rápido")
                    }
                }
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
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Cadastre uma fazenda primeiro", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Para iniciar a contagem com IA, cadastre ao menos uma Fazenda e uma Roça / Quadra.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onNavigateToFarms,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Ir para Fazendas")
                        }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                uiState.pendingResumeDraft?.let { draft ->
                    SolarCard(
                        containerColor = SeverityWarningBg,
                        borderColor = SeverityWarningAmber.copy(alpha = 0.5f),
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
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retomar")
                                }
                                TextButton(
                                    onClick = { viewModel.discardPendingDraft(draft.evaluationId) },
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text("Descartar", color = MaterialTheme.colorScheme.error)
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
                    SolarCard(modifier = Modifier.fillMaxWidth()) {
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
                    Text(
                        text = "Escolha a Roça / Quadra (1 toque):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    plots.forEach { plot ->
                        val isSelected = plot.id == selectedPlot?.id
                        SolarCard(
                            onClick = { viewModel.selectPlot(plot.id) },
                            containerColor = if (isSelected) CacaoForestContainer.copy(alpha = 0.45f) else CacaoSurfaceCard,
                            borderColor = if (isSelected) CacaoForestPrimary else CacaoCardBorder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) CacaoForestPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plot.nome,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format(
                                            ptBr,
                                            "%.2f ha • %d pés • FA %.0f • Meta: %d fotos",
                                            plot.areaHa,
                                            plot.npt,
                                            plot.fa,
                                            plot.npf
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    SolarCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = CacaoForestPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Dica de campo: caminhe em zigue-zague pela roça e tire 1 foto frontal do tronco/copa baixa (~1,5 m) de cada árvore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    selectedPlot?.let { plot ->
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
                                    modifier = Modifier
                                        .height(22.dp)
                                        .width(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Preparando IA CacauLens...")
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Iniciar Amostragem (${plot.npf} plantas)")
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
}
