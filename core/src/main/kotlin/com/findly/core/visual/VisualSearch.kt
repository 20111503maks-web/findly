package com.findly.core.visual

import com.findly.core.model.Category
import com.findly.core.search.ColorDictionary
import kotlin.math.sqrt

/** Downscaled image pixels in ARGB format. The Android layer decodes and scales the photo. */
class ImagePixels(val width: Int, val height: Int, val argb: IntArray) {
    init {
        require(argb.size == width * height) { "Pixel buffer size mismatch" }
    }
}

enum class VisualSearchMode {
    /** Nothing configured. */
    DISABLED,
    /** On-device colour analysis only; product type must be chosen by the user. Clearly labelled in UI. */
    LOCAL_DEMO,
    /** Remote vision model via the Findly backend. */
    REMOTE,
}

data class VisualAnalysis(
    /** Detected category; null when the engine cannot recognize objects (local mode). */
    val category: Category?,
    val dominantColors: List<String>,
    /** Free-form labels from a vision model ("sneaker", "low-top", "leather"). Empty in local mode. */
    val labels: List<String>,
    /** Query to run, built only from what was actually detected. */
    val suggestedQuery: String,
    val confidence: Double?,
    val mode: VisualSearchMode,
)

/** Contract for photo search. A real implementation lives on the backend (vision model + embedding index). */
interface VisualSearchService {
    val mode: VisualSearchMode
    suspend fun analyze(image: ImagePixels): VisualAnalysis
}

/**
 * Honest local fallback: it really analyzes the photo's colours on-device, but it does NOT recognize
 * what the object is. The UI asks the user to pick the product type instead.
 */
class LocalColorVisualSearchService : VisualSearchService {
    override val mode = VisualSearchMode.LOCAL_DEMO

    override suspend fun analyze(image: ImagePixels): VisualAnalysis {
        val colors = DominantColorAnalyzer.dominantColorNames(image)
        return VisualAnalysis(
            category = null,
            dominantColors = colors,
            labels = emptyList(),
            suggestedQuery = colors.firstOrNull().orEmpty(),
            confidence = null,
            mode = mode,
        )
    }
}

object DominantColorAnalyzer {

    private val palette: Map<String, Triple<Int, Int, Int>> = mapOf(
        "white" to Triple(240, 240, 240),
        "black" to Triple(20, 20, 20),
        "grey" to Triple(128, 128, 128),
        "blue" to Triple(40, 70, 170),
        "red" to Triple(200, 30, 40),
        "green" to Triple(40, 140, 70),
        "beige" to Triple(220, 200, 165),
        "brown" to Triple(120, 75, 40),
        "pink" to Triple(235, 150, 180),
        "yellow" to Triple(240, 210, 50),
        "purple" to Triple(120, 60, 160),
    )

    /**
     * Returns up to [max] colour names covering at least [minShare] of the centre area of the image
     * (the centre is where the product usually is; edges are often background).
     */
    fun dominantColorNames(image: ImagePixels, max: Int = 2, minShare: Double = 0.18): List<String> {
        val counts = HashMap<String, Int>()
        val x0 = image.width / 5
        val x1 = image.width - x0
        val y0 = image.height / 5
        val y1 = image.height - y0
        var total = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val c = image.argb[y * image.width + x]
                if ((c ushr 24) < 128) continue
                val name = nearest((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)
                counts[name] = (counts[name] ?: 0) + 1
                total++
            }
        }
        if (total == 0) return emptyList()
        return counts.entries
            .filter { it.value.toDouble() / total >= minShare }
            .sortedByDescending { it.value }
            .take(max)
            .map { it.key }
            .filter { it in ColorDictionary.canonical }
    }

    fun nearest(r: Int, g: Int, b: Int): String =
        palette.minBy { (_, c) ->
            val dr = (r - c.first).toDouble()
            val dg = (g - c.second).toDouble()
            val db = (b - c.third).toDouble()
            sqrt(0.3 * dr * dr + 0.59 * dg * dg + 0.11 * db * db)
        }.key
}
