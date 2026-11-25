package com.cacaoforecast.ai.ui.farms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cacaoforecast.ai.domain.calculator.ForecastCalculator
import com.cacaoforecast.ai.domain.calculator.PlotPresetHelper
import com.cacaoforecast.ai.domain.model.Plot

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlotFormDialog(
    farmId: Long,
    initialPlot: Plot?,
    onDismiss: () -> Unit,
    onSave: (Plot) -> Unit
) {
    var nome by remember(initialPlot) { mutableStateOf(initialPlot?.nome ?: "") }
    var areaText by remember(initialPlot) {
        mutableStateOf(initialPlot?.areaHa?.toString()?.replace('.', ',') ?: "")
    }
    var nptText by remember(initialPlot) { mutableStateOf(initialPlot?.npt?.toString() ?: "") }
    var faText by remember(initialPlot) {
        mutableStateOf(initialPlot?.fa?.toString()?.replace('.', ',') ?: "250")
    }
    var npfText by remember(initialPlot) { mutableStateOf((initialPlot?.npf ?: 20).toString()) }
    var showNptCalculator by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun parsePositiveDecimal(raw: String): Double? {
        val normalized = raw.trim().replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value.isFinite() && value > 0.0) value else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialPlot == null) "Nova Roça / Quadra" else "Editar Roça / Quadra")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = {
                        nome = it
                        errorMsg = null
                    },
                    label = { Text("Nome da Roça / Quadra *") },
                    placeholder = { Text("Ex.: Roça Baixada CCN-51") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = areaText,
                        onValueChange = {
                            areaText = it
                            errorMsg = null
                        },
                        label = { Text("Área (ha) *") },
                        placeholder = { Text("Ex.: 2,5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = nptText,
                        onValueChange = {
                            nptText = it.filter { ch -> ch.isDigit() }
                            errorMsg = null
                        },
                        label = { Text("NPT (Plantas) *") },
                        placeholder = { Text("Ex.: 2000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Calculadora assistida opcional de NPT por espaçamento
                TextButton(
                    onClick = { showNptCalculator = !showNptCalculator },
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        if (showNptCalculator) "Ocultar calculadora de plantas"
                        else "Não sabe o total de plantas? Estimar pelo espaçamento"
                    )
                }
                if (showNptCalculator) {
                    val areaVal = parsePositiveDecimal(areaText)
                    if (areaVal == null) {
                        Text(
                            text = "Preencha a Área (ha) acima para calcular o NPT automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PlotPresetHelper.spacingPresets.forEach { preset ->
                                SuggestionChip(
                                    onClick = {
                                        PlotPresetHelper.estimateNpt(areaVal, preset.plantsPerHa)?.let { est ->
                                            nptText = est.toString()
                                            errorMsg = null
                                        }
                                    },
                                    label = { Text(preset.label) }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = faText,
                        onValueChange = {
                            faText = it
                            errorMsg = null
                        },
                        label = { Text("FA (Frutos/@) *") },
                        placeholder = { Text("Ex.: 250") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = npfText,
                        onValueChange = {
                            npfText = it.filter { ch -> ch.isDigit() }
                            errorMsg = null
                        },
                        label = { Text("Meta Fotos (NPF) *") },
                        placeholder = { Text("20") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Atalho FA por clone:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val currentFa = parsePositiveDecimal(faText)
                    PlotPresetHelper.cloneFaPresets.forEach { preset ->
                        FilterChip(
                            selected = currentFa == preset.fa,
                            onClick = {
                                faText = preset.fa.toInt().toString()
                                errorMsg = null
                            },
                            label = { Text(preset.label) }
                        )
                    }
                }

                Text(
                    text = "Atalho Meta de Fotos (NPF):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val currentNpf = npfText.trim().toIntOrNull()
                    PlotPresetHelper.npfPresets.forEach { presetNpf ->
                        FilterChip(
                            selected = currentNpf == presetNpf,
                            onClick = {
                                npfText = presetNpf.toString()
                                errorMsg = null
                            },
                            label = { Text("$presetNpf plantas") }
                        )
                    }
                }

                Text(
                    text = "A > 0, NPT > 0, FA > 0 e NPF entre 1 e 500 plantas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = nome.trim()
                    val area = parsePositiveDecimal(areaText)
                    val npt = nptText.trim().toIntOrNull()
                    val fa = parsePositiveDecimal(faText)
                    val npf = npfText.trim().toIntOrNull()

                    when {
                        cleanName.isEmpty() -> errorMsg = "Informe o nome da Roça / Quadra."
                        area == null -> errorMsg = "Área (ha) deve ser maior que zero."
                        npt == null || npt <= 0 -> errorMsg = "NPT deve ser um inteiro maior que zero."
                        fa == null -> errorMsg = "FA (frutos/@) deve ser maior que zero."
                        npf == null || npf !in ForecastCalculator.MIN_NPF..ForecastCalculator.MAX_NPF ->
                            errorMsg = "NPF deve estar entre ${ForecastCalculator.MIN_NPF} e ${ForecastCalculator.MAX_NPF}."
                        else -> onSave(
                            Plot(
                                id = initialPlot?.id ?: 0L,
                                fazendaId = farmId,
                                nome = cleanName,
                                areaHa = area,
                                npt = npt,
                                fa = fa,
                                npf = npf,
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
