package com.example.cobainui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class FoodClassifier(val context: Context) {

    private var classifier: ImageClassifier? = null
    private var labels: List<String> = listOf()

    init {
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setMaxResults(1)
            .setScoreThreshold(0.0f) // Set ke 0.0 agar menampilkan hasil apa pun yang paling mirip
            .build()

        try {
            // 1. Load Model
            classifier = ImageClassifier.createFromFileAndOptions(
                context, "model.tflite", options
            )
            // 2. Load Labels secara manual dari assets/labels.txt
            labels = FileUtil.loadLabels(context, "labels.txt")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun classify(bitmap: Bitmap): String {
        if (classifier == null) return "Model Error"

        val tensorImage = TensorImage.fromBitmap(bitmap)
        val results = classifier?.classify(tensorImage)

        val category = results?.firstOrNull()?.categories?.firstOrNull()

        // Log untuk melihat hasil deteksi di Logcat (Ketik 'FoodAI' di filter Logcat)
        category?.let {
            Log.d("FoodAI", "AI melihat: ${it.label} (Index: ${it.index}, Skor: ${it.score})")
        }

        return if (category != null) {
            // Jika label di model kosong, gunakan index untuk ambil dari labels.txt
            val labelRaw = if (category.label.isNullOrEmpty()) {
                if (category.index < labels.size) labels[category.index] else "Unknown Index"
            } else {
                category.label
            }
            
            // Bersihkan angka di depan (misal "1 baso" jadi "baso")
            labelRaw.replace(Regex("^[0-9]+\\s*"), "").replaceFirstChar { it.uppercase() }
        } else {
            "Tidak Dikenali"
        }
    }
}