package com.cacaoforecast.ai.ui.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoforecast.ai.pdf.ReportShareHelper
import com.cacaoforecast.ai.ui.components.BrandTopAppBar
import com.cacaoforecast.ai.ui.components.GlossaryHelpSheet
import com.cacaoforecast.ai.ui.theme.BrandIdentity

@Composable
fun ReportsHubScreen(
    viewModel: FarmReportViewModel,
    onNavigateToFarms: () -> Unit,
    onOpenPlotHistory: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    val farms by viewModel.farms.collectAsStateWithLifecycle()
    val plots by viewModel.plotsForSelectedFarm.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val cumulativeReport by viewModel.cumulativeReport.collectAsStateWithLifecycle()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showGlossary by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current)
        viewModel.clearMessage()
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = "Relatórios",
                subtitle = BrandIdentity.NAME,
                actions = {
                    IconButton(onClick = { showGlossary = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Guia Rápido")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedSubTab) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Consolidado (Tabela & PDF)") }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Evolução (Gráfico)") }
                )
            }

            if (selectedSubTab == 0) {
                TabularReportContent(
                    farms = farms,
                    plots = plots,
                    filters = filters,
                    report = report,
                    isGeneratingPdf = isGeneratingPdf,
                    onSelectFarm = { viewModel.selectFarm(it) },
                    onSelectPlotFilter = { viewModel.selectPlotFilter(it) },
                    onSelectPeriod = { s, e, l -> viewModel.selectPeriod(s, e, l) },
                    onSelectBasis = { viewModel.selectBasis(it) },
                    onSelectSortOrder = { viewModel.selectSortOrder(it) },
                    onOpenPlotHistory = onOpenPlotHistory,
                    onShareWhatsApp = {
                        viewModel.generatePdf { file, caption ->
                            ReportShareHelper.sharePdfToWhatsApp(context, file, caption)
                        }
                    },
                    onShareGenericPdf = {
                        viewModel.generatePdf { file, caption ->
                            ReportShareHelper.shareFileGeneric(
                                context = context,
                                file = file,
                                mimeType = "application/pdf",
                                chooserTitle = "Compartilhar / Salvar Relatório PDF",
                                caption = caption
                            )
                        }
                    },
                    onExportCsv = {
                        viewModel.generateCsv { file, caption ->
                            ReportShareHelper.shareFileGeneric(
                                context = context,
                                file = file,
                                mimeType = "text/csv",
                                chooserTitle = "Exportar Planilha CSV",
                                caption = caption
                            )
                        }
                    },
                    onNavigateToFarms = onNavigateToFarms
                )
            } else {
                CumulativePlaceholderContent(
                    farms = farms,
                    selectedFarmId = filters.selectedFarmId ?: farms.firstOrNull()?.id,
                    plots = plots,
                    selectedPlotId = filters.cumulativePlotId ?: plots.firstOrNull()?.id,
                    basis = filters.basis,
                    cumulativeReport = cumulativeReport,
                    onSelectFarm = { viewModel.selectFarm(it) },
                    onSelectPlot = { viewModel.selectCumulativePlot(it) },
                    onSelectBasis = { viewModel.selectBasis(it) },
                    onOpenPlotHistory = onOpenPlotHistory
                )
            }
        }
    }

    if (showGlossary) {
        GlossaryHelpSheet(onDismiss = { showGlossary = false })
    }
}
