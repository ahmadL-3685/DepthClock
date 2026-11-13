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
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

const val PREFS = "depth"
const val KEY_MOTION = "motion"
const val BG_FILE = "bg.jpg"
const val FG_FILE = "fg.png"
const val BASE_FONTS = 11

/** Extra fonts: display name to file name in assets/fonts (downloaded by the build workflow). */
val EXTRA_FONTS: List<Pair<String, String>> = listOf(
    "Anton" to "Anton-Regular.ttf",
    "Bebas" to "BebasNeue-Regular.ttf",
    "Pirata" to "PirataOne-Regular.ttf",
    "Bungee" to "Bungee-Regular.ttf",
    "Stencil" to "BlackOpsOne-Regular.ttf",
    "Pixel" to "PressStart2P-Regular.ttf",
    "Dots" to "DotGothic16-Regular.ttf",
    "Marker" to "PermanentMarker-Regular.ttf",
    "Orbitron" to "Orbitron.ttf",
    "Italiana" to "Italiana-Regular.ttf",
    "Monoton" to "Monoton-Regular.ttf",
    "Distress" to "RubikDistressed-Regular.ttf",
    "Metal" to "MetalMania-Regular.ttf",
    "Cinzel" to "CinzelDecorative-Bold.ttf"
)
val FONT_COUNT = BASE_FONTS + EXTRA_FONTS.size

val FONT_NAMES = listOf("Medium", "Light", "Condensed", "Serif", "Mono", "Black", "Casual", "Script", "Thin", "Regular", "Narrow") + EXTRA_FONTS.map { it.first }

/** Set once at start-up so fontFor can read fonts from assets. */
@Volatile
var fontAssets: android.content.res.AssetManager? = null
val DATE_FMTS = arrayOf("EEEE d MMMM", "EEE d MMM", "yyyy/MM/dd", "d MMMM")

