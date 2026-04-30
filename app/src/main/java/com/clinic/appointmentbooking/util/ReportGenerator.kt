package com.clinic.appointmentbooking.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import com.clinic.appointmentbooking.model.Appointment
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Type of report to generate. */
enum class ReportType { TODAY, MONTH }

/**
 * Generates a professional PDF report for either today's or this month's appointments
 * and saves it to the device Downloads folder.
 *
 * Returns the saved [File] (on API ≤ 28) or a content-scheme Uri string (on API 29+)
 * wrapped in a [Result].
 */
object ReportGenerator {

    private const val PAGE_WIDTH  = 595   // A4 pt width  (72 dpi equivalent)
    private const val PAGE_HEIGHT = 842   // A4 pt height
    private const val MARGIN      = 48f

    // ─── Colours ────────────────────────────────────────────────────────────
    private val COLOR_PRIMARY      = Color.parseColor("#2563EB")
    private val COLOR_PRIMARY_DARK = Color.parseColor("#1D4ED8")
    private val COLOR_SUCCESS      = Color.parseColor("#22C55E")
    private val COLOR_WARNING      = Color.parseColor("#F59E0B")
    private val COLOR_BG_LIGHT     = Color.parseColor("#F8FAFC")
    private val COLOR_TEXT_PRIMARY = Color.parseColor("#0F172A")
    private val COLOR_TEXT_HINT    = Color.parseColor("#94A3B8")
    private val COLOR_DIVIDER      = Color.parseColor("#E2E8F0")
    private val COLOR_WHITE        = Color.WHITE

