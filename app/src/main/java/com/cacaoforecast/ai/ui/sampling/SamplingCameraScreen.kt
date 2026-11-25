package com.cacaoforecast.ai.ui.sampling

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun SamplingCameraScreen(
    evaluationId: Long,
    viewModel: SamplingViewModel,
    onPhotoCaptured: (Long, Int, String) -> Unit,
    onExitToSetup: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(evaluationId) {
        viewModel.loadSessionIfNeeded(evaluationId)
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    val draft = uiState.activeDraft
    val plantIndex = draft?.nextPlantIndex ?: uiState.currentPlantIndex
    val targetNpf = draft?.snapshotNpf ?: 20
    val progress = if (targetNpf > 0) (draft?.sampledCount ?: 0).toFloat() / targetNpf.toFloat() else 0f

    val imageCapture = remember { createCacaoImageCapture() }
    var torchEnabled by remember { mutableStateOf(false) }
    var torchSupported by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(4f) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { pickedUri ->
        if (pickedUri == null) return@rememberLauncherForActivityResult
        isCapturing = true
        cameraError = null
        coroutineScope.launch {
            val copyResult = withContext(Dispatchers.IO) {
                runCatching {
                    val targetFile = viewModel.sessionPhotoFile(evaluationId, plantIndex)
                    targetFile.parentFile?.mkdirs()
                    val input = context.contentResolver.openInputStream(pickedUri)
                        ?: error("Não foi possível abrir a imagem selecionada.")
                    input.use { stream ->
                        FileOutputStream(targetFile).use { out ->
                            stream.copyTo(out)
                        }
                    }
                    Uri.fromFile(targetFile).toString()
                }
            }
            isCapturing = false
            copyResult
                .onSuccess { uriStr -> onPhotoCaptured(evaluationId, plantIndex, uriStr) }
                .onFailure { err -> cameraError = err.message ?: "Erro ao importar imagem da galeria." }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Permissão de Câmera Necessária",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Para fotografar os cacaueiros e contar frutos sadios e doentes offline, conceda acesso à câmera nas configurações.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.weight(1f).height(52.dp)
                            ) {
                                Text("Permitir")
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f).height(52.dp)
                            ) {
                                Text("Configurações")
                            }
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            enabled = !isCapturing,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importar Foto da Galeria (Modo Teste)")
                        }
                        TextButton(onClick = { showExitDialog = true }) {
                            Text("Voltar")
                        }
                    }
                }
            }
        } else {
            CameraPreviewBox(
                imageCapture = imageCapture,
                torchEnabled = torchEnabled,
                zoomRatio = zoomRatio,
                onTorchSupported = { torchSupported = it },
                onMaxZoomAvailable = { maxZoom = it },
                onZoomRatioChanged = { zoomRatio = it },
                onCameraError = { cameraError = it }
            )

            FramingReticleOverlay(modifier = Modifier.fillMaxSize())

            // Cabeçalho fixo gigante k/NPF
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { showExitDialog = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Sair",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = String.format(Locale("pt", "BR"), "Planta %02d de %02d", plantIndex, targetNpf),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${draft?.plotName ?: "Roça"} • Enquadre o tronco/copa baixa",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        if (torchSupported) {
                            IconButton(onClick = { torchEnabled = !torchEnabled }) {
                                Icon(
                                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Lanterna",
                                    tint = if (torchEnabled) Color.Yellow else Color.White
                                )
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFF4CAF50),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            // Rodapé com controles de zoom e disparo (sem botão Finalizar antes de NPF/NPF)
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    cameraError?.let {
                        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1f, 2f, 3f).filter { it <= maxZoom + 0.1f }.forEach { level ->
                            val isActiveZoom = abs(zoomRatio - level) < 0.35f
                            if (isActiveZoom) {
                                Button(
                                    onClick = { zoomRatio = level.coerceAtMost(maxZoom) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF4B400),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("${level.toInt()}x", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { zoomRatio = level.coerceAtMost(maxZoom) }
                                ) {
                                    Text("${level.toInt()}x", color = Color.White)
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            enabled = !isCapturing,
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Importar da Galeria",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galeria", color = Color.White)
                        }
                        Button(
                            onClick = {
                                if (isCapturing) return@Button
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                isCapturing = true
                                cameraError = null
                                val file = viewModel.sessionPhotoFile(evaluationId, plantIndex)
                                captureToPrivateFile(
                                    context = context,
                                    imageCapture = imageCapture,
                                    targetFile = file,
                                    onError = { err ->
                                        isCapturing = false
                                        cameraError = err
                                    },
                                    onSuccess = { uri ->
                                        isCapturing = false
                                        onPhotoCaptured(evaluationId, plantIndex, uri.toString())
                                    }
                                )
                            },
                            enabled = !isCapturing,
                            shape = CircleShape,
                            modifier = Modifier.size(76.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capturar Planta",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Text(
                        text = "Sadios acumulados: ${draft?.totalNfs ?: 0} • Doentes: ${draft?.totalNfd ?: 0}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Sair da Amostragem?") },
            text = {
                Text("Você já salvou ${draft?.sampledCount ?: 0} de $targetNpf plantas. Deseja manter o rascunho para retomar depois ou cancelar esta sessão?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.saveDraftAndExit(onExitToSetup)
                    }
                ) {
                    Text("Salvar rascunho e sair")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.discardActiveSession(onExitToSetup)
                    }
                ) {
                    Text("Cancelar amostragem", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
private fun FramingReticleOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width * 0.72f
        val h = size.height * 0.44f
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f
        val right = left + w
        val bottom = top + h
        val arm = 36f
        val stroke = 4f
        val reticleColor = Color.White.copy(alpha = 0.55f)

        // Canto superior esquerdo
        drawLine(reticleColor, Offset(left, top), Offset(left + arm, top), stroke)
        drawLine(reticleColor, Offset(left, top), Offset(left, top + arm), stroke)
        // Canto superior direito
        drawLine(reticleColor, Offset(right - arm, top), Offset(right, top), stroke)
        drawLine(reticleColor, Offset(right, top), Offset(right, top + arm), stroke)
        // Canto inferior esquerdo
        drawLine(reticleColor, Offset(left, bottom - arm), Offset(left, bottom), stroke)
        drawLine(reticleColor, Offset(left, bottom), Offset(left + arm, bottom), stroke)
        // Canto inferior direito
        drawLine(reticleColor, Offset(right - arm, bottom), Offset(right, bottom), stroke)
        drawLine(reticleColor, Offset(right, bottom - arm), Offset(right, bottom), stroke)
    }
}
