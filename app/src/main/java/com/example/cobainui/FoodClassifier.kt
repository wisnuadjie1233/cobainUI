package com.example.cobainui

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.MappedByteBuffer

class FoodClassifier(val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = listOf()
    private val inputImageSize = 224 // Teachable Machine standar pakai 224x224

    init {
        try {
            // 1. Load model.tflite
            val modelBuffer: MappedByteBuffer = FileUtil.loadMappedFile(context, "model.tflite")
            interpreter = Interpreter(modelBuffer)

            // 2. Load labels.txt
            val labelsRaw = FileUtil.loadLabels(context, "labels.txt")
            // Bersihkan format label (misal "1 baso" -> "baso")
            labels = labelsRaw.map { it.replace(Regex("^[0-9]+\\s*"), "").trim() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun classify(bitmap: Bitmap): String {
        val currentInterpreter = interpreter ?: return "Model Error"

        try {
            // --- PERBAIKAN NORMALISASI (Sesuai Teachable Machine) ---
            // Teachable Machine menggunakan range -1 sampai 1
            // Rumus: (pixel - 127.5) / 127.5
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageSize, inputImageSize, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(127.5f, 127.5f)) 
                .build()

            // 2. Load Bitmap ke TensorImage
            var tensorImage = TensorImage(currentInterpreter.getInputTensor(0).dataType())
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // 3. Siapkan wadah output
            val outputBuffer = Array(1) { FloatArray(labels.size) }

            // 4. Jalankan AI
            currentInterpreter.run(tensorImage.buffer, outputBuffer)

            // 5. Cari skor tertinggi
            val scores = outputBuffer[0]
            var maxIdx = -1
            var maxScore = -1f
            
            for (i in scores.indices) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i]
                    maxIdx = i
                }
            }

            // 6. Ambil nama makanan (Threshold 0.5 agar lebih akurat)
            return if (maxIdx != -1 && maxScore > 0.5f) {
                labels[maxIdx].replaceFirstChar { it.uppercase() }
            } else {
                "Tidak Dikenali"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error Klasifikasi"
        }
    }
}