/** Default value of every setting. Anything not stored in prefs falls back to this. */
val DEFAULTS: Map<String, Any> = mapOf<String, Any>(
    // clock
    "clk_on" to true,
    "clk_hf" to 0,
    "clk_layout" to 0,
    "clk_sep" to 0,
    "clk_style" to 1,
    "clk_wt" to 0,
    "clk_it" to false,
    "clk_pg" to 25,
    "clk_sk" to 50,
    "clk_vs" to 100,
    "clk_gl" to false,
    "clk_glo" to 40,
    "clk_glc" to 52,
    "clk_glh" to 45,
    "clk_glv" to 30,
    "clk_glt" to 0xFFB8C4D0.toInt(),
    "clk_gb" to true,
    "clk_gbw" to 15,
    "clk_gbc" to Color.WHITE,
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
    "dt_dx" to 50,
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
        "clk_layout" to 1, "clk_y" to 8, "dt_pos" to 1, "dt_gap" to 30),
    tpl("Poster",
        "clk_font" to 10, "clk_layout" to 1, "clk_sep" to 2, "clk_size" to 100, "clk_c1" to Color.WHITE,
        "clk_c2" to 0xFF5A66C8.toInt(), "clk_str" to 70, "clk_pg" to 10, "clk_y" to 14,
        "dt_font" to 4, "dt_up" to true),
    tpl("Glass",
        "clk_font" to 9, "clk_layout" to 1, "clk_size" to 80, "clk_style" to 0, "clk_c1" to Color.WHITE,
        "clk_gl" to true, "clk_glo" to 35, "clk_y" to 30, "dt_font" to 9),
    // ---- قالب‌های مخصوص عکس‌های ارسالی ----
    tpl("Crimson Hood",
        "clk_font" to 2, "clk_size" to 88, "clk_style" to 1, "clk_c1" to Color.WHITE, "clk_c2" to 0xFFE53935.toInt(),
        "clk_lh" to 38, "clk_sh" to true, "clk_shs" to 55, "clk_y" to 30,
        "dt_font" to 2, "dt_up" to true, "dt_c" to 0xFFFFB3B0.toInt(), "dt_ls" to 50),
    tpl("Night City",
        "clk_font" to 2, "clk_size" to 82, "clk_style" to 1, "clk_c1" to 0xFFFF3DDB.toInt(), "clk_c2" to 0xFF00E5FF.toInt(),
        "clk_ow" to 12, "clk_oc" to 0xFF0A0420.toInt(), "clk_sh" to true, "clk_shs" to 85, "clk_lh" to 40, "clk_y" to 22,
        "dt_font" to 4, "dt_up" to true, "dt_c" to 0xFF9BF6FF.toInt(), "dt_ls" to 60),
    tpl("Eclipse",
        "clk_font" to 8, "clk_layout" to 1, "clk_size" to 75, "clk_style" to 1, "clk_c1" to Color.WHITE, "clk_c2" to 0xFFFF3B30.toInt(),
        "clk_ls" to 40, "clk_op" to 95, "clk_sh" to true, "clk_shs" to 70, "clk_y" to 20,
        "dt_font" to 8, "dt_up" to true, "dt_ls" to 90, "dt_c" to 0xFFFF8A80.toInt()),
    tpl("Noir",
        "clk_font" to 5, "clk_size" to 92, "clk_style" to 2, "clk_c1" to Color.WHITE, "clk_c2" to 0xFF8A949E.toInt(),
        "clk_lh" to 35, "clk_op" to 92, "clk_y" to 32, "clk_sh" to true, "clk_shs" to 40,
        "dt_font" to 2, "dt_up" to true, "dt_ls" to 70, "dt_c" to 0xFFC8CED4.toInt()),
    tpl("Lagoon",
        "clk_font" to 9, "clk_layout" to 1, "clk_size" to 82, "clk_style" to 3, "clk_c1" to Color.WHITE, "clk_c2" to 0xFF7FF0FF.toInt(),
        "clk_gl" to true, "clk_glo" to 25, "clk_sh" to true, "clk_shs" to 35, "clk_y" to 16,
        "dt_font" to 9, "dt_c" to Color.WHITE, "dt_ls" to 40)
)

private val FONT_FAMS = arrayOf(
    "sans-serif-medium", "sans-serif-light", "sans-serif-condensed", "serif", "monospace",
    "sans-serif-black", "casual", "cursive", "sans-serif-thin", "sans-serif", "sans-serif-condensed"
)
private val FONT_BOLD = booleanArrayOf(true, false, true, true, true, false, false, false, false, false, false)
private val tfCache = HashMap<Int, Typeface>()

