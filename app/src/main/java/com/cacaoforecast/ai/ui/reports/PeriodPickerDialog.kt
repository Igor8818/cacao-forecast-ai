package com.cacaoforecast.ai.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun PeriodPickerDialog(
    onDismiss: () -> Unit,
    onSelectPeriod: (startMillis: Long, endMillis: Long, label: String) -> Unit
) {
    val nowCal = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    val currentMonth = nowCal.get(Calendar.MONTH)
    val months = listOf(
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar Período") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { year -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ano anterior")
                    }
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { year += 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo ano")
                    }
                }

                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 4) {
                            val mIdx = row * 4 + col
                            OutlinedButton(
                                onClick = {
                                    val (s, e) = FarmReportViewModel.monthRange(year, mIdx)
                                    onSelectPeriod(s, e, FarmReportViewModel.formatMonthLabel(year, mIdx))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(months[mIdx], maxLines = 1)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val (s, _) = FarmReportViewModel.monthRange(year, 0)
                            val (_, e) = FarmReportViewModel.monthRange(year, 11)
                            onSelectPeriod(s, e, "Ano $year")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ano Todo")
                    }
                    OutlinedButton(
                        onClick = {
                            val yNow = nowCal.get(Calendar.YEAR)
                            val (s, e) = FarmReportViewModel.monthRange(yNow, currentMonth)
                            onSelectPeriod(s, e, FarmReportViewModel.formatMonthLabel(yNow, currentMonth))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mês Atual")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
