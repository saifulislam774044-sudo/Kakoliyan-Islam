package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream

object PdfAndExportHelper {

    private fun loadBengaliTypeface(context: Context): Typeface {
        return ResourcesCompat.getFont(context, R.font.noto_sans_bengali) ?: Typeface.DEFAULT
    }

    fun generatePdfDocument(
        context: Context,
        fileName: String,
        title: String,
        subtitle: String,
        headers: List<String>,
        rows: List<List<String>>,
        summaryLines: List<String> = emptyList(),
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "$fileName.pdf")

        val pdf = PdfDocument()
        val typeface = loadBengaliTypeface(context)
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.parseColor("#059669")
            textSize = 18f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.DKGRAY
            textSize = 11f
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.WHITE
            textSize = 10f
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.BLACK
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.8f
        }
        val bgPaint = Paint().apply {
            color = Color.parseColor("#059669")
        }

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas: Canvas = page.canvas
        var y = margin + 12f

        fun drawHeader() {
            canvas.drawText("${AppConstants.APP_NAME} — ${AppConstants.SHOP_LEDGER_NAME}", margin, y, headerPaint)
            y += 20f
            canvas.drawText(title, margin, y, cellPaint.apply { textSize = 13f; isFakeBoldText = true })
            cellPaint.textSize = 10f
            cellPaint.isFakeBoldText = false
            y += 16f
            canvas.drawText(subtitle, margin, y, subPaint)
            y += 18f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 14f

            if (headers.isNotEmpty()) {
                canvas.drawRect(margin, y - 12f, pageWidth - margin, y + 8f, bgPaint)
                val colWidth = (pageWidth - margin * 2) / headers.size
                headers.forEachIndexed { idx, h ->
                    canvas.drawText(h.take(18), margin + 4f + idx * colWidth, y + 2f, tableHeaderPaint)
                }
                y += 18f
            }
        }

        drawHeader()

        val colCount = maxOf(1, headers.size)
        val colWidth = (pageWidth - margin * 2) / colCount

        for (row in rows) {
            if (y > pageHeight - 70f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = margin + 12f
                drawHeader()
            }
            row.forEachIndexed { idx, cell ->
                if (idx < colCount) {
                    canvas.drawText(cell.take(22), margin + 4f + idx * colWidth, y, cellPaint)
                }
            }
            y += 6f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 14f
        }

        if (summaryLines.isNotEmpty()) {
            y += 8f
            for (line in summaryLines) {
                if (y > pageHeight - 40f) {
                    pdf.finishPage(page)
                    pageNum++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = margin + 18f
                }
                canvas.drawText(line, margin, y, cellPaint.apply { isFakeBoldText = true; textSize = 11f })
                cellPaint.isFakeBoldText = false
                cellPaint.textSize = 10f
                y += 16f
            }
        }

        pdf.finishPage(page)
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()
        return file
    }

    fun generateCsvExcelFile(
        context: Context,
        fileName: String,
        headers: List<String>,
        rows: List<List<String>>,
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "$fileName.csv")
        val sb = StringBuilder()
        // UTF-8 BOM so Microsoft Excel renders Bengali characters properly
        sb.append('\uFEFF')
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")
        for (row in rows) {
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }

    fun saveBitmapPng(context: Context, fileName: String, bitmap: Bitmap): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "$fileName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun printPdfFile(context: Context, file: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback,
            ) {
                try {
                    file.inputStream().use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }
}
