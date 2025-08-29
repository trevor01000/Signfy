package com.prueba.signfy.ui.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class HandOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var hands: List<List<NormalizedLandmark>> = emptyList()
    private var mirrorX: Boolean = false
    private var rotation: Int = 0

    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 8f
    }

    fun setLandmarks(
        hands: List<List<NormalizedLandmark>>,
        mirrorX: Boolean,
        rotation: Int
    ) {
        this.hands = hands
        this.mirrorX = mirrorX
        this.rotation = rotation
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (hand in hands) {
            for (landmark in hand) {
                val (nx, ny) = transformPoint(
                    landmark.x(),
                    landmark.y(),
                    rotation,
                    mirrorX
                )

                val px = nx * width
                val py = (1 - ny) * height

                canvas.drawCircle(px, py, 10f, paint)
            }
        }
    }

    private fun transformPoint(
        xNorm: Float,
        yNorm: Float,
        rotation: Int,
        mirrorX: Boolean
    ): Pair<Float, Float> {
        var x = xNorm
        var y = yNorm

        when (rotation) {
            90 -> {
                val temp = x
                x = y
                y = 1 - temp
            }
            180 -> {
                x = 1 - x
                y = 1 - y
            }
            270 -> {
                val temp = x
                x = 1 - y
                y = temp
            }
        }

        if (mirrorX) {
            x = 1 - x
        }

        return Pair(x, y)
    }
}