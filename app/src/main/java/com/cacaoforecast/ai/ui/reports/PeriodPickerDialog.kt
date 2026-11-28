package com.cacaoforecast.ai.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    currentPeriodLabel: String = "",
    allowAllHistory: Boolean = false,
    onSelectAllHistory: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSelectPeriod: (startMillis: Long, endMillis: Long, label: String) -> Unit
) {
    val nowCal = remember { Calendar.getInstance() }
    val initialYear = remember(currentPeriodLabel) {
        Regex("""(\d{4})$""").find(currentPeriodLabel)?.groupValues?.get(1)?.toIntOrNull()
            ?: nowCal.get(Calendar.YEAR)
    }
    var year by remember(currentPeriodLabel) { mutableIntStateOf(initialYear) }
    val currentMonth = nowCal.get(Calendar.MONTH)
    val months = listOf(
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    )
    val compactPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
    val actionPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar Período") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            val itemLabel = FarmReportViewModel.formatMonthLabel(year, mIdx)
                            val isSelected = currentPeriodLabel == itemLabel
                            val onPickMonth = {
                                val (s, e) = FarmReportViewModel.monthRange(year, mIdx)
                                onSelectPeriod(s, e, itemLabel)
                            }
                            if (isSelected) {
                                Button(
                                    onClick = onPickMonth,
                                    contentPadding = compactPadding,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Text(
                                        text = months[mIdx],
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onPickMonth,
                                    contentPadding = compactPadding,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Text(
                                        text = months[mIdx],
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                val fullYearLabel = "Ano $year"
                val isFullYearSelected = currentPeriodLabel == fullYearLabel
                val currentMonthLabel = FarmReportViewModel.formatMonthLabel(
                    nowCal.get(Calendar.YEAR),
                    currentMonth
                )
                val isCurrentMonthSelected = currentPeriodLabel == currentMonthLabel

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val onPickYear = {
                        val (s, _) = FarmReportViewModel.monthRange(year, 0)
                        val (_, e) = FarmReportViewModel.monthRange(year, 11)
                        onSelectPeriod(s, e, fullYearLabel)
                    }
                    if (isFullYearSelected) {
                        Button(
                            onClick = onPickYear,
                            contentPadding = actionPadding,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ano Todo", maxLines = 1, softWrap = false)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPickYear,
                            contentPadding = actionPadding,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ano Todo", maxLines = 1, softWrap = false)
                        }
                    }

                    val onPickCurrent = {
                        val yNow = nowCal.get(Calendar.YEAR)
                        val (s, e) = FarmReportViewModel.monthRange(yNow, currentMonth)
                        onSelectPeriod(s, e, currentMonthLabel)
                    }
                    if (isCurrentMonthSelected) {
                        Button(
                            onClick = onPickCurrent,
                            contentPadding = actionPadding,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mês Atual", maxLines = 1, softWrap = false)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPickCurrent,
                            contentPadding = actionPadding,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mês Atual", maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (allowAllHistory && onSelectAllHistory != null) {
                    val isAllHistorySelected = currentPeriodLabel == "Todo o Histórico"
                    if (isAllHistorySelected) {
                        Button(
                            onClick = onSelectAllHistory,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Todo o Histórico", maxLines = 1, softWrap = false)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSelectAllHistory,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Todo o Histórico", maxLines = 1, softWrap = false)
                        }
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
