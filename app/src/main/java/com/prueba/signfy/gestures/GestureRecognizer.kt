package com.prueba.signfy.gestures

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

object GestureRecognizer {


    fun recognize(landmarks: List<NormalizedLandmark>): String {
        if (landmarks.size < 21) return "Desconocido"


        val thumbExtended = isFingerExtended(landmarks, 1, 4)    // Pulgar
        val indexExtended = isFingerExtended(landmarks, 5, 8)   // Índice
        val middleExtended = isFingerExtended(landmarks, 9, 12) // Medio
        val ringExtended = isFingerExtended(landmarks, 13, 16)  // Anular
        val pinkyExtended = isFingerExtended(landmarks, 17, 20) // Meñique

        return when {

            indexExtended && !middleExtended && !ringExtended && !pinkyExtended -> "👉 Apuntar"

            indexExtended && middleExtended && ringExtended && pinkyExtended && thumbExtended -> "✋ Mano abierta"

            !indexExtended && !middleExtended && !ringExtended && !pinkyExtended && !thumbExtended -> "✊ Puño"

            else -> "🤷‍♂️ Otro gesto"
        }
    }

    private fun isFingerExtended(
        landmarks: List<NormalizedLandmark>,
        base: Int,
        tip: Int
    ): Boolean {
        val baseY = landmarks[base].y()
        val tipY = landmarks[tip].y()
        return tipY < baseY
    }
}
