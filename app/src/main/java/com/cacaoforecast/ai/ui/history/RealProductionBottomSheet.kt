package com.cacaoforecast.ai.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.cacaoforecast.ai.domain.model.EvaluationSummary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealProductionBottomSheet(
    evaluation: EvaluationSummary,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit
) {
    var realText by remember(evaluation.id) {
        mutableStateOf(
            evaluation.producaoRealArrobas?.let {
                String.format(Locale("pt", "BR"), "%.2f", it)
            } ?: ""
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Produção Real Colhida (@)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format(
                    Locale("pt", "BR"),
                    "%s • Estimada Líquida: %.1f\u00A0@ (Bruta: %.1f\u00A0@)",
                    evaluation.snapshotPlotName,
                    evaluation.producaoLiquidaArrobas,
                    evaluation.producaoBrutaArrobas
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = realText,
                onValueChange = {
                    realText = it
                    errorMsg = null
                },
                label = { Text("Produção Real (@)") },
                placeholder = { Text("Ex.: 74,5") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        val parsed = realText.trim().replace(',', '.').toDoubleOrNull()
                        if (parsed == null || !parsed.isFinite() || parsed < 0.0) {
                            errorMsg = "Informe um valor válido maior ou igual a zero."
                        } else {
                            onSave(parsed)
                        }
                    },
                    modifier = Modifier.weight(1.3f).height(52.dp)
                ) {
                    Text("Salvar Produção Real")
                }
            }

            if (evaluation.producaoRealArrobas != null) {
                TextButton(
                    onClick = { onSave(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Limpar lançamento anterior", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
