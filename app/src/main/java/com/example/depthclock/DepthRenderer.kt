package com.example.depthclock

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.format.DateFormat
import android.view.View
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

const val PREFS = "depth"
const val KEY_MOTION = "motion"
const val BG_FILE = "bg.jpg"
const val FG_FILE = "fg.png"
const val FONT_COUNT = 8

val FONT_NAMES = listOf("Medium", "Light", "Condensed", "Serif", "Mono", "Black", "Casual", "Script")
val DATE_FMTS = arrayOf("EEEE d MMMM", "EEE d MMM", "yyyy/MM/dd", "d MMMM")

/** Default value of every setting. Anything not stored in prefs falls back to this. */
val DEFAULTS: Map<String, Any> = mapOf<String, Any>(
    // clock
    "clk_on" to true,
    "clk_hf" to 0,
    "clk_layout" to 0,
    "clk_font" to 0,
    "clk_size" to 55,
    "clk_ls" to 10,
    "clk_lh" to 50,
    "clk_c1" to Color.WHITE,
    "clk_c2" to 0xFFFFE066.toInt(),
    "clk_op" to 100,
    "clk_x" to 50,
    "clk_y" to 25,
    "clk_sh" to false,
    "clk_shs" to 50,
    "clk_ow" to 0,
    "clk_oc" to Color.BLACK,
    "clk_rot" to 45,
    "clk_str" to 100,
    // date
    "dt_on" to true,
    "dt_fmt" to 0,
    "dt_up" to false,
    "dt_font" to 1,
    "dt_size" to 40,
    "dt_ls" to 30,
    "dt_c" to Color.WHITE,
    "dt_pos" to 0,
    "dt_gap" to 50,
    // free text
    "tx_on" to false,
    "tx_text" to "",
    "tx_font" to 0,
    "tx_size" to 40,
    "tx_c" to Color.WHITE,
    "tx_op" to 100,
    "tx_x" to 50,
    "tx_y" to 80,
    // photo
    "ph_hue" to 180,
    "ph_sat" to 100,
    "ph_bri" to 100,
    "ph_con" to 100,
    "ph_dim" to 0,
    "ph_zoom" to 0,
    "ph_px" to 50,
    "ph_py" to 50,
    // misc
    "motion" to false,
    "tpl" to -1
)

fun isStyleKey(k: String): Boolean = k.startsWith("clk_") || k.startsWith("dt_") || k.startsWith("tx_")

val STYLE_KEYS: List<String> = DEFAULTS.keys.filter { isStyleKey(it) }

/** Read-only view of settings with fallback to DEFAULTS. */
class Cfg(private val m: Map<String, Any>) {
    fun i(k: String): Int = (m[k] as? Int) ?: (DEFAULTS[k] as? Int) ?: 0
    fun b(k: String): Boolean = (m[k] as? Boolean) ?: (DEFAULTS[k] as? Boolean) ?: false
    fun s(k: String): String = (m[k] as? String) ?: (DEFAULTS[k] as? String) ?: ""
}

fun cfgFromPrefs(p: SharedPreferences): Cfg {
    val m = HashMap<String, Any>()
    for ((k, v) in p.all) {
        if (v != null) m[k] = v
    }
    return Cfg(m)
}

class Template(val name: String, val cfg: Map<String, Any>)

private fun tpl(name: String, vararg p: Pair<String, Any>) = Template(name, mapOf(*p))

