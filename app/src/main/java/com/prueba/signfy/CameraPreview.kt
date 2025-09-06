package com.prueba.signfy

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.prueba.signfy.ui.theme.HandOverlayView
import com.prueba.signfy.gestures.GestureRecognizer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import androidx.compose.runtime.key

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    useFrontCamera: Boolean = true
) {
    val context = LocalContext.current

    key(useFrontCamera) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val container = FrameLayout(ctx)
                val previewView = PreviewView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val overlay = HandOverlayView(ctx)

                container.addView(previewView)
                container.addView(
                    overlay,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                startCamera(ctx, previewView, overlay, useFrontCamera)

                container
            }
        )
    }
}

private fun startCamera(
    context: Context,
    previewView: PreviewView,
    overlay: HandOverlayView,
    useFrontCamera: Boolean
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val executor = Executors.newSingleThreadExecutor()

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        var lastRotationDegrees = 0

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(2)
            .setResultListener { result: HandLandmarkerResult?, _: MPImage? ->
                result?.let {
                    Log.d("HandLandmarks", "Detectadas ${it.handednesses().size} manos")

                    val hands = it.landmarks()


                    overlay.post {
                        overlay.setLandmarks(
                            hands,
                            mirrorX = useFrontCamera,
                            rotation = lastRotationDegrees
                        )
                    }

                    val gestures = hands.map { hand -> GestureRecognizer.recognize(hand) }
                    val gestureText = if (gestures.isEmpty()) "" else gestures.joinToString("  |  ")
                    overlay.post {
                        overlay.setGesture(gestureText)
                    }

                    for (hand in hands) {
                        val gesture = GestureRecognizer.recognize(hand)
                        Log.d("Gesture", "Detectado: $gesture")
                    }
                }
            }
            .build()

        val handLandmarker = HandLandmarker.createFromOptions(context, options)

        imageAnalysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
            try {
                lastRotationDegrees = imageProxy.imageInfo.rotationDegrees

                val bitmap: Bitmap = imageProxyToBitmap(imageProxy)
                val mpImage: MPImage = BitmapImageBuilder(bitmap).build()

                handLandmarker.detectAsync(mpImage, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e("CameraPreview", "Error procesando frame", e)
            } finally {
                imageProxy.close()
            }
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as LifecycleOwner,
                if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error al bindear cámara", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val yBuffer: ByteBuffer = imageProxy.planes[0].buffer
    val uBuffer: ByteBuffer = imageProxy.planes[1].buffer
    val vBuffer: ByteBuffer = imageProxy.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)


    yBuffer.get(nv21, 0, ySize)

    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
