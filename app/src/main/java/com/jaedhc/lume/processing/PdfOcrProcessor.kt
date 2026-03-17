package com.jaedhc.lume.processing

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PdfOcrProcessor {

    /**
     * Converts every page of a PDF (or a single image) to a Bitmap,
     * runs ML Kit OCR on each, and returns the concatenated text of all pages.
     */
    suspend fun extractText(contentResolver: ContentResolver, uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val mimeType = contentResolver.getType(uri) ?: ""
            if (mimeType == "application/pdf") {
                extractFromPdf(contentResolver, uri)
            } else {
                extractFromImage(contentResolver, uri)
            }
        }
    }

    // ── PDF ────────────────────────────────────────────────────────────────

    private suspend fun extractFromPdf(contentResolver: ContentResolver, uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val pfd: ParcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                ?: error("Cannot open PDF file descriptor")

            pfd.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                val pageCount = renderer.pageCount
                Log.d("LumeNet", "PDF pages: $pageCount")

                val pageTexts = mutableListOf<String>()

                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)

                    // Render at 2x scale for better OCR accuracy
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )
                    // Fill white background (PDF pages may be transparent)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val pageText = runOcrOnBitmap(bitmap)
                    Log.d("LumeNet", "=== PAGE ${i + 1}/$pageCount ===\n$pageText")
                    pageTexts.add(pageText)
                    bitmap.recycle()
                }

                renderer.close()
                pageTexts.joinToString("\n\n--- NUEVA PÁGINA ---\n\n")
            }
        }
    }

    // ── Image ──────────────────────────────────────────────────────────────

    private suspend fun extractFromImage(contentResolver: ContentResolver, uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val bitmap: Bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            val text = runOcrOnBitmap(bitmap)
            bitmap.recycle()
            text
        }
    }

    // ── OCR shared helper ──────────────────────────────────────────────────

    private suspend fun runOcrOnBitmap(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