val TEMPLATES: List<Template> = listOf(
    tpl("Zen",
        "clk_font" to 1, "clk_size" to 62, "clk_c1" to Color.WHITE, "clk_c2" to 0xFFFFE066.toInt(),
        "clk_y" to 22, "dt_font" to 1),
    tpl("Ember",
        "clk_font" to 0, "clk_size" to 70, "clk_c1" to 0xFFFF6B4A.toInt(), "clk_c2" to 0xFFFFE066.toInt(),
        "clk_sh" to true, "clk_shs" to 60, "clk_y" to 20),
    tpl("Mono",
        "clk_font" to 4, "clk_size" to 55, "clk_c1" to Color.WHITE, "clk_c2" to 0xFFB0B0B0.toInt(),
        "clk_layout" to 1, "clk_y" to 12, "dt_font" to 4),
    tpl("Slim",
        "clk_font" to 2, "clk_size" to 80, "clk_c1" to Color.WHITE, "clk_c2" to Color.WHITE,
        "clk_lh" to 30, "clk_op" to 85, "clk_y" to 18, "dt_up" to true),
    tpl("Royal",
        "clk_font" to 3, "clk_size" to 60, "clk_c1" to 0xFFE8C872.toInt(), "clk_c2" to Color.WHITE,
        "clk_sh" to true, "clk_y" to 26, "dt_font" to 3),
    tpl("Candy",
        "clk_font" to 6, "clk_size" to 62, "clk_c1" to 0xFFFF8FB1.toInt(), "clk_c2" to 0xFF7FDBFF.toInt(),
        "clk_ow" to 20, "clk_oc" to Color.WHITE, "clk_y" to 24, "dt_font" to 6),
    tpl("Aqua",
        "clk_font" to 0, "clk_size" to 66, "clk_c1" to 0xFF7FDBFF.toInt(), "clk_c2" to Color.WHITE,
        "clk_y" to 28, "clk_sh" to true, "clk_shs" to 35),
    tpl("Flame",
        "clk_font" to 5, "clk_size" to 75, "clk_c1" to 0xFFFFA94D.toInt(), "clk_c2" to 0xFFFF4D4D.toInt(),
        "clk_sh" to true, "clk_lh" to 40, "clk_y" to 15),
    tpl("Neon",
        "clk_font" to 2, "clk_size" to 68, "clk_c1" to 0xFF7CFFB2.toInt(), "clk_c2" to 0xFFE056FD.toInt(),
        "clk_ow" to 15, "clk_oc" to 0xFF141B3A.toInt(), "clk_sh" to true, "clk_shs" to 80, "clk_y" to 22),
    tpl("Retro",
        "clk_font" to 4, "clk_size" to 60, "clk_c1" to 0xFFFFE066.toInt(), "clk_c2" to 0xFFFF8FB1.toInt(),
        "clk_layout" to 1, "clk_rot" to 40, "clk_ow" to 10, "clk_oc" to Color.BLACK, "clk_y" to 30),
    tpl("Minimal",
        "clk_font" to 1, "clk_size" to 40, "clk_c1" to Color.WHITE, "clk_c2" to Color.WHITE,
        "clk_layout" to 1, "clk_y" to 8, "dt_pos" to 1, "dt_gap" to 30)
)

private val FONTS: Array<Typeface> by lazy {
    arrayOf(
        Typeface.create("sans-serif-medium", Typeface.BOLD),
        Typeface.create("sans-serif-light", Typeface.NORMAL),
        Typeface.create("sans-serif-condensed", Typeface.BOLD),
        Typeface.create(Typeface.SERIF, Typeface.BOLD),
        Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
        Typeface.create("sans-serif-black", Typeface.NORMAL),
        Typeface.create("casual", Typeface.NORMAL),
        Typeface.create("cursive", Typeface.NORMAL)
    )
}

fun fontFor(i: Int): Typeface = FONTS[i.coerceIn(0, FONTS.size - 1)]

