package com.cacaoforecast.ai.ui.sampling

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoforecast.ai.domain.ml.DetectedFruitBox
import com.cacaoforecast.ai.domain.ml.FruitHealthCategory
import com.cacaoforecast.ai.ml.BitmapExifLoader
import com.cacaoforecast.ai.ui.theme.BoxHealthyColor
import com.cacaoforecast.ai.ui.theme.BoxMoniliaseColor
import com.cacaoforecast.ai.ui.theme.BoxPodridaoColor
import com.cacaoforecast.ai.ui.theme.BoxVassouraColor
import com.cacaoforecast.ai.ui.theme.SeverityControlledGreen
import com.cacaoforecast.ai.ui.theme.SeverityCriticalRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantPhotoReviewScreen(
    evaluationId: Long,
    plantIndex: Int,
    photoUriString: String,
    viewModel: SamplingViewModel,
    onRetake: () -> Unit,
    onNextPlant: () -> Unit,
    onCompleted: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var previewBitmap by remember(photoUriString) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(evaluationId, plantIndex, photoUriString) {
        viewModel.analyzeCapturedPhoto(evaluationId, plantIndex, photoUriString)
        previewBitmap = withContext(Dispatchers.IO) {
            BitmapExifLoader.loadOrientedBitmap(context, Uri.parse(photoUriString), maxDim = 1280)
        }
    }

    DisposableEffect(photoUriString) {
        onDispose {
            val bmp = previewBitmap
            previewBitmap = null
            if (bmp != null && !bmp.isRecycled) {
                bmp.recycle()
            }
        }
    }

    val targetNpf = uiState.activeDraft?.snapshotNpf ?: plantIndex
    val isLastPlant = plantIndex >= targetNpf

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = String.format(Locale("pt", "BR"), "Revisão • Planta %02d/%02d", plantIndex, targetNpf),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Área da foto com caixas interativas (toque na caixa alterna Sadio -> Doente -> Remover)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val bmp = previewBitmap
                if (bmp != null && !bmp.isRecycled) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Foto da planta $plantIndex",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    DetectionOverlayCanvas(
                        boxes = uiState.detectedBoxes,
                        imageWidth = bmp.width.toFloat(),
                        imageHeight = bmp.height.toFloat(),
                        onBoxTap = { boxId -> viewModel.cycleDetectedBox(boxId) }
                    )
                }

                if (uiState.isAnalyzingPhoto) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Text(
                                text = "Analisando frutos e doenças...",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // Painel de ajuste rápido NFS / NFD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Toque em uma caixa para alternar (Sadio → Doente → Remover) ou ajuste nos botões:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReviewLegendItem(BoxHealthyColor, "Sadio")
                        ReviewLegendItem(BoxPodridaoColor, "P. Parda")
                        ReviewLegendItem(BoxMoniliaseColor, "Monil.")
                        ReviewLegendItem(BoxVassouraColor, "Vassoura")
                    }
                    if (uiState.inferenceTimeMs > 0L) {
                        Text(
                            text = "${uiState.inferenceTimeMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (uiState.reviewNfd > 0) {
                    Text(
                        text = "Doenças: Podridão ${uiState.reviewPodridao} • Monilíase ${uiState.reviewMoniliase} • Vassoura ${uiState.reviewVassoura}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.reviewMoniliase > 0) BoxMoniliaseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (uiState.reviewMoniliase > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                uiState.errorMessage?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CounterCard(
                        label = "Sadios (NFS)",
                        count = uiState.reviewNfs,
                        accent = SeverityControlledGreen,
                        onDecrement = { viewModel.adjustHealthyCount(-1) },
                        onIncrement = { viewModel.adjustHealthyCount(1) },
                        modifier = Modifier.weight(1f)
                    )
                    CounterCard(
                        label = "Doentes (NFD)",
                        count = uiState.reviewNfd,
                        accent = SeverityCriticalRed,
                        onDecrement = { viewModel.adjustDiseasedCount(-1) },
                        onIncrement = { viewModel.adjustDiseasedCount(1) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearCurrentPhotoReview()
                            onRetake()
                        },
                        enabled = !uiState.isAnalyzingPhoto && !uiState.isSavingPlant,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refazer", maxLines = 1)
                    }
                    Button(
                        onClick = {
                            viewModel.confirmCurrentPlant(
                                onNextPlant = onNextPlant,
                                onCompleted = onCompleted
                            )
                        },
                        enabled = !uiState.isAnalyzingPhoto && !uiState.isSavingPlant,
                        modifier = Modifier.weight(1.6f).height(56.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLastPlant) {
                                "Concluir ($plantIndex/$targetNpf) ✓"
                            } else {
                                "Próxima ($plantIndex/$targetNpf)"
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CounterCard(
    label: String,
    count: Int,
    accent: Color,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onDecrement) {
                    Icon(Icons.Default.Remove, contentDescription = "Diminuir $label")
                }
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar $label")
                }
            }
        }
    }
}

@Composable
private fun DetectionOverlayCanvas(
    boxes: List<DetectedFruitBox>,
    imageWidth: Float,
    imageHeight: Float,
    onBoxTap: (String) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(boxes, imageWidth, imageHeight) {
                detectTapGestures { offset ->
                    if (imageWidth <= 0f || imageHeight <= 0f) return@detectTapGestures
                    val viewW = size.width.toFloat()
                    val viewH = size.height.toFloat()
                    val scale = minOf(viewW / imageWidth, viewH / imageHeight)
                    val offsetX = (viewW - imageWidth * scale) / 2f
                    val offsetY = (viewH - imageHeight * scale) / 2f
                    val normX = ((offset.x - offsetX) / (imageWidth * scale))
                    val normY = ((offset.y - offsetY) / (imageHeight * scale))
                    val hit = boxes.lastOrNull { b ->
                        normX in b.normLeft..b.normRight && normY in b.normTop..b.normBottom
                    }
                    if (hit != null) {
                        onBoxTap(hit.id)
                    }
                }
            }
    ) {
        if (imageWidth <= 0f || imageHeight <= 0f) return@Canvas
        val scale = minOf(size.width / imageWidth, size.height / imageHeight)
        val drawnW = imageWidth * scale
        val drawnH = imageHeight * scale
        val offsetX = (size.width - drawnW) / 2f
        val offsetY = (size.height - drawnH) / 2f

        boxes.forEach { box ->
            val color = when (box.category) {
                FruitHealthCategory.SADIO -> BoxHealthyColor
                FruitHealthCategory.PODRIDAO_PARDA -> BoxPodridaoColor
                FruitHealthCategory.MONILIASE -> BoxMoniliaseColor
                FruitHealthCategory.VASSOURA_BRUXA -> BoxVassouraColor
            }
            val left = offsetX + box.normLeft * drawnW
            val top = offsetY + box.normTop * drawnH
            val boxW = (box.normRight - box.normLeft) * drawnW
            val boxH = (box.normBottom - box.normTop) * drawnH
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
