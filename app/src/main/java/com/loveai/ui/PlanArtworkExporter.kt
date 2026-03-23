package com.loveai.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import com.loveai.model.EffectType
import com.loveai.model.LovePlan
import com.loveai.model.PlanCover
import com.loveai.model.PlanTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PlanArtworkExporter {

    data class ExportedArtwork(
        val file: File,
        val title: String
    )

    fun exportPoster(context: Context, plan: LovePlan, songName: String?): ExportedArtwork {
        val bitmap = renderPoster(plan, songName)
        val safeName = plan.name.ifBlank { "loveai_plan" }
            .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]+"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val baseDir = context.getExternalFilesDir("exports") ?: context.filesDir
        val dir = File(baseDir, "posters").apply { mkdirs() }
        val file = File(dir, "${safeName}_${timestamp}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return ExportedArtwork(file = file, title = "${plan.name}\u6d77\u62a5")
    }

    fun buildShareUri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun renderPoster(plan: LovePlan, songName: String?): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cover = PlanCover.fromKey(plan.coverKey)
        val theme = PlanTheme.fromKey(plan.themeKey)
        val colors = when {
            cover != null -> intArrayOf(
                Color.parseColor(cover.startColor),
                Color.parseColor(cover.endColor)
            )
            theme == PlanTheme.CONFESSION -> intArrayOf(Color.parseColor("#FF6F91"), Color.parseColor("#FF9671"))
            theme == PlanTheme.ANNIVERSARY -> intArrayOf(Color.parseColor("#6A67CE"), Color.parseColor("#C06C84"))
            theme == PlanTheme.BIRTHDAY -> intArrayOf(Color.parseColor("#F9C74F"), Color.parseColor("#F3722C"))
            theme == PlanTheme.LONG_DISTANCE -> intArrayOf(Color.parseColor("#277DA1"), Color.parseColor("#577590"))
            else -> intArrayOf(Color.parseColor("#5C4B8A"), Color.parseColor("#A55C9A"))
        }

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                colors,
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawCircle(width * 0.78f, height * 0.16f, 220f, glowPaint)
        canvas.drawCircle(width * 0.16f, height * 0.3f, 160f, glowPaint)

        val panel = RectF(72f, 92f, width - 72f, height - 92f)
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(44, 255, 255, 255)
        }
        canvas.drawRoundRect(panel, 42f, 42f, panelPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 62f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(228, 255, 255, 255)
            textSize = 30f
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 255, 255)
            textSize = 24f
        }
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(56, 0, 0, 0)
        }
        val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            isFakeBoldText = true
        }

        var cursorY = 188f
        drawChip(canvas, 108f, cursorY, plan.name.ifBlank { "\u6211\u7684\u65b9\u6848" }, chipPaint, chipTextPaint)
        cursorY += 90f

        drawMultilineText(
            canvas = canvas,
            text = plan.title.ifBlank { "\u7ed9\u4f60\u7684\u6d6a\u6f2b\u7247\u6bb5" },
            startX = 108f,
            startY = cursorY,
            maxWidth = width - 216f,
            lineHeight = 78f,
            paint = titlePaint
        ).also { cursorY = it + 24f }

        if (plan.subtitle.isNotBlank()) {
            drawMultilineText(
                canvas = canvas,
                text = plan.subtitle,
                startX = 108f,
                startY = cursorY,
                maxWidth = width - 216f,
                lineHeight = 42f,
                paint = subPaint
            ).also { cursorY = it + 34f }
        }

        val meta = buildString {
            append("\u4e3b\u9898\uff1a")
            append(PlanTheme.fromKey(plan.themeKey)?.label ?: "\u81ea\u5b9a\u4e49")
            append("    \u7279\u6548\uff1a")
            append(plan.effectTypes.size)
            append(" \u4e2a")
            if (!songName.isNullOrBlank()) {
                append("    \u97f3\u4e50\uff1a")
                append(songName)
            }
        }
        canvas.drawText(meta, 108f, cursorY, smallPaint)
        cursorY += 70f

        if (plan.tags.isNotEmpty()) {
            drawMultilineText(
                canvas = canvas,
                text = plan.tags.joinToString("  ") { "#$it" },
                startX = 108f,
                startY = cursorY,
                maxWidth = width - 216f,
                lineHeight = 36f,
                paint = smallPaint
            ).also { cursorY = it + 36f }
        }

        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
        }
        canvas.drawText("\u52a8\u6001\u9875\u9762", 108f, cursorY, sectionPaint)
        cursorY += 26f

        plan.effectTypes.forEachIndexed { index, type ->
            val itemTop = cursorY + index * 94f
            val itemRect = RectF(108f, itemTop, width - 108f, itemTop + 74f)
            canvas.drawRoundRect(itemRect, 26f, 26f, chipPaint)

            val pageLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 24f
                isFakeBoldText = true
            }
            val pageTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 255, 255, 255)
                textSize = 22f
            }
            canvas.drawText("${index + 1}. ${effectLabel(type)}", 132f, itemTop + 30f, pageLabel)
            val pageText = plan.pageTexts.getOrNull(index)
            val summaryText = pageText?.title?.takeIf { it.isNotBlank() } ?: plan.title
            canvas.drawText(summaryText.take(24), 132f, itemTop + 58f, pageTextPaint)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = 24f
        }
        canvas.drawText("LoveAI \u00b7 ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}", 108f, height - 128f, footerPaint)
        return bitmap
    }

    private fun drawChip(
        canvas: Canvas,
        startX: Float,
        baselineY: Float,
        text: String,
        chipPaint: Paint,
        textPaint: Paint
    ) {
        val width = textPaint.measureText(text) + 52f
        val rect = RectF(startX, baselineY - 34f, startX + width, baselineY + 14f)
        canvas.drawRoundRect(rect, 24f, 24f, chipPaint)
        canvas.drawText(text, startX + 26f, baselineY, textPaint)
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        startX: Float,
        startY: Float,
        maxWidth: Float,
        lineHeight: Float,
        paint: Paint
    ): Float {
        val lines = mutableListOf<String>()
        var current = ""
        text.forEach { char ->
            val candidate = current + char
            if (paint.measureText(candidate) > maxWidth && current.isNotBlank()) {
                lines += current
                current = char.toString()
            } else {
                current = candidate
            }
        }
        if (current.isNotBlank()) lines += current

        var y = startY
        lines.forEach { line ->
            canvas.drawText(line, startX, y, paint)
            y += lineHeight
        }
        return y
    }

    private fun effectLabel(type: EffectType): String {
        return when (type) {
            EffectType.HEART_RAIN -> "\u7231\u5fc3\u96e8"
            EffectType.FIREWORK -> "\u70df\u82b1"
            EffectType.STARRY_SKY -> "\u661f\u7a7a"
            EffectType.PETAL_FALL -> "\u82b1\u74e3\u96e8"
            EffectType.BUBBLE_FLOAT -> "\u6ce1\u6ce1\u6f02\u6d6e"
            EffectType.TYPEWRITER -> "\u6253\u5b57\u673a"
            EffectType.HEART_PULSE -> "\u5fc3\u8df3"
            EffectType.RIPPLE -> "\u6d9f\u6f2a"
            EffectType.SNOW_FALL -> "\u96ea\u82b1"
            EffectType.METEOR_SHOWER -> "\u6d41\u661f\u96e8"
            EffectType.BUTTERFLY -> "\u8774\u8776"
            EffectType.AURORA -> "\u6781\u5149"
        }
    }
}