fun hueMatrix(deg: Float): ColorMatrix {
    val r = Math.toRadians(deg.toDouble())
    val c = cos(r).toFloat()
    val s = sin(r).toFloat()
    val lr = 0.213f
    val lg = 0.715f
    val lb = 0.072f
    return ColorMatrix(
        floatArrayOf(
            lr + c * (1 - lr) + s * (-lr), lg + c * (-lg) + s * (-lg), lb + c * (-lb) + s * (1 - lb), 0f, 0f,
            lr + c * (-lr) + s * 0.143f, lg + c * (1 - lg) + s * 0.140f, lb + c * (-lb) + s * (-0.283f), 0f, 0f,
            lr + c * (-lr) + s * (-(1 - lr)), lg + c * (-lg) + s * lg, lb + c * (1 - lb) + s * lb, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

/** The two photo layers, shared between the app and the wallpaper service. */
object Layers {
    @Volatile
    var bg: Bitmap? = null

    @Volatile
    var fg: Bitmap? = null
    private var stamp = -1L

    @Synchronized
    fun ensure(ctx: Context) {
        val bf = File(ctx.filesDir, BG_FILE)
        val ff = File(ctx.filesDir, FG_FILE)
        val s = max(bf.lastModified(), ff.lastModified())
        if (s == stamp) return
        stamp = s
        bg = if (bf.exists()) BitmapFactory.decodeFile(bf.path) else null
        fg = if (ff.exists()) BitmapFactory.decodeFile(ff.path) else null
    }
}

/**
 * Draws: background photo -> clock/date/text -> cut-out subject on top.
 * ox, oy (-1..1) only matter when motion is on.
 */
class DepthRenderer(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dimPaint = Paint()
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private val hf24 = SimpleDateFormat("HH", Locale.US)
    private val hf12 = SimpleDateFormat("hh", Locale.US)
    private val minFmt = SimpleDateFormat("mm", Locale.US)

    fun draw(c: Canvas, w: Int, h: Int, ox0: Float, oy0: Float, custom: Cfg? = null) {
        Layers.ensure(ctx)
        val cfg = custom ?: cfgFromPrefs(prefs)
        val motion = cfg.b(KEY_MOTION)
        val ox = if (motion) ox0 else 0f
        val oy = if (motion) oy0 else 0f

        c.drawColor(Color.BLACK)
        bitmapPaint.colorFilter = buildFilter(cfg)

        val bg = Layers.bg
        val fg = Layers.fg
        if (bg != null) {
            layer(c, bg, w, h, cfg, motion, ox * 0.012f * w, oy * 0.008f * h)
            val dim = cfg.i("ph_dim")
            if (dim > 0) {
                dimPaint.color = Color.argb(dim * 255 / 100, 0, 0, 0)
                c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dimPaint)
            }
        } else {
            val p = Paint()
            p.shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                0xFF3A4678.toInt(), 0xFF0D1017.toInt(), Shader.TileMode.CLAMP
            )
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        }

        val sx = ox * 0.03f * w
        val sy = oy * 0.02f * h
        var clock: FloatArray? = null
        if (cfg.b("clk_on")) {
            clock = drawClock(c, w, h, cfg, sx, sy)
        }
        if (cfg.b("dt_on") && clock != null) {
            drawDate(c, w, cfg, clock)
        }
        if (cfg.b("tx_on")) {
            drawCustomText(c, w, h, cfg)
        }

        if (fg != null) {
            layer(c, fg, w, h, cfg, motion, ox * 0.055f * w, oy * 0.035f * h)
        }
    }

    private fun buildFilter(cfg: Cfg): ColorFilter? {
        val hue = cfg.i("ph_hue") - 180
        val sat = cfg.i("ph_sat")
        val bri = cfg.i("ph_bri")
        val con = cfg.i("ph_con")
        if (hue == 0 && sat == 100 && bri == 100 && con == 100) return null
        val m = ColorMatrix()
        m.postConcat(hueMatrix(hue.toFloat()))
        val s = ColorMatrix()
        s.setSaturation(sat / 100f)
        m.postConcat(s)
        val k = con / 100f
        val t = (-0.5f * k + 0.5f) * 255f + (bri - 100) * 2.55f
        m.postConcat(
            ColorMatrix(
                floatArrayOf(
                    k, 0f, 0f, 0f, t,
                    0f, k, 0f, 0f, t,
                    0f, 0f, k, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        return ColorMatrixColorFilter(m)
    }

    private fun layer(c: Canvas, bmp: Bitmap, w: Int, h: Int, cfg: Cfg, motion: Boolean, sx: Float, sy: Float) {
        val margin = if (motion) 0.12f else 0f
        val zoom = cfg.i("ph_zoom") / 100f * 0.6f
        val s = max(w.toFloat() / bmp.width, h.toFloat() / bmp.height) * (1f + margin + zoom)
        val ovx = bmp.width * s - w
        val ovy = bmp.height * s - h
        val tx = -ovx / 2f + (cfg.i("ph_px") - 50) / 50f * (ovx / 2f) + sx
        val ty = -ovy / 2f + (cfg.i("ph_py") - 50) / 50f * (ovy / 2f) + sy
        matrix.reset()
        matrix.postScale(s, s)
        matrix.postTranslate(tx, ty)
        c.drawBitmap(bmp, matrix, bitmapPaint)
    }

    private fun setup(font: Int, size: Float, spacing: Float, stretch: Float) {
        tp.typeface = fontFor(font)
        tp.textSize = size
        tp.letterSpacing = spacing
        tp.textScaleX = stretch
        tp.textAlign = Paint.Align.LEFT
        tp.style = Paint.Style.FILL
        tp.clearShadowLayer()
    }

    private fun put(
        c: Canvas, t: String, x: Float, y: Float, color: Int,
        op: Int, ow: Int, oc: Int, sh: Boolean, shs: Int
    ) {
        val a = (op * 255 / 100).coerceIn(0, 255)
        if (ow > 0) {
            tp.style = Paint.Style.STROKE
            tp.strokeJoin = Paint.Join.ROUND
            tp.strokeWidth = tp.textSize * ow / 100f * 0.12f
            tp.color = oc
            tp.alpha = a
            tp.clearShadowLayer()
            c.drawText(t, x, y, tp)
        }
        tp.style = Paint.Style.FILL
        tp.color = color
        tp.alpha = a
        if (sh) {
            val sa = (160 * a / 255).coerceIn(0, 255)
            tp.setShadowLayer(
                tp.textSize * 0.08f * shs / 50f + 1f,
                tp.textSize * 0.03f, tp.textSize * 0.04f,
                Color.argb(sa, 0, 0, 0)
            )
        } else {
            tp.clearShadowLayer()
        }
        c.drawText(t, x, y, tp)
        tp.clearShadowLayer()
    }

    /** Returns [top, bottomBaseline, centerX] so the date can sit next to the clock. */
    private fun drawClock(c: Canvas, w: Int, h: Int, cfg: Cfg, sx: Float, sy: Float): FloatArray {
        val ts = w * (0.16f + cfg.i("clk_size") / 100f * 0.6f) * (if (cfg.i("clk_layout") == 1) 0.5f else 1f)
        setup(cfg.i("clk_font"), ts, cfg.i("clk_ls") / 100f * 0.3f, cfg.i("clk_str") / 100f)

        val now = Date()
        val use24 = when (cfg.i("clk_hf")) {
            1 -> true
            2 -> false
            else -> DateFormat.is24HourFormat(ctx)
        }
        val hh = (if (use24) hf24 else hf12).format(now)
        val mm = minFmt.format(now)

        val cx = w * cfg.i("clk_x") / 100f + sx
        val top = h * (0.04f + cfg.i("clk_y") / 100f * 0.8f) + sy
        val gap = ts * (0.55f + cfg.i("clk_lh") / 100f * 0.8f)
        val rot = (cfg.i("clk_rot") - 45).toFloat()
        val c1 = cfg.i("clk_c1")
        val c2 = cfg.i("clk_c2")
        val op = cfg.i("clk_op")
        val ow = cfg.i("clk_ow")
        val oc = cfg.i("clk_oc")
        val sh = cfg.b("clk_sh")
        val shs = cfg.i("clk_shs")
        val base1 = top + ts * 0.78f
        val bottom: Float

        c.save()
        if (cfg.i("clk_layout") == 1) {
            val wh = tp.measureText(hh)
            val wc = tp.measureText(":")
            val wm = tp.measureText(mm)
            val total = wh + wc + wm
            c.rotate(rot, cx, base1 - ts * 0.3f)
            var x = cx - total / 2f
            put(c, hh, x, base1, c1, op, ow, oc, sh, shs)
            x += wh
            put(c, ":", x, base1, c1, op, ow, oc, sh, shs)
            x += wc
            put(c, mm, x, base1, c2, op, ow, oc, sh, shs)
            bottom = base1
        } else {
            val base2 = base1 + gap
            c.rotate(rot, cx, (base1 + base2) / 2f - ts * 0.3f)
            put(c, hh, cx - tp.measureText(hh) / 2f, base1, c1, op, ow, oc, sh, shs)
            put(c, mm, cx - tp.measureText(mm) / 2f, base2, c2, op, ow, oc, sh, shs)
            bottom = base2
        }
        c.restore()
        return floatArrayOf(top, bottom, cx)
    }

    private fun drawDate(c: Canvas, w: Int, cfg: Cfg, clock: FloatArray) {
        val ds = w * (0.03f + cfg.i("dt_size") / 100f * 0.12f)
        setup(cfg.i("dt_font"), ds, cfg.i("dt_ls") / 100f * 0.5f, 1f)
        var t = SimpleDateFormat(DATE_FMTS[cfg.i("dt_fmt").coerceIn(0, DATE_FMTS.size - 1)], Locale.getDefault())
            .format(Date())
        if (cfg.b("dt_up")) t = t.uppercase()
        val g = ds * (0.5f + cfg.i("dt_gap") / 100f * 1.5f)
        val y = if (cfg.i("dt_pos") == 0) clock[1] + g + ds * 0.8f else clock[0] - g
        val x = clock[2] - tp.measureText(t) / 2f
        put(c, t, x, y, cfg.i("dt_c"), cfg.i("clk_op"), 0, 0, cfg.b("clk_sh"), cfg.i("clk_shs"))
    }

    private fun drawCustomText(c: Canvas, w: Int, h: Int, cfg: Cfg) {
        val t = cfg.s("tx_text")
        if (t.isBlank()) return
        val size = w * (0.03f + cfg.i("tx_size") / 100f * 0.2f)
        setup(cfg.i("tx_font"), size, 0f, 1f)
        val x = w * cfg.i("tx_x") / 100f - tp.measureText(t) / 2f
        val y = h * cfg.i("tx_y") / 100f
        put(c, t, x, y, cfg.i("tx_c"), cfg.i("tx_op"), 0, 0, false, 0)
    }
}

/** Live preview inside the app. */
class PreviewView(context: Context) : View(context) {
    private val renderer = DepthRenderer(context.applicationContext)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val start = System.currentTimeMillis()

    override fun onDraw(canvas: Canvas) {
        val motion = prefs.getBoolean(KEY_MOTION, false)
        val t = (System.currentTimeMillis() - start) / 1000f
        renderer.draw(canvas, width, height, if (motion) sin(t * 1.5f) * 0.8f else 0f, 0f)
        if (motion) postInvalidateOnAnimation() else postInvalidateDelayed(1000)
    }
}
