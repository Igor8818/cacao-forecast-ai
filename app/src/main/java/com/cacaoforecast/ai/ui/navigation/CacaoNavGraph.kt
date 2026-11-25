package com.cacaoforecast.ai.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cacaoforecast.ai.di.AppContainer
import com.cacaoforecast.ai.ui.backup.BackupRestoreScreen
import com.cacaoforecast.ai.ui.backup.BackupViewModel
import com.cacaoforecast.ai.ui.farms.FarmPlotsScreen
import com.cacaoforecast.ai.ui.farms.FarmsListScreen
import com.cacaoforecast.ai.ui.farms.FarmsViewModel
import com.cacaoforecast.ai.ui.history.PlotHistoryScreen
import com.cacaoforecast.ai.ui.history.PlotHistoryViewModel
import com.cacaoforecast.ai.ui.reports.FarmReportViewModel
import com.cacaoforecast.ai.ui.reports.ReportsHubScreen
import com.cacaoforecast.ai.ui.sampling.PlantPhotoReviewScreen
import com.cacaoforecast.ai.ui.sampling.SamplingCameraScreen
import com.cacaoforecast.ai.ui.sampling.SamplingSetupScreen
import com.cacaoforecast.ai.ui.sampling.SamplingSummaryScreen
import com.cacaoforecast.ai.ui.sampling.SamplingViewModel

