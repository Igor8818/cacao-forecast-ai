package com.cacaoforecast.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.ui.theme.BrandIdentity
import com.cacaoforecast.ai.ui.theme.CacaoCardBorder
import com.cacaoforecast.ai.ui.theme.CacaoForestContainer
import com.cacaoforecast.ai.ui.theme.CacaoForestOnContainer
import com.cacaoforecast.ai.ui.theme.CacaoForestPrimary
import com.cacaoforecast.ai.ui.theme.CacaoSurfaceCard
import com.cacaoforecast.ai.ui.theme.SeverityControlledBg
import com.cacaoforecast.ai.ui.theme.SeverityControlledGreen
import com.cacaoforecast.ai.ui.theme.SeverityCriticalBg
import com.cacaoforecast.ai.ui.theme.SeverityCriticalRed
import com.cacaoforecast.ai.ui.theme.SeverityWarningAmber
import com.cacaoforecast.ai.ui.theme.SeverityWarningBg
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandTopAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                        maxLines = 1
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun SolarCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = CacaoSurfaceCard,
    borderColor: Color = CacaoCardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val border = BorderStroke(1.dp, borderColor)
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            border = border,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            border = border,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            content = content
        )
    }
}

@Composable
fun KpiStatCard(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color = CacaoForestPrimary,
    containerColor: Color = CacaoSurfaceCard
) {
    SolarCard(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SeverityPill(
    severity: DiseaseSeverity,
    lossPct: Double? = null,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon, label) = when (severity) {
        DiseaseSeverity.CONTROLLED -> Quad(SeverityControlledBg, SeverityControlledGreen, Icons.Default.CheckCircle, "Controlado")
        DiseaseSeverity.WARNING -> Quad(SeverityWarningBg, SeverityWarningAmber, Icons.Default.Warning, "Atenção")
        DiseaseSeverity.CRITICAL -> Quad(SeverityCriticalBg, SeverityCriticalRed, Icons.Default.ReportProblem, "Crítico")
    }
    val text = if (lossPct != null) {
        String.format(Locale("pt", "BR"), "%.1f%% • %s", lossPct, label)
    } else {
        label
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
fun OnboardingWelcomeCard(
    onCreateFirstFarm: () -> Unit,
    onOpenGlossary: () -> Unit
) {
    SolarCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CacaoForestPrimary.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(CacaoForestContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = CacaoForestOnContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Bem-vindo ao ${BrandIdentity.NAME}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Previsão de safra e sanidade 100% offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            OnboardingStepRow(
                step = "1",
                title = "Cadastre a Fazenda e divida em Roças",
                desc = "Informe área (ha), plantas (NPT) e use nossos presets de clones (CCN-51, Clonal, Comum)."
            )
            OnboardingStepRow(
                step = "2",
                title = "Fotografe a amostra de cacaueiros",
                desc = "A IA conta frutos sadios e identifica Podridão Parda, Monilíase e Vassoura sem internet."
            )
            OnboardingStepRow(
                step = "3",
                title = "Envie o PDF no WhatsApp ou exporte CSV",
                desc = "Acompanhe perdas, compare Estimado vs. Real e receba alertas quarentenários."
            )

            Button(
                onClick = onCreateFirstFarm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cadastrar Primeira Fazenda")
            }

            OutlinedButton(
                onClick = onOpenGlossary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Entender termos (NPT, FA, NPF, NFS/NFT)")
            }
        }
    }
}

@Composable
private fun OnboardingStepRow(step: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(CacaoForestPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryHelpSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Guia Rápido de Campo • ${BrandIdentity.NAME}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            GlossaryItem(
                term = "Roça / Quadra",
                desc = "Subdivisão homogênea da fazenda (mesmo clone, idade ou relevo). Permite analisar cada área separadamente ou somar toda a fazenda."
            )
            GlossaryItem(
                term = "NPT (Total de Plantas)",
                desc = "Quantidade aproximada de cacaueiros produtivos na roça. Você pode usar nossa calculadora automática por espaçamento (ex.: 3×3m) ao cadastrar."
            )
            GlossaryItem(
                term = "FA (Frutos por Arroba)",
                desc = "Quantos frutos sadios produzem 1 arroba (15 kg) de amêndoa seca. Referências: CCN-51 ≈ 220 • Clones CEPEC ≈ 250 • Comum ≈ 290."
            )
            GlossaryItem(
                term = "NPF (Meta de Fotos)",
                desc = "Quantas árvores serão fotografadas na caminhada em zigue-zague para estimar a roça (padrão CEPLAC: 20 plantas)."
            )
            GlossaryItem(
                term = "Safra Líquida (NFS) vs. Potencial Bruto (NFT)",
                desc = "Líquida (NFS) estima apenas os frutos sadios colhíveis/vendáveis. Bruta (NFT) mede a carga biológica total incluindo frutos doentes."
            )
            GlossaryItem(
                term = "Alerta Quarentenário (Monilíase)",
                desc = "Qualquer detecção de Monilíase dispara aviso prioritário para isolamento da área e acionamento técnico da CEPLAC."
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GlossaryItem(term: String, desc: String) {
    SolarCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = term,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CacaoForestPrimary
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
