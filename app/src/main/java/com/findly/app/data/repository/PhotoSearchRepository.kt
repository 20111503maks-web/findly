package com.findly.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.findly.app.data.remote.FindlyApi
import com.findly.core.model.Category
import com.findly.core.visual.ImagePixels
import com.findly.core.visual.LocalColorVisualSearchService
import com.findly.core.visual.VisualAnalysis
import com.findly.core.visual.VisualSearchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

/**
 * Photo search entry point.
 *  - REMOTE: uploads a downscaled JPEG to the Findly backend vision endpoint.
 *  - LOCAL_DEMO: real on-device colour analysis only; the user picks the product type.
 */
class PhotoSearchRepository(
    private val context: Context,
    private val api: FindlyApi?,
) {
    private val local = LocalColorVisualSearchService()

    val mode: VisualSearchMode get() = if (api != null) VisualSearchMode.REMOTE else VisualSearchMode.LOCAL_DEMO

    suspend fun analyze(uri: Uri): VisualAnalysis = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(uri, maxSide = if (api != null) 1024 else 96)
            ?: throw IllegalArgumentException("Cannot decode image")
        try {
            if (api != null) remote(api, bitmap) else local.analyze(bitmap.toPixels())
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun remote(api: FindlyApi, bitmap: Bitmap): VisualAnalysis {
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
        val part = MultipartBody.Part.createFormData("image", "photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
        val r = api.visualSearch(part)
        return VisualAnalysis(
            category = r.category?.let { c -> Category.entries.firstOrNull { it.name.equals(c, true) } },
            dominantColors = r.colors,
            labels = r.labels,
            suggestedQuery = r.query,
            confidence = r.confidence,
            mode = VisualSearchMode.REMOTE,
        )
    }

    private fun decodeScaled(uri: Uri, maxSide: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val scale = maxSide.toFloat() / maxOf(decoded.width, decoded.height)
        if (scale >= 1f) return decoded
        val scaled = Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
        if (scaled != decoded) decoded.recycle()
        return scaled
    }

    private fun Bitmap.toPixels(): ImagePixels {
        val argb = IntArray(width * height)
        getPixels(argb, 0, width, 0, 0, width, height)
        return ImagePixels(width, height, argb)
    }
}
