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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoforecast.ai.ui.components.BrandTopAppBar
import com.cacaoforecast.ai.ui.components.KpiStatCard
import com.cacaoforecast.ai.ui.components.SeverityPill
import com.cacaoforecast.ai.ui.components.SolarCard
import com.cacaoforecast.ai.ui.theme.CacaoForestPrimary
import com.cacaoforecast.ai.ui.theme.CacaoPodSecondary
import com.cacaoforecast.ai.ui.theme.MoniliaseQuarantineBg
import com.cacaoforecast.ai.ui.theme.MoniliaseQuarantineBorder
import com.cacaoforecast.ai.ui.theme.MoniliaseQuarantineText
import com.cacaoforecast.ai.ui.theme.SeverityControlledGreen
import java.util.Locale

@Composable
fun SamplingSummaryScreen(
    evaluationId: Long,
    viewModel: SamplingViewModel,
    onGoToReports: (farmId: Long, evaluationDateMillis: Long) -> Unit,
    onDoneToSetup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(evaluationId) {
        viewModel.loadCompletedSummary(evaluationId)
    }

    val summary = uiState.completedSummary

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = "Avaliação Concluída",
                subtitle = summary?.snapshotPlotName
            )
        }
    ) { innerPadding ->
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SeverityControlledGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${summary.snapshotPlotName} salva offline",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SeverityPill(severity = summary.severity, lossPct = summary.perdasPct)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiStatCard(
                        label = "Líquida (NFS)",
                        value = String.format(ptBr, "%.1f @", summary.producaoLiquidaArrobas),
                        subtitle = String.format(ptBr, "%.1f @/ha", summary.produtividadeLiquidaPorHa),
                        accentColor = CacaoForestPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        label = "Bruta (NFT)",
                        value = String.format(ptBr, "%.1f @", summary.producaoBrutaArrobas),
                        subtitle = String.format(ptBr, "%.1f @/ha", summary.produtividadeBrutaPorHa),
                        accentColor = CacaoPodSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Resumo Auditado (${summary.nPlantasAmostradas}/${summary.snapshotNpf} plantas)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Frutos Sadios (NFS): ${summary.nfs}")
                        Text("Frutos Doentes (NFD): ${summary.nfd}")
                        Text("• Podridão Parda: ${summary.nPodridao}", style = MaterialTheme.typography.bodySmall)
                        Text("• Monilíase: ${summary.nMoniliase}", style = MaterialTheme.typography.bodySmall)
                        Text("• Vassoura de Bruxa: ${summary.nVassoura}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (summary.nMoniliase > 0) {
                    SolarCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MoniliaseQuarantineBg,
                        borderColor = MoniliaseQuarantineBorder
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MoniliaseQuarantineText
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ALERTA QUARENTENÁRIO: Monilíase detectada (${summary.nMoniliase} fruto(s))",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MoniliaseQuarantineText
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
                    onClick = { onGoToReports(summary.farmId, summary.dataMillis) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Relatórios da Fazenda")
                }

                OutlinedButton(
                    onClick = onDoneToSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Nova Amostragem")
                }
            }
        }
    }
}