@Composable
fun CacaoNavGraph(appContainer: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

        val samplingViewModel: SamplingViewModel = viewModel(
            factory = SamplingViewModel.Factory(
                repository = appContainer.repository,
                fruitAnalysisEngine = appContainer.fruitAnalysisEngine,
                sessionDirProvider = { evalId -> appContainer.sessionDirFor(evalId) }
            )
        )
        val farmsViewModel: FarmsViewModel = viewModel(
            factory = FarmsViewModel.Factory(appContainer.repository)
        )
        val reportViewModel: FarmReportViewModel = viewModel(
            factory = FarmReportViewModel.Factory(
                repository = appContainer.repository,
                pdfReportGenerator = appContainer.pdfReportGenerator,
                csvReportExporter = appContainer.csvReportExporter
            )
        )

        val showBottomBar = currentRoute in setOf(
            Routes.FARMS_LIST,
            Routes.SAMPLING_SETUP_PATTERN,
            Routes.REPORTS_HUB
        )

        fun navigateTopTab(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(modifier = Modifier.height(72.dp)) {
                        NavigationBarItem(
                            selected = currentRoute == Routes.FARMS_LIST,
                            onClick = { navigateTopTab(Routes.FARMS_LIST) },
                            icon = { Icon(Icons.Default.Agriculture, contentDescription = "Fazendas") },
                            label = { Text("Fazendas") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.SAMPLING_SETUP_PATTERN,
                            onClick = { navigateTopTab(Routes.samplingSetup()) },
                            icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Amostrar") },
                            label = { Text("Amostrar") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.REPORTS_HUB,
                            onClick = { navigateTopTab(Routes.REPORTS_HUB) },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "Relatórios") },
                            label = { Text("Relatórios") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.FARMS_LIST,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.FARMS_LIST) {
                    FarmsListScreen(
                        viewModel = farmsViewModel,
                        onOpenPlots = { farmId -> navController.navigate(Routes.farmPlots(farmId)) },
                        onOpenBackup = { navController.navigate(Routes.BACKUP_RESTORE) }
                    )
                }

                composable(
                    route = Routes.FARM_PLOTS_PATTERN,
                    arguments = listOf(navArgument("farmId") { type = NavType.LongType })
                ) { entry ->
                    val farmId = entry.arguments?.getLong("farmId") ?: 0L
                    FarmPlotsScreen(
                        farmId = farmId,
                        viewModel = farmsViewModel,
                        onBack = { navController.popBackStack() },
                        onStartSampling = { fId, pId ->
                            navController.navigate(Routes.samplingSetup(fId, pId))
                        },
                        onOpenHistory = { fId, pId ->
                            navController.navigate(Routes.plotHistory(fId, pId))
                        }
                    )
                }

                composable(
                    route = Routes.PLOT_HISTORY_PATTERN,
                    arguments = listOf(
                        navArgument("farmId") { type = NavType.LongType },
                        navArgument("plotId") { type = NavType.LongType }
                    )
                ) { entry ->
                    val plotId = entry.arguments?.getLong("plotId") ?: 0L
                    val historyViewModel: PlotHistoryViewModel = viewModel(
                        factory = PlotHistoryViewModel.Factory(plotId, appContainer.repository)
                    )
                    PlotHistoryScreen(
                        viewModel = historyViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.BACKUP_RESTORE) {
                    val backupViewModel: BackupViewModel = viewModel(
                        factory = BackupViewModel.Factory(appContainer.backupManager)
                    )
                    BackupRestoreScreen(
                        viewModel = backupViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.SAMPLING_SETUP_PATTERN,
                    arguments = listOf(
                        navArgument("farmId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("plotId") { type = NavType.LongType; defaultValue = -1L }
                    )
                ) { entry ->
                    val preFarm = entry.arguments?.getLong("farmId")?.takeIf { it > 0L }
                    val prePlot = entry.arguments?.getLong("plotId")?.takeIf { it > 0L }
                    SamplingSetupScreen(
                        viewModel = samplingViewModel,
                        preselectedFarmId = preFarm,
                        preselectedPlotId = prePlot,
                        onNavigateToFarms = { navigateTopTab(Routes.FARMS_LIST) },
                        onOpenCamera = { evalId ->
                            navController.navigate(Routes.samplingCamera(evalId))
                        }
                    )
                }

                composable(
                    route = Routes.SAMPLING_CAMERA_PATTERN,
                    arguments = listOf(navArgument("evaluationId") { type = NavType.LongType })
                ) { entry ->
                    val evalId = entry.arguments?.getLong("evaluationId") ?: 0L
                    SamplingCameraScreen(
                        evaluationId = evalId,
                        viewModel = samplingViewModel,
                        onPhotoCaptured = { id, plantIdx, uriStr ->
                            navController.navigate(Routes.plantReview(id, plantIdx, uriStr))
                        },
                        onExitToSetup = {
                            navController.popBackStack(Routes.SAMPLING_SETUP_PATTERN, inclusive = false)
                        }
                    )
                }

                composable(
                    route = Routes.PLANT_REVIEW_PATTERN,
                    arguments = listOf(
                        navArgument("evaluationId") { type = NavType.LongType },
                        navArgument("plantIndex") { type = NavType.IntType },
                        navArgument("photoUri") { type = NavType.StringType }
                    )
                ) { entry ->
                    val evalId = entry.arguments?.getLong("evaluationId") ?: 0L
                    val plantIndex = entry.arguments?.getInt("plantIndex") ?: 1
                    val encodedUri = entry.arguments?.getString("photoUri") ?: ""
                    val decodedUri = Uri.decode(encodedUri)
                    PlantPhotoReviewScreen(
                        evaluationId = evalId,
                        plantIndex = plantIndex,
                        photoUriString = decodedUri,
                        viewModel = samplingViewModel,
                        onRetake = { navController.popBackStack() },
                        onNextPlant = { navController.popBackStack() },
                        onCompleted = { completedEvalId ->
                            navController.navigate(Routes.samplingSummary(completedEvalId)) {
                                popUpTo(Routes.SAMPLING_SETUP_PATTERN) { inclusive = false }
                            }
                        }
                    )
                }

                composable(
                    route = Routes.SAMPLING_SUMMARY_PATTERN,
                    arguments = listOf(navArgument("evaluationId") { type = NavType.LongType })
                ) { entry ->
                    val evalId = entry.arguments?.getLong("evaluationId") ?: 0L
                    SamplingSummaryScreen(
                        evaluationId = evalId,
                        viewModel = samplingViewModel,
                        onGoToReports = { navigateTopTab(Routes.REPORTS_HUB) },
                        onDoneToSetup = {
                            navController.popBackStack(Routes.SAMPLING_SETUP_PATTERN, inclusive = false)
                        }
                    )
                }

                composable(Routes.REPORTS_HUB) {
                    ReportsHubScreen(
                        viewModel = reportViewModel,
                        onNavigateToFarms = { navigateTopTab(Routes.FARMS_LIST) },
                        onOpenPlotHistory = { farmId, plotId ->
                            navController.navigate(Routes.plotHistory(farmId, plotId))
                        }
                    )
                }
            }
        }
}
