package com.example.nestblr.feature.owner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val MAX_EDGE_PX = 1600
private const val JPEG_QUALITY = 80

/**
 * Reads the picked image and writes a compressed JPEG (q80, longer edge ≤ 1600 px)
 * to cacheDir/photo_upload_<timestamp>.jpg.
 *
 * API 28+ uses ImageDecoder — handles HEIC, WebP, animated images natively.
 * Older devices fall back to BitmapFactory with two-pass downsampling.
 *
 * Throws [IOException] with the underlying cause's message on any failure so the
 * caller can surface real diagnostic info to the UI.
 */
suspend fun compressImage(context: Context, sourceUri: Uri): File = withContext(Dispatchers.IO) {
    val bitmap: Bitmap = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(context, sourceUri)
        } else {
            decodeWithBitmapFactory(context, sourceUri)
        }
    } catch (t: Throwable) {
        // Preserve the original cause — the message is the real diagnostic.
        throw IOException("Image decode failed: ${t.message}", t)
    }

    val outFile = File(context.cacheDir, "photo_upload_${System.currentTimeMillis()}.jpg")
    try {
        outFile.outputStream().use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                throw IOException("Bitmap.compress returned false (codec rejected bitmap)")
            }
        }
    } catch (t: Throwable) {
        outFile.delete()
        throw if (t is IOException) t
        else IOException("Couldn't write compressed photo: ${t.message}", t)
    } finally {
        bitmap.recycle()
    }
    outFile
}

@RequiresApi(Build.VERSION_CODES.P)
private fun decodeWithImageDecoder(context: Context, sourceUri: Uri): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        // Hardware bitmaps can't be compress()'d on some OEMs — force software alloc.
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = false

        val w = info.size.width
        val h = info.size.height
        val longest = maxOf(w, h)
        if (longest > MAX_EDGE_PX) {
            val scale = MAX_EDGE_PX.toFloat() / longest
            decoder.setTargetSize(
                (w * scale).toInt().coerceAtLeast(1),
                (h * scale).toInt().coerceAtLeast(1)
            )
        }
    }
}

private fun decodeWithBitmapFactory(context: Context, sourceUri: Uri): Bitmap {
    val resolver = context.contentResolver

    // Pass 1 — bounds only. decodeStream(... inJustDecodeBounds=true) returns
    // null by design; check stream open separately from the (always-null) result.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(sourceUri)
        ?: throw IOException("ContentResolver returned null stream for $sourceUri")
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IOException(
            "Selected file isn't a readable image (no dimensions; mime=${bounds.outMimeType})"
        )
    }

    var sample = 1
    while (
        bounds.outWidth / sample > MAX_EDGE_PX * 2 ||
        bounds.outHeight / sample > MAX_EDGE_PX * 2
    ) {
        sample *= 2
    }

    // Pass 2 — actual decode at downsampled resolution.
    val decodeStream = resolver.openInputStream(sourceUri)
        ?: throw IOException("ContentResolver returned null stream for $sourceUri (pass 2)")
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded: Bitmap = decodeStream.use { stream ->
        BitmapFactory.decodeStream(stream, null, opts)
    } ?: throw IOException("BitmapFactory returned null bitmap (likely unsupported format)")

    val longest = maxOf(decoded.width, decoded.height)
    return if (longest > MAX_EDGE_PX) {
        val ratio = MAX_EDGE_PX.toFloat() / longest
        val w = (decoded.width * ratio).toInt().coerceAtLeast(1)
        val h = (decoded.height * ratio).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(decoded, w, h, true).also {
            if (it !== decoded) decoded.recycle()
        }
    } else decoded
}
