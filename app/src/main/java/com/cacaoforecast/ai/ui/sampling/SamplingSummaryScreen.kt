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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoforecast.ai.ui.theme.BoxMoniliaseColor
import com.cacaoforecast.ai.ui.theme.SeverityControlledGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplingSummaryScreen(
    evaluationId: Long,
    viewModel: SamplingViewModel,
    onGoToReports: () -> Unit,
    onDoneToSetup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(evaluationId) {
        viewModel.loadCompletedSummary(evaluationId)
    }

    val summary = uiState.completedSummary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avaliação Concluída", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (summary == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val ptBr = Locale("pt", "BR")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SeverityControlledGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${summary.snapshotPlotName} selada no banco offline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = String.format(
                                ptBr,
                                "Produção Líquida (NFS): %.1f @ (%.1f @/ha)",
                                summary.producaoLiquidaArrobas,
                                summary.produtividadeLiquidaPorHa
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(
                                ptBr,
                                "Potencial Bruto (NFT): %.1f @ (%.1f @/ha)",
                                summary.producaoBrutaArrobas,
                                summary.produtividadeBrutaPorHa
                            )
                        )
                        Text(
                            text = String.format(ptBr, "Perdas Fitossanitárias: %.1f%%", summary.perdasPct),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Resumo Auditado (${summary.nPlantasAmostradas}/${summary.snapshotNpf} plantas)", fontWeight = FontWeight.Bold)
                        Text("Frutos Sadios (NFS): ${summary.nfs}")
                        Text("Frutos Doentes (NFD): ${summary.nfd}")
                        Text("• Podridão Parda: ${summary.nPodridao}")
                        Text("• Monilíase: ${summary.nMoniliase}")
                        Text("• Vassoura de Bruxa: ${summary.nVassoura}")
                    }
                }

                if (summary.nMoniliase > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BoxMoniliaseColor.copy(alpha = 0.12f))
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
                                    text = "ALERTA QUARENTENÁRIO: Monilíase detectada (${summary.nMoniliase} fruto(s))",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BoxMoniliaseColor
                                )
                                Text(
                                    text = "Isole a área amostrada e comunique a assistência técnica CEPLAC para vistoria imediata.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onGoToReports,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Relatórios da Fazenda")
                }

                OutlinedButton(
                    onClick = onDoneToSetup,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Nova Amostragem")
                }
            }
        }
    }
}
