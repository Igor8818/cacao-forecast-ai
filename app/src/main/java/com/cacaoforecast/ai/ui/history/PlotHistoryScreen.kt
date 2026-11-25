package com.cacaoforecast.ai.ui.history

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.EvaluationSummary
import com.cacaoforecast.ai.ui.theme.SeverityControlledGreen
import com.cacaoforecast.ai.ui.theme.SeverityCriticalRed
import com.cacaoforecast.ai.ui.theme.SeverityWarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlotHistoryScreen(
    viewModel: PlotHistoryViewModel,
    onBack: () -> Unit
) {
    val plot by viewModel.plot.collectAsStateWithLifecycle()
    val evaluations by viewModel.evaluations.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedForEdit by remember { mutableStateOf<EvaluationSummary?>(null) }

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
                        Text("Histórico & Produção Real", fontWeight = FontWeight.Bold)
                        plot?.nome?.let {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (evaluations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma amostragem concluída nesta Roça / Quadra ainda.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val ptBr = Locale("pt", "BR")
            val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", ptBr) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(evaluations, key = { it.id }) { eval ->
                    val sevColor = when (eval.severity) {
                        DiseaseSeverity.CONTROLLED -> SeverityControlledGreen
                        DiseaseSeverity.WARNING -> SeverityWarningAmber
                        DiseaseSeverity.CRITICAL -> SeverityCriticalRed
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFmt.format(Date(eval.dataMillis)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(ptBr, "Perdas: %.1f%%", eval.perdasPct),
                                    color = sevColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = String.format(
                                    ptBr,
                                    "Estimada Líquida: %.1f @ (%.1f @/ha) • Bruta: %.1f @",
                                    eval.producaoLiquidaArrobas,
                                    eval.produtividadeLiquidaPorHa,
                                    eval.producaoBrutaArrobas
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Amostra: ${eval.nPlantasAmostradas}/${eval.snapshotNpf} plantas • Sadios: ${eval.nfs} • Doentes: ${eval.nfd}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            if (eval.producaoRealArrobas == null) {
                                AssistChip(
                                    onClick = { selectedForEdit = eval },
                                    label = { Text("+ Informar Produção Real (@)") }
                                )
                            } else {
                                val real = eval.producaoRealArrobas
                                val diff = real - eval.producaoLiquidaArrobas
                                val relErrorPct = if (real > 0.0) {
                                    (abs(eval.producaoLiquidaArrobas - real) / real) * 100.0
                                } else {
                                    0.0
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = String.format(
                                                ptBr,
                                                "Estimada vs. Real: %.1f @ vs. %.1f @",
                                                eval.producaoLiquidaArrobas,
                                                real
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = String.format(
                                                ptBr,
                                                "Diferença: %+.1f @ • Erro relativo: %.1f%%",
                                                diff,
                                                relErrorPct
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { selectedForEdit = eval }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar Produção Real")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedForEdit?.let { target ->
        RealProductionBottomSheet(
            evaluation = target,
            onDismiss = { selectedForEdit = null },
            onSave = { realValue ->
                viewModel.updateRealProduction(target.id, realValue) {
                    selectedForEdit = null
                }
            }
        )
    }
}
