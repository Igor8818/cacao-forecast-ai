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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.ui.components.BrandTopAppBar
import com.cacaoforecast.ai.ui.components.GlossaryHelpSheet
import com.cacaoforecast.ai.ui.components.OnboardingWelcomeCard
import com.cacaoforecast.ai.ui.components.SolarCard
import com.cacaoforecast.ai.ui.theme.BrandIdentity
import com.cacaoforecast.ai.ui.theme.CacaoForestPrimary
import java.util.Locale

@Composable
fun FarmsListScreen(
    viewModel: FarmsViewModel,
    onOpenPlots: (Long) -> Unit,
    onOpenBackup: () -> Unit
) {
    val farms by viewModel.farms.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val ptBr = remember { Locale("pt", "BR") }

    var editingFarm by remember { mutableStateOf<Farm?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var farmToArchive by remember { mutableStateOf<Farm?>(null) }
    var showGlossary by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current)
        viewModel.clearMessage()
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = BrandIdentity.NAME,
                subtitle = BrandIdentity.TAGLINE,
                actions = {
                    IconButton(onClick = { showGlossary = true }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Guia Rápido"
                        )
                    }
                    IconButton(onClick = onOpenBackup) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup e Restauração"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (farms.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova Fazenda")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (farms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                OnboardingWelcomeCard(
                    onCreateFirstFarm = { showCreateDialog = true },
                    onOpenGlossary = { showGlossary = true }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(farms, key = { it.id }) { farm ->
                    val summary by remember(farm.id) {
                        viewModel.observeFarmPlotsSummary(farm.id)
                    }.collectAsStateWithLifecycle(initialValue = 0 to 0.0)

                    SolarCard(
                        onClick = { onOpenPlots(farm.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = farm.nome,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!farm.municipioUf.isNullOrBlank()) {
                                    Text(
                                        text = farm.municipioUf,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!farm.proprietario.isNullOrBlank()) {
                                    Text(
                                        text = "Resp.: ${farm.proprietario}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (summary.first == 0) {
                                        "Nenhuma roça cadastrada • Toque para dividir áreas →"
                                    } else {
                                        String.format(
                                            ptBr,
                                            "%d roça(s) • %.2f ha totais • Gerenciar →",
                                            summary.first,
                                            summary.second
                                        )
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CacaoForestPrimary
                                )
                            }
                            IconButton(onClick = { editingFarm = farm }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar Fazenda")
                            }
                            IconButton(onClick = { farmToArchive = farm }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Arquivar Fazenda",
                                    tint = MaterialTheme.colorScheme.error
                                )
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

    if (showCreateDialog || editingFarm != null) {
        FarmFormDialog(
            initialFarm = editingFarm,
            onDismiss = {
                showCreateDialog = false
                editingFarm = null
            },
            onSave = { farm ->
                viewModel.saveFarm(farm) {
                    showCreateDialog = false
                    editingFarm = null
                }
            }
        )
    }

    farmToArchive?.let { target ->
        AlertDialog(
            onDismissRequest = { farmToArchive = null },
            title = { Text("Arquivar Fazenda?") },
            text = {
                Text("A fazenda \"${target.nome}\" e suas Roças / Quadras serão ocultadas de novas amostragens, preservando 100% do histórico já avaliado.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.archiveFarm(target.id)
                        farmToArchive = null
                    }
                ) {
                    Text("Arquivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { farmToArchive = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun FarmFormDialog(
    initialFarm: Farm?,
    onDismiss: () -> Unit,
    onSave: (Farm) -> Unit
) {
    var nome by remember(initialFarm) { mutableStateOf(initialFarm?.nome ?: "") }
    var municipio by remember(initialFarm) { mutableStateOf(initialFarm?.municipioUf ?: "") }
    var proprietario by remember(initialFarm) { mutableStateOf(initialFarm?.proprietario ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialFarm == null) "Nova Fazenda" else "Editar Fazenda") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = {
                        nome = it
                        errorMsg = null
                    },
                    label = { Text("Nome da Fazenda *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = municipio,
                    onValueChange = { municipio = it },
                    label = { Text("Município / UF") },
                    placeholder = { Text("Ex.: Ilhéus / BA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proprietario,
                    onValueChange = { proprietario = it },
                    label = { Text("Proprietário / Responsável") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = nome.trim()
                    if (clean.isEmpty()) {
                        errorMsg = "Informe o nome da fazenda."
                    } else {
                        onSave(
                            Farm(
                                id = initialFarm?.id ?: 0L,
                                nome = clean,
                                municipioUf = municipio.trim().takeIf { it.isNotEmpty() },
                                proprietario = proprietario.trim().takeIf { it.isNotEmpty() },
                                ativo = true
                            )
                        )
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