/** wt: 0 = font as designed, 1 = semi-bold, 2 = bold. */
fun fontFor(i: Int, wt: Int = 0, italic: Boolean = false): Typeface {
    if (i >= BASE_FONTS) {
        val ek = i.coerceAtMost(FONT_COUNT - 1)
        val ck = 10000 + ek
        val hit = tfCache[ck]
        if (hit != null) return hit
        val am = fontAssets
        val tf = try {
            Typeface.createFromAsset(am, "fonts/" + EXTRA_FONTS[ek - BASE_FONTS].second)
        } catch (e: Exception) {
            return Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        tfCache[ck] = tf
        return tf
    }
    val k = i.coerceIn(0, FONT_FAMS.size - 1)
    val key = k * 100 + wt * 10 + (if (italic) 1 else 0)
    val cached = tfCache[key]
    if (cached != null) return cached
    val fam = FONT_FAMS[k]
    var style = if (FONT_BOLD[k]) Typeface.BOLD else Typeface.NORMAL
    if (italic) style = style or Typeface.ITALIC
    val base = Typeface.create(fam, style)
    val tf = if (wt == 0) {
        base
    } else if (Build.VERSION.SDK_INT >= 28) {
        Typeface.create(base, if (wt == 1) 600 else 800, italic)
    } else {
        Typeface.create(fam, Typeface.BOLD or (if (italic) Typeface.ITALIC else 0))
    }
    tfCache[key] = tf
    return tf
}

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
    init { fontAssets = ctx.applicationContext.assets }
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

    private val platePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = ctx.resources.displayMetrics.density

    private fun setup(font: Int, wt: Int, italic: Boolean, size: Float, spacing: Float, stretch: Float, skew: Float) {
        tp.typeface = fontFor(font, wt, italic)
        tp.textSize = size
        tp.letterSpacing = spacing
        tp.textScaleX = stretch
        tp.textSkewX = -skew
        tp.textAlign = Paint.Align.LEFT
        tp.style = Paint.Style.FILL
        tp.shader = null
        tp.clearShadowLayer()
    }

    private fun put(
        c: Canvas, t: String, x: Float, y: Float, color: Int,
        op: Int, ow: Int, oc: Int, sh: Boolean, shs: Int, shader: Shader? = null
    ) {
        val a = (op * 255 / 100).coerceIn(0, 255)
        if (ow > 0) {
            tp.shader = null
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
        tp.shader = shader
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
        tp.shader = null
        tp.clearShadowLayer()
    }

    private fun drawPlate(c: Canvas, cfg: Cfg, cx: Float, tw: Float, firstBase: Float, lastBase: Float, ts: Float) {
        val ph = ts * cfg.i("clk_glh") / 100f
        val pv = ts * cfg.i("clk_glv") / 100f
        val r = RectF(cx - tw / 2f - ph, firstBase - ts * 0.76f - pv, cx + tw / 2f + ph, lastBase + ts * 0.18f + pv)
        val rad = min(r.width(), r.height()) / 2f * cfg.i("clk_glc") / 100f
        platePaint.style = Paint.Style.FILL
        platePaint.color = cfg.i("clk_glt")
        platePaint.alpha = (cfg.i("clk_glo") * 255 / 100).coerceIn(0, 255)
        c.drawRoundRect(r, rad, rad, platePaint)
        if (cfg.b("clk_gb")) {
            platePaint.style = Paint.Style.STROKE
            platePaint.strokeWidth = max(1f, cfg.i("clk_gbw") / 10f * density)
            platePaint.color = cfg.i("clk_gbc")
            platePaint.alpha = 190
            c.drawRoundRect(r, rad, rad, platePaint)
        }
    }

    /** Returns [top, bottomBaseline, centerX] so the date can sit next to the clock. */
    private fun drawClock(c: Canvas, w: Int, h: Int, cfg: Cfg, sx: Float, sy: Float): FloatArray {
        val layout = cfg.i("clk_layout")
        var ts = w * (0.16f + cfg.i("clk_size") / 100f * 0.6f)
        if (layout != 0) ts *= 0.62f
        setup(
            cfg.i("clk_font"), cfg.i("clk_wt"), cfg.b("clk_it"), ts,
            cfg.i("clk_ls") / 100f * 0.3f, cfg.i("clk_str") / 100f, (cfg.i("clk_sk") - 50) / 50f * 0.5f
        )

        val now = Date()
        val use24 = when (cfg.i("clk_hf")) {
            1 -> true
            2 -> false
            else -> DateFormat.is24HourFormat(ctx)
        }
        val hh = (if (use24) hf24 else hf12).format(now)
        val mm = minFmt.format(now)
        val sep = if (layout == 1) {
            when (cfg.i("clk_sep")) {
                1 -> "."
                2 -> ""
                else -> ":"
            }
        } else ""

        val wh = tp.measureText(hh)
        val wm = tp.measureText(mm)
        val ws = if (sep.isEmpty()) 0f else tp.measureText(sep)
        val gap = ts * cfg.i("clk_pg") / 100f * 0.6f
        val totalW = when (layout) {
            0 -> max(wh, wm)
            2 -> wh + gap * 3f + wm
            else -> if (sep.isEmpty()) wh + gap + wm else wh + ws + gap * 2f + wm
        }

        val cx = w * cfg.i("clk_x") / 100f + sx
        val top = h * (0.04f + cfg.i("clk_y") / 100f * 0.8f) + sy
        val lineGap = ts * (0.55f + cfg.i("clk_lh") / 100f * 0.8f)
        val rot = (cfg.i("clk_rot") - 45).toFloat()
        val vs = cfg.i("clk_vs") / 100f
        val style = cfg.i("clk_style")
        val c1 = cfg.i("clk_c1")
        val c2raw = cfg.i("clk_c2")
        val c2 = if (style == 0) c1 else c2raw
        val op = cfg.i("clk_op")
        val ow = cfg.i("clk_ow")
        val oc = cfg.i("clk_oc")
        val sh = cfg.b("clk_sh")
        val shs = cfg.i("clk_shs")
        val base1 = top + ts * 0.78f
        val base2 = if (layout == 0) base1 + lineGap else base1
        val gx0 = cx - totalW / 2f
        val gx1 = cx + totalW / 2f
        val midY = (base1 + base2) / 2f - ts * 0.3f

        fun shaderAt(base: Float): Shader? = when (style) {
            2 -> LinearGradient(0f, base - ts * 0.75f, 0f, base, c1, c2raw, Shader.TileMode.CLAMP)
            3 -> LinearGradient(gx0, 0f, gx1, 0f, c1, c2raw, Shader.TileMode.CLAMP)
            else -> null
        }

        fun part(t: String, x: Float, y: Float, col: Int) {
            put(c, t, x, y, col, op, ow, oc, sh, shs, shaderAt(y))
        }

        c.save()
        c.rotate(rot, cx, midY)
        if (vs != 1f) c.scale(1f, vs, cx, midY)
        if (cfg.b("clk_gl")) drawPlate(c, cfg, cx, totalW, base1, base2, ts)

        if (layout == 0) {
            part(hh, cx - wh / 2f, base1, c1)
            part(mm, cx - wm / 2f, base2, c2)
        } else {
            var x = gx0
            part(hh, x, base1, c1)
            x += wh
            if (layout == 2) {
                x += gap * 3f
            } else if (sep.isNotEmpty()) {
                x += gap
                part(sep, x, base1, c1)
                x += ws + gap
            } else {
                x += gap
            }
            part(mm, x, base1, c2)
        }
        c.restore()
        return floatArrayOf(top, base2, cx)
    }

    private fun drawDate(c: Canvas, w: Int, cfg: Cfg, clock: FloatArray) {
        val ds = w * (0.03f + cfg.i("dt_size") / 100f * 0.12f)
        setup(cfg.i("dt_font"), 0, false, ds, cfg.i("dt_ls") / 100f * 0.5f, 1f, 0f)
        var t = SimpleDateFormat(DATE_FMTS[cfg.i("dt_fmt").coerceIn(0, DATE_FMTS.size - 1)], Locale.getDefault())
            .format(Date())
        if (cfg.b("dt_up")) t = t.uppercase()
        val g = ds * (0.5f + cfg.i("dt_gap") / 100f * 1.5f)
        val y = if (cfg.i("dt_pos") == 0) clock[1] + g + ds * 0.8f else clock[0] - g
        val x = clock[2] - tp.measureText(t) / 2f + (cfg.i("dt_dx") - 50) / 50f * w * 0.3f
        put(c, t, x, y, cfg.i("dt_c"), cfg.i("clk_op"), 0, 0, cfg.b("clk_sh"), cfg.i("clk_shs"))
    }

    private fun drawCustomText(c: Canvas, w: Int, h: Int, cfg: Cfg) {
        val t = cfg.s("tx_text")
        if (t.isBlank()) return
        val size = w * (0.03f + cfg.i("tx_size") / 100f * 0.2f)
        setup(cfg.i("tx_font"), 0, false, size, 0f, 1f, 0f)
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
