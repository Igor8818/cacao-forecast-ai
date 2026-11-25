package com.cacaoforecast.ai.ui.sampling

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

fun createCacaoImageCapture(): ImageCapture {
    val captureResSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(1600, 1200),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        .build()

    return ImageCapture.Builder()
        .setResolutionSelector(captureResSelector)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setJpegQuality(85)
        .build()
}

fun captureToPrivateFile(
    context: Context,
    imageCapture: ImageCapture,
    targetFile: File,
    onError: (String) -> Unit,
    onSuccess: (Uri) -> Unit
) {
    runCatching {
        targetFile.parentFile?.mkdirs()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(targetFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = outputFileResults.savedUri ?: Uri.fromFile(targetFile)
                    onSuccess(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Falha ao capturar foto.")
                }
            }
        )
    }.onFailure { err ->
        onError(err.message ?: "Falha ao preparar captura de foto.")
    }
}

/**
 * Preview CameraX leve (apenas Preview + ImageCapture 1600x1200, sem ImageAnalysis contínuo
 * para evitar drenagem térmica e contenção de RAM em campo).
 */
@Composable
fun CameraPreviewBox(
    imageCapture: ImageCapture,
    torchEnabled: Boolean,
    zoomRatio: Float,
    modifier: Modifier = Modifier,
    onTorchSupported: (Boolean) -> Unit = {},
    onMaxZoomAvailable: (Float) -> Unit = {},
    onZoomRatioChanged: (Float) -> Unit = {},
    onCameraError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val disposed = remember { AtomicBoolean(false) }

    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var zoomMinRef by remember { mutableFloatStateOf(1f) }
    var zoomMaxRef by remember { mutableFloatStateOf(4f) }

    LaunchedEffect(cameraRef, torchEnabled) {
        runCatching {
            if (cameraRef?.cameraInfo?.hasFlashUnit() == true) {
                cameraRef?.cameraControl?.enableTorch(torchEnabled)
            }
        }
    }

    LaunchedEffect(cameraRef, zoomRatio) {
        runCatching {
            val zMin = cameraRef?.cameraInfo?.zoomState?.value?.minZoomRatio ?: zoomMinRef
            val zMax = cameraRef?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: zoomMaxRef
            cameraRef?.cameraControl?.setZoomRatio(zoomRatio.coerceIn(zMin, zMax.coerceAtLeast(zMin)))
        }
    }

    DisposableEffect(lifecycleOwner) {
        disposed.set(false)
        onDispose {
            disposed.set(true)
            runCatching { cameraRef?.cameraControl?.enableTorch(false) }
            cameraRef = null
            runCatching {
                if (providerFuture.isDone) {
                    providerFuture.get().unbindAll()
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                keepScreenOn = true
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                    PreviewView.ImplementationMode.COMPATIBLE
                } else {
                    PreviewView.ImplementationMode.PERFORMANCE
                }
            }

            var pinchInProgress = false
            val scaleDetector = ScaleGestureDetector(
                ctx,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                        pinchInProgress = true
                        return true
                    }

                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val cam = cameraRef ?: return false
                        val zState = cam.cameraInfo.zoomState.value
                        val cur = zState?.zoomRatio ?: zoomRatio
                        val minZ = zState?.minZoomRatio ?: zoomMinRef
                        val maxZ = zState?.maxZoomRatio ?: zoomMaxRef
                        val next = (cur * detector.scaleFactor).coerceIn(minZ, maxZ.coerceAtLeast(minZ))
                        runCatching {
                            cam.cameraControl.setZoomRatio(next)
                            onZoomRatioChanged(next)
                        }
                        return true
                    }
                }
            )

            previewView.setOnTouchListener { view, event ->
                scaleDetector.onTouchEvent(event)
                if (event.pointerCount <= 1 && !scaleDetector.isInProgress && !pinchInProgress &&
                    event.actionMasked == MotionEvent.ACTION_UP
                ) {
                    runCatching {
                        val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cameraRef?.cameraControl?.startFocusAndMetering(action)
                    }
                    view.performClick()
                }
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    pinchInProgress = false
                }
                true
            }

            providerFuture.addListener({
                if (disposed.get()) return@addListener
                runCatching {
                    val provider = providerFuture.get()
                    val camSelector = when {
                        provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                        provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> {
                            onCameraError("Nenhuma câmera disponível neste dispositivo.")
                            return@addListener
                        }
                    }

                    val previewResSelector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .build()

                    val preview = Preview.Builder()
                        .setResolutionSelector(previewResSelector)
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    provider.unbindAll()
                    val cam = provider.bindToLifecycle(
                        lifecycleOwner,
                        camSelector,
                        preview,
                        imageCapture
                    )
                    cameraRef = cam
                    val zState = cam.cameraInfo.zoomState.value
                    zoomMinRef = zState?.minZoomRatio ?: 1f
                    zoomMaxRef = zState?.maxZoomRatio ?: 4f
                    onMaxZoomAvailable(zoomMaxRef)
                    onTorchSupported(cam.cameraInfo.hasFlashUnit())
                }.onFailure {
                    onCameraError("Não foi possível iniciar a câmera neste aparelho.")
                }
            }, mainExecutor)

            previewView
        }
    )
}
