package com.example.depthclock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.text.format.DateFormat
import android.view.View
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.sin

const val PREFS = "depth"
const val KEY_SIZE = "size"
const val KEY_Y = "ypos"
const val KEY_COLOR = "color"
const val KEY_FONT = "font"
const val KEY_MOTION = "motion"
const val BG_FILE = "bg.jpg"
const val FG_FILE = "fg.png"
const val FONT_COUNT = 8

fun fontFor(i: Int): Typeface = when (i) {
    0 -> Typeface.create("sans-serif-black", Typeface.NORMAL)
    1 -> Typeface.create("sans-serif-medium", Typeface.BOLD)
    2 -> Typeface.create("sans-serif-thin", Typeface.NORMAL)
    3 -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
    4 -> Typeface.create("serif", Typeface.BOLD)
    5 -> Typeface.create("monospace", Typeface.BOLD)
    6 -> Typeface.create("casual", Typeface.NORMAL)
    else -> Typeface.create("cursive", Typeface.BOLD)
}

class DepthRenderer(private val ctx: Context) {
    private var bg: Bitmap? = null
    private var fg: Bitmap? = null
    private var stamp = -1L
    private var fontIdx = -1
    private val fonts = Array(FONT_COUNT) { fontFor(it) }
    private val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val matrix = Matrix()
    private val hourFmt24 = SimpleDateFormat("HH", Locale.US)
    private val hourFmt12 = SimpleDateFormat("hh", Locale.US)
    private val minFmt = SimpleDateFormat("mm", Locale.US)

    private fun reload() {
        val bf = File(ctx.filesDir, BG_FILE)
        val ff = File(ctx.filesDir, FG_FILE)
        val s = max(bf.lastModified(), ff.lastModified())
        if (s == stamp) return
        stamp = s
        bg = if (bf.exists()) BitmapFactory.decodeFile(bf.path) else null
        fg = if (ff.exists()) BitmapFactory.decodeFile(ff.path) else null
    }

    fun draw(c: Canvas, w: Int, h: Int, ox: Float, oy: Float) {
        reload()
        c.drawColor(Color.BLACK)
        val zoom = if (prefs.getBoolean(KEY_MOTION, false)) 1.15f else 1.0f
        val b = bg
        if (b != null) layer(c, b, w, h, ox * 0.012f * w, oy * 0.008f * h, zoom)
        drawClock(c, w, h, ox * 0.03f * w, oy * 0.02f * h)
        val f = fg
        if (f != null) layer(c, f, w, h, ox * 0.055f * w, oy * 0.035f * h, zoom)
    }

    private fun layer(c: Canvas, bmp: Bitmap, w: Int, h: Int, sx: Float, sy: Float, zoom: Float) {
        val s = max(w.toFloat() / bmp.width, h.toFloat() / bmp.height) * zoom
        matrix.reset()
        matrix.postScale(s, s)
        matrix.postTranslate((w - bmp.width * s) / 2f + sx, (h - bmp.height * s) / 2f + sy)
        c.drawBitmap(bmp, matrix, bitmapPaint)
    }

    private fun drawClock(c: Canvas, w: Int, h: Int, sx: Float, sy: Float) {
        val size = prefs.getInt(KEY_SIZE, 50)
        val ypos = prefs.getInt(KEY_Y, 30)
        val fi = prefs.getInt(KEY_FONT, 0).coerceIn(0, FONT_COUNT - 1)
        if (fi != fontIdx) {
            fontIdx = fi
            textPaint.typeface = fonts[fi]
        }
        textPaint.color = prefs.getInt(KEY_COLOR, Color.WHITE)
        val ts = w * (0.25f + size / 100f * 0.45f)
        textPaint.textSize = ts
        val now = Date()
        val hours = if (DateFormat.is24HourFormat(ctx)) hourFmt24.format(now) else hourFmt12.format(now)
        val mins = minFmt.format(now)
        val x = w / 2f + sx
        val y0 = h * (0.08f + ypos / 100f * 0.45f) + ts * 0.8f + sy
        c.drawText(hours, x, y0, textPaint)
        c.drawText(mins, x, y0 + ts * 0.95f, textPaint)
    }
}

class PreviewView(context: Context) : View(context) {
    private val renderer = DepthRenderer(context.applicationContext)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val start = System.currentTimeMillis()

    override fun onDraw(canvas: Canvas) {
        val motion = prefs.getBoolean(KEY_MOTION, false)
        val t = (System.currentTimeMillis() - start) / 1000f
        renderer.draw(canvas, width, height, if (motion) sin(t * 1.5f) * 0.8f else 0f, 0f)
        if (motion) postInvalidateOnAnimation() else postInvalidateDelayed(120)
    }
}