    // ─── Reusable Paints ────────────────────────────────────────────────────
    private fun boldPaint(size: Float, color: Int = COLOR_TEXT_PRIMARY) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            isFakeBoldText = true
        }

    private fun regularPaint(size: Float, color: Int = COLOR_TEXT_PRIMARY) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
        }

    private fun fillPaint(color: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

    // ─── Public entry point ──────────────────────────────────────────────────

    /**
     * Generates the PDF and saves it.
     * @return a [Result] wrapping the output [File] or an [Exception].
     */
    fun generate(
        context: Context,
        appointments: List<Appointment>,
        type: ReportType
    ): Result<File> = runCatching {

        val now        = Calendar.getInstance()
        val todayFmt   = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val monthFmt   = SimpleDateFormat("MM/yyyy",    Locale.getDefault())
        val today      = todayFmt.format(now.time)
        val thisMonth  = monthFmt.format(now.time)

        // ── Filter appointments ──────────────────────────────────────────
        val filtered = when (type) {
            ReportType.TODAY  -> appointments.filter { it.date == today }
            ReportType.MONTH  -> appointments.filter {
                it.date.length >= 10 && it.date.substring(3) == thisMonth
            }
        }

        // ── Stats ────────────────────────────────────────────────────────
        val total     = filtered.size
        val completed = filtered.count { it.status.equals("completed", ignoreCase = true) }
        val pending   = total - completed

        // ── Upcoming visits (future nextVisitDate) ───────────────────────
        val upcoming = filtered.filter { appt ->
            appt.nextVisitDate.isNotBlank() &&
            runCatching { todayFmt.parse(appt.nextVisitDate) }.getOrNull()?.after(now.time) == true
        }.sortedBy { it.nextVisitDate }

        // ── Build PDF ────────────────────────────────────────────────────
        val pdfDoc = PdfDocument()
        var pageNumber    = 1
        var pageInfo      = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page          = pdfDoc.startPage(pageInfo)
        var canvas        = page.canvas
        var y             = drawHeader(canvas, type, now)

        y = drawSummarySection(canvas, y, total, completed, pending)

        // section title: Patient List
        y += 20f
        y = drawSectionTitle(canvas, y, "PATIENT LIST")

        filtered.forEachIndexed { index, appt ->
            // Need a new page?
            if (y > PAGE_HEIGHT - 120f) {
                pdfDoc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page     = pdfDoc.startPage(pageInfo)
                canvas   = page.canvas
                y        = MARGIN
            }
            y = drawPatientRow(canvas, y, index + 1, appt)
        }

        // Upcoming visits section (if any)
        if (upcoming.isNotEmpty()) {
            if (y > PAGE_HEIGHT - 200f) {
                pdfDoc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page     = pdfDoc.startPage(pageInfo)
                canvas   = page.canvas
                y        = MARGIN
            }
            y += 24f
            y = drawSectionTitle(canvas, y, "UPCOMING FOLLOW-UP VISITS")
            upcoming.forEachIndexed { index, appt ->
                if (y > PAGE_HEIGHT - 80f) {
                    pdfDoc.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page     = pdfDoc.startPage(pageInfo)
                    canvas   = page.canvas
                    y        = MARGIN
                }
                y = drawUpcomingRow(canvas, y, index + 1, appt)
            }
        }

        // Footer on last page
        drawFooter(canvas, pageNumber)
        pdfDoc.finishPage(page)

        // ── Save to Downloads ────────────────────────────────────────────
        val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(now.time)
        val fileName = when (type) {
            ReportType.TODAY -> "report-today-$ts.pdf"
            ReportType.MONTH -> "report-month-$ts.pdf"
        }

        val outputFile = saveToDownloads(context, pdfDoc, fileName)
        pdfDoc.close()
        outputFile
    }

    // ─── Save helpers ────────────────────────────────────────────────────────

    private fun saveToDownloads(
        context: Context,
        pdfDoc: PdfDocument,
        fileName: String
    ): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+ — MediaStore (no WRITE permission required)
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("MediaStore insert returned null")

            resolver.openOutputStream(uri)?.use { out ->
                pdfDoc.writeTo(out)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            // Return a File pointing to the same location (for FileProvider sharing)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(downloadsDir, fileName)
        } else {
            // API 24–28 — direct file write
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val outputFile = File(downloadsDir, fileName)
            FileOutputStream(outputFile).use { out -> pdfDoc.writeTo(out) }
            outputFile
        }
    }

    // ─── Drawing helpers ─────────────────────────────────────────────────────

    /** Returns the new Y position after drawing the header. */
    private fun drawHeader(canvas: Canvas, type: ReportType, now: Calendar): Float {
        val contentWidth = PAGE_WIDTH - MARGIN * 2

        // Blue header band
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 130f, fillPaint(COLOR_PRIMARY))

        // Hospital name
        val namePaint = boldPaint(18f, COLOR_WHITE)
        canvas.drawText("Bhagirati Orthopedic Hospital", MARGIN, 42f, namePaint)

        // Report title
        val titlePaint = regularPaint(11f, Color.parseColor("#BFDBFE"))
        canvas.drawText("Doctor Report  •  ${if (type == ReportType.TODAY) "Today" else "This Month"}", MARGIN, 62f, titlePaint)

        // Generation timestamp
        val tsFmt   = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val tsPaint = regularPaint(9f, Color.parseColor("#93C5FD"))
        canvas.drawText("Generated: ${tsFmt.format(now.time)}", MARGIN, 80f, tsPaint)

        // Report type badge (right-aligned)
        val badgeText  = if (type == ReportType.TODAY) "TODAY" else "THIS MONTH"
        val badgePaint = boldPaint(10f, COLOR_WHITE)
        val badgeW     = badgePaint.measureText(badgeText) + 20f
        val badgeRect  = RectF(PAGE_WIDTH - MARGIN - badgeW, 44f, PAGE_WIDTH - MARGIN, 66f)
        canvas.drawRoundRect(badgeRect, 6f, 6f, fillPaint(COLOR_PRIMARY_DARK))
        canvas.drawText(badgeText, badgeRect.left + 10f, 59f, badgePaint)

        return 150f  // start of content below header
    }

    /** Draws the 3-stat summary band. Returns next Y. */
    private fun drawSummarySection(
        canvas: Canvas,
        startY: Float,
        total: Int,
        completed: Int,
        pending: Int
    ): Float {
        val contentWidth = PAGE_WIDTH - MARGIN * 2
        val cardW        = contentWidth / 3f - 8f
        val cardH        = 72f
        val cardY        = startY

        val labels  = listOf("Total Patients", "Completed", "Pending")
        val values  = listOf(total, completed, pending)
        val colors  = listOf(COLOR_PRIMARY, COLOR_SUCCESS, COLOR_WARNING)
        val bgColors = listOf(
            Color.parseColor("#DBEAFE"),
            Color.parseColor("#DCFCE7"),
            Color.parseColor("#FEF3C7")
        )

        values.forEachIndexed { i, value ->
            val left = MARGIN + i * (cardW + 12f)
            val rect = RectF(left, cardY, left + cardW, cardY + cardH)
            canvas.drawRoundRect(rect, 10f, 10f, fillPaint(bgColors[i]))

            // Big number
            val numPaint = boldPaint(26f, colors[i])
            val numStr   = value.toString()
            val numX     = left + (cardW - numPaint.measureText(numStr)) / 2f
            canvas.drawText(numStr, numX, cardY + 38f, numPaint)

            // Label
            val lblPaint = regularPaint(8.5f, COLOR_TEXT_HINT)
            val lblX     = left + (cardW - lblPaint.measureText(labels[i])) / 2f
            canvas.drawText(labels[i], lblX, cardY + 56f, lblPaint)
        }

        return cardY + cardH + 24f
    }

    /** Draws a section title with underline. Returns next Y. */
    private fun drawSectionTitle(canvas: Canvas, y: Float, title: String): Float {
        val paint = boldPaint(10f, COLOR_TEXT_HINT)
        canvas.drawText(title, MARGIN, y, paint)
        val lineY = y + 6f
        canvas.drawLine(MARGIN, lineY, PAGE_WIDTH - MARGIN, lineY, fillPaint(COLOR_DIVIDER).also {
            it.strokeWidth = 1f; it.style = Paint.Style.STROKE
        })
        return lineY + 14f
    }

    /** Draws one patient appointment row. Returns next Y. */
    private fun drawPatientRow(canvas: Canvas, y: Float, serial: Int, appt: Appointment): Float {
        val instrList = appt.instructionList()
        val rowH      = if (instrList.isNotEmpty()) 94f else 74f
        val rowRect   = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowH - 6f)
        canvas.drawRoundRect(rowRect, 8f, 8f, fillPaint(COLOR_BG_LIGHT))

        val innerY   = y + 14f
        val numPaint = boldPaint(11f, COLOR_PRIMARY)
        canvas.drawText("$serial.", MARGIN + 12f, innerY, numPaint)

        // Patient name
        val namePaint = boldPaint(11f, COLOR_TEXT_PRIMARY)
        canvas.drawText(appt.patientName.ifBlank { "Unknown" }, MARGIN + 34f, innerY, namePaint)

        // Time
        val timeLbl  = regularPaint(9f, COLOR_TEXT_HINT)
        val timeFmt  = "Time: ${appt.time.ifBlank { "—" }}"
        canvas.drawText(timeFmt, MARGIN + 34f, innerY + 17f, timeLbl)

        // Status chip
        val isCompleted = appt.status.equals("completed", ignoreCase = true)
        val statusText  = if (isCompleted) "Completed" else "Pending"
        val statusColor = if (isCompleted) COLOR_SUCCESS else COLOR_WARNING
        val statusBg    = if (isCompleted) Color.parseColor("#DCFCE7") else Color.parseColor("#FEF3C7")
        val statusPaint = boldPaint(8.5f, statusColor)
        val chipW       = statusPaint.measureText(statusText) + 14f
        val chipRect    = RectF(MARGIN + 34f, innerY + 22f, MARGIN + 34f + chipW, innerY + 37f)
        canvas.drawRoundRect(chipRect, 5f, 5f, fillPaint(statusBg))
        canvas.drawText(statusText, chipRect.left + 7f, innerY + 33f, statusPaint)

        // ── Instructions ─────────────────────────────────────────────────
        if (instrList.isNotEmpty()) {
            val instrLabel     = "Instructions: "
            val instrLblPaint  = boldPaint(8.5f, COLOR_TEXT_HINT)
            val instrValPaint  = regularPaint(8.5f, COLOR_PRIMARY)
            val instrText      = instrList.joinToString("  •  ")
            canvas.drawText(instrLabel, MARGIN + 34f, innerY + 53f, instrLblPaint)
            canvas.drawText(instrText,  MARGIN + 34f + instrLblPaint.measureText(instrLabel), innerY + 53f, instrValPaint)
        }

        // Next visit
        val visitText      = formatNextVisit(appt.nextVisitDate)
        val visitPaint     = regularPaint(9f, COLOR_TEXT_HINT)
        val visitLabel     = "Next Visit: "
        val visitLblPaint  = boldPaint(9f, COLOR_TEXT_HINT)
        val visitX         = PAGE_WIDTH - MARGIN - visitPaint.measureText(visitText) - 12f
        canvas.drawText(visitLabel, visitX - visitLblPaint.measureText(visitLabel), innerY + 17f, visitLblPaint)
        canvas.drawText(visitText,  visitX, innerY + 17f, visitPaint)

        return y + rowH
    }

    /** Draws one upcoming-visit row. Returns next Y. */
    private fun drawUpcomingRow(canvas: Canvas, y: Float, serial: Int, appt: Appointment): Float {
        val rowH     = 46f
        val rowRect  = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowH - 4f)
        canvas.drawRoundRect(rowRect, 8f, 8f, fillPaint(Color.parseColor("#EFF6FF")))

        val innerY   = y + 16f
        val numPaint = boldPaint(10f, COLOR_PRIMARY)
        canvas.drawText("$serial.", MARGIN + 12f, innerY, numPaint)

        val namePaint = boldPaint(10f, COLOR_TEXT_PRIMARY)
        canvas.drawText(appt.patientName.ifBlank { "Unknown" }, MARGIN + 34f, innerY, namePaint)

        val datePaint = regularPaint(10f, COLOR_PRIMARY)
        val dateStr   = appt.nextVisitDate
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - datePaint.measureText(dateStr) - 12f, innerY, datePaint)

        return y + rowH
    }

    /** Draws a footer with page number. */
    private fun drawFooter(canvas: Canvas, pageNum: Int) {
        val paint = regularPaint(8f, COLOR_TEXT_HINT)
        val text  = "Page $pageNum  •  ClinicBook — Bhagirati Orthopedic Hospital"
        val x     = (PAGE_WIDTH - paint.measureText(text)) / 2f
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 36f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 36f,
            fillPaint(COLOR_DIVIDER).also { it.strokeWidth = 1f; it.style = Paint.Style.STROKE })
        canvas.drawText(text, x, PAGE_HEIGHT - 20f, paint)
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    private fun formatNextVisit(raw: String): String {
        if (raw.isBlank()) return "Not Scheduled"
        return raw  // already stored as dd/MM/yyyy
    }
}
