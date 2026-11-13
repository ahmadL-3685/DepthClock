package com.example.depthclock

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

// ---------------------------------------------------------------- app themes

internal class AppTheme(
    val name: String,
    val light: Boolean,
    val bg: Int, val nav: Int, val card: Int, val panel: Int, val row: Int, val chip: Int,
    val pill: Int, val text: Int, val muted: Int, val accent: Int, val onAccent: Int,
    val b1: Int, val b2: Int
)

internal val THEMES: List<AppTheme> = listOf(
    AppTheme("مشکی و زرد", false,
        0xFF000000.toInt(), 0xFF161616.toInt(), 0xFF141414.toInt(), 0xFF121212.toInt(), 0xFF1C1C1C.toInt(), 0xFF2A2A2A.toInt(),
        0xFF3D3600.toInt(), 0xFFFFFFFF.toInt(), 0xFF8A8A8A.toInt(), 0xFFFFD60A.toInt(), 0xFF000000.toInt(),
        0xFF3D3600.toInt(), 0xFF141414.toInt()),
    AppTheme("نیمه‌شب", false,
        0xFF0D1017.toInt(), 0xFF141824.toInt(), 0xFF171B25.toInt(), 0xFF1A1E27.toInt(), 0xFF151922.toInt(), 0xFF262A33.toInt(),
        0xFF2B3556.toInt(), 0xFFF1F3F8.toInt(), 0xFF8D94A6.toInt(), 0xFFB7C3FF.toInt(), 0xFF141B3A.toInt(),
        0xFF3A4A8A.toInt(), 0xFF1B2140.toInt()),
    AppTheme("بنفش", false,
        0xFF120E1F.toInt(), 0xFF191330.toInt(), 0xFF1E1838.toInt(), 0xFF221B40.toInt(), 0xFF191330.toInt(), 0xFF2E2552.toInt(),
        0xFF4A3A8C.toInt(), 0xFFF4F1FF.toInt(), 0xFF9C94BF.toInt(), 0xFFC9B8FF.toInt(), 0xFF1E1442.toInt(),
        0xFF5B3FC2.toInt(), 0xFF1E1442.toInt()),
    AppTheme("زمردی", false,
        0xFF0B1512.toInt(), 0xFF101E19.toInt(), 0xFF132620.toInt(), 0xFF16291F.toInt(), 0xFF0F1D18.toInt(), 0xFF1F3A30.toInt(),
        0xFF1F5A44.toInt(), 0xFFEFFAF5.toInt(), 0xFF86A89A.toInt(), 0xFF7CFFB2.toInt(), 0xFF06281A.toInt(),
        0xFF1F7A58.toInt(), 0xFF0F2E24.toInt()),
    AppTheme("غروب", false,
        0xFF1A0F12.toInt(), 0xFF24141A.toInt(), 0xFF2C1A21.toInt(), 0xFF301C24.toInt(), 0xFF24141A.toInt(), 0xFF43262F.toInt(),
        0xFF7A3B4A.toInt(), 0xFFFFF1F0.toInt(), 0xFFB89098.toInt(), 0xFFFFB4A2.toInt(), 0xFF3A1218.toInt(),
        0xFFB04A63.toInt(), 0xFF3A1A24.toInt()),
    AppTheme("آمولد", false,
        0xFF000000.toInt(), 0xFF0A0A0A.toInt(), 0xFF121212.toInt(), 0xFF161616.toInt(), 0xFF0E0E0E.toInt(), 0xFF242424.toInt(),
        0xFF333333.toInt(), 0xFFFFFFFF.toInt(), 0xFF8A8A8A.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(),
        0xFF3A3A3A.toInt(), 0xFF111111.toInt()),
    AppTheme("روشن", true,
        0xFFF4F6FB.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFF1F3FA.toInt(), 0xFFE3E7F2.toInt(),
        0xFFD6DEFF.toInt(), 0xFF1A1F36.toInt(), 0xFF6B7390.toInt(), 0xFF3D4FD6.toInt(), 0xFFFFFFFF.toInt(),
        0xFFD6DEFF.toInt(), 0xFFEADFFF.toInt()),
    AppTheme("کاغذی", true,
        0xFFF7F1E6.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFF3ECDD.toInt(), 0xFFEADFC9.toInt(),
        0xFFF1DDB8.toInt(), 0xFF2B2418.toInt(), 0xFF8A7F6B.toInt(), 0xFFB5651D.toInt(), 0xFFFFFFFF.toInt(),
        0xFFF1DDB8.toInt(), 0xFFFAEBD0.toInt()),
    AppTheme("نعنایی", true,
        0xFFEEF8F3.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFE4F3EC.toInt(), 0xFFD3EBDF.toInt(),
        0xFFBFE8D3.toInt(), 0xFF10281F.toInt(), 0xFF5E8072.toInt(), 0xFF1F8A62.toInt(), 0xFFFFFFFF.toInt(),
        0xFFBFE8D3.toInt(), 0xFFDDF3E8.toInt()),
    AppTheme("صورتی", true,
        0xFFFFF1F5.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFCE6EE.toInt(), 0xFFF7D6E2.toInt(),
        0xFFFAC8DA.toInt(), 0xFF3A1423.toInt(), 0xFF9A6B7C.toInt(), 0xFFD6336C.toInt(), 0xFFFFFFFF.toInt(),
        0xFFFAC8DA.toInt(), 0xFFFFE3EC.toInt()),
    AppTheme("لاوندر", true,
        0xFFF4F1FF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFEBE6FF.toInt(), 0xFFDDD5FA.toInt(),
        0xFFD0C6F8.toInt(), 0xFF1D1740.toInt(), 0xFF7A74A3.toInt(), 0xFF6C4FE0.toInt(), 0xFFFFFFFF.toInt(),
        0xFFD0C6F8.toInt(), 0xFFE9E3FF.toInt()),
    AppTheme("آسمانی", true,
        0xFFEEF6FC.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFE3F0FA.toInt(), 0xFFD0E6F6.toInt(),
        0xFFBBDDF3.toInt(), 0xFF0E2433.toInt(), 0xFF5F7F94.toInt(), 0xFF0F7FBF.toInt(), 0xFFFFFFFF.toInt(),
        0xFFBBDDF3.toInt(), 0xFFDCEEFA.toInt())
)

// ---------------------------------------------------------------- editor model

internal sealed class Ctl(val label: String)
internal class Tgl(label: String, val key: String) : Ctl(label)
internal class Sld(label: String, val key: String, val min: Int, val max: Int, val offset: Int = 0, val unit: String = "") : Ctl(label)
internal class Chp(label: String, val key: String, val options: List<String>) : Ctl(label)
internal class Fnt(label: String, val key: String) : Ctl(label)
internal class Clr(label: String, val key: String) : Ctl(label)
internal class Txt(label: String, val key: String) : Ctl(label)
internal class Btn(label: String, val id: Int) : Ctl(label)
internal class Prs(label: String, val k1: String, val k2: String) : Ctl(label)
internal class Grd(label: String, val kx: String, val ky: String, val xs: IntArray, val ys: IntArray) : Ctl(label)

internal val PAIRS = listOf(
    Triple("Rose", 0xFFF5EFE6.toInt(), 0xFFFF5C8A.toInt()),
    Triple("Sunset", 0xFFFFD166.toInt(), 0xFFFF5A36.toInt()),
    Triple("Ocean", 0xFF5ED3F3.toInt(), 0xFF2563EB.toInt()),
    Triple("Mint", 0xFFA7F3A0.toInt(), 0xFF10B981.toInt()),
    Triple("Violet", 0xFFD9C8FF.toInt(), 0xFF8B5CF6.toInt()),
    Triple("Cyan", 0xFF67E8F9.toInt(), 0xFF3B82F6.toInt()),
    Triple("Gold", 0xFFFFF3B0.toInt(), 0xFFE0A100.toInt()),
    Triple("Mono", 0xFFFFFFFF.toInt(), 0xFF6B7280.toInt())
)

internal class Sub(val name: String, val ctls: List<Ctl>)
internal class EdModule(val name: String, val icon: Int, val subs: List<Sub>)

internal val MODULES: List<EdModule> = listOf(
    EdModule("ساعت", 3, listOf(
        Sub("قالب‌بندی", listOf(
            Tgl("نمایش ساعت", "clk_on"),
            Chp("چیدمان", "clk_layout", listOf("دو خطی", "یک خطی", "جدا از هم")),
            Chp("جداکننده (یک خطی)", "clk_sep", listOf("دونقطه ( : )", "نقطه ( . )", "بدون")),
            Chp("فرمت ساعت", "clk_hf", listOf("سیستم", "۲۴ ساعته", "۱۲ ساعته"))
        )),
        Sub("تایپوگرافی", listOf(
            Fnt("فونت", "clk_font"),
            Chp("ضخامت", "clk_wt", listOf("پیش‌فرض", "نیمه‌ضخیم", "ضخیم")),
            Tgl("کج (ایتالیک)", "clk_it"),
            Sld("اندازه", "clk_size", 10, 100, 0, "%"),
            Sld("فاصله حروف", "clk_ls", 0, 100, 0, "%"),
            Sld("فاصله خطوط", "clk_lh", 0, 100, 0, "%"),
            Sld("فاصله ساعت و دقیقه", "clk_pg", 0, 100, 0, "%")
        )),
        Sub("استایل", listOf(
            Chp("نوع رنگ‌آمیزی", "clk_style", listOf("یکدست", "دو رنگ", "گرادیان عمودی", "گرادیان افقی")),
            Prs("رنگ‌های آماده", "clk_c1", "clk_c2"),
            Clr("رنگ اول (ساعت)", "clk_c1"),
            Clr("رنگ دوم (دقیقه)", "clk_c2"),
            Sld("شفافیت", "clk_op", 10, 100, 0, "%")
        )),
        Sub("موقعیت", listOf(
            Grd("جای ساعت", "clk_x", "clk_y", intArrayOf(28, 50, 72), intArrayOf(8, 32, 58)),
            Sld("افقی", "clk_x", 0, 100, 0, "%"),
            Sld("عمودی", "clk_y", 0, 100, 0, "%")
        )),
        Sub("افکت‌ها", listOf(
            Tgl("سایه", "clk_sh"),
            Sld("شدت سایه", "clk_shs", 0, 100, 0, "%"),
            Sld("ضخامت حاشیه حروف", "clk_ow", 0, 100, 0, "%"),
            Clr("رنگ حاشیه حروف", "clk_oc"),
            Tgl("پلاک شیشه‌ای پشت ساعت", "clk_gl"),
            Sld("شفافیت پلاک", "clk_glo", 0, 100, 0, "%"),
            Sld("گردی گوشه‌ها", "clk_glc", 0, 100, 0, "%"),
            Sld("فاصله افقی از متن", "clk_glh", 0, 100, 0, "%"),
            Sld("فاصله عمودی از متن", "clk_glv", 0, 100, 0, "%"),
            Clr("رنگ پلاک", "clk_glt"),
            Tgl("لبه پلاک", "clk_gb"),
            Sld("ضخامت لبه", "clk_gbw", 1, 50, 0, ""),
            Clr("رنگ لبه", "clk_gbc")
        )),
        Sub("تبدیل", listOf(
            Sld("چرخش", "clk_rot", 0, 90, 45, "°"),
            Sld("کشیدگی افقی", "clk_str", 50, 150, 0, "%"),
            Sld("کشیدگی عمودی", "clk_vs", 50, 150, 0, "%"),
            Sld("کج کردن", "clk_sk", 0, 100, 50, "")
        ))
    )),
    EdModule("تاریخ", 4, listOf(
        Sub("قالب‌بندی", listOf(
            Tgl("نمایش تاریخ", "dt_on"),
            Chp("نوع نمایش", "dt_fmt", listOf("کامل", "کوتاه", "عددی", "روز و ماه")),
            Tgl("حروف بزرگ", "dt_up"),
            Chp("محل", "dt_pos", listOf("زیر ساعت", "بالای ساعت"))
        )),
        Sub("تایپوگرافی", listOf(
            Fnt("فونت", "dt_font"),
            Sld("اندازه", "dt_size", 10, 100, 0, "%"),
            Sld("فاصله حروف", "dt_ls", 0, 100, 0, "%")
        )),
        Sub("استایل", listOf(
            Clr("رنگ تاریخ", "dt_c")
        )),
        Sub("موقعیت", listOf(
            Sld("فاصله از ساعت", "dt_gap", 0, 100, 0, "%"),
            Sld("جابه‌جایی افقی", "dt_dx", 0, 100, 50, "")
        ))
    )),
    EdModule("متن", 5, listOf(
        Sub("قالب‌بندی", listOf(
            Tgl("نمایش متن", "tx_on"),
            Txt("متن دلخواه", "tx_text")
        )),
        Sub("تایپوگرافی", listOf(
            Fnt("فونت", "tx_font"),
            Sld("اندازه", "tx_size", 10, 100, 0, "%")
        )),
        Sub("استایل", listOf(
            Clr("رنگ", "tx_c"),
            Sld("شفافیت", "tx_op", 10, 100, 0, "%")
        )),
        Sub("موقعیت", listOf(
            Grd("جای متن", "tx_x", "tx_y", intArrayOf(28, 50, 72), intArrayOf(15, 50, 85)),
            Sld("افقی", "tx_x", 0, 100, 0, "%"),
            Sld("عمودی", "tx_y", 0, 100, 0, "%")
        ))
    )),
    EdModule("عکس", 6, listOf(
        Sub("قالب‌بندی", listOf(
            Btn("انتخاب عکس جدید", 0),
            Btn("اصلاح دستی برش سوژه", 1)
        )),
        Sub("استایل", listOf(
            Sld("رنگ‌مایه (Hue)", "ph_hue", 0, 360, 180, "°"),
            Sld("اشباع", "ph_sat", 0, 200, 0, "%"),
            Sld("روشنایی", "ph_bri", 0, 200, 0, "%"),
            Sld("کنتراست", "ph_con", 50, 150, 0, "%"),
            Sld("تیرگی پس‌زمینه", "ph_dim", 0, 80, 0, "%")
        )),
        Sub("موقعیت", listOf(
            Sld("بزرگ‌نمایی", "ph_zoom", 0, 100, 0, "%"),
            Sld("جابه‌جایی افقی", "ph_px", 0, 100, 0, "%"),
            Sld("جابه‌جایی عمودی", "ph_py", 0, 100, 0, "%")
        ))
    ))
)

// ---------------------------------------------------------------- small custom views

internal fun roundClip(v: View, r: Float) {
    v.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, r)
        }
    }
    v.clipToOutline = true
}

/** Icons drawn by hand: 0 grid, 2 gear, 3 clock, 4 calendar, 5 T, 6 image, 7 reset, 8 back, 9 plus, else minus. */
internal class IconView(ctx: Context, private val type: Int, private var tint: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun setTint(c: Int) {
        tint = c
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w, h)
        val cx = w / 2f
        val cy = h / 2f
        p.color = tint
        p.strokeWidth = s * 0.09f
        when (type) {
            0 -> {
                val sz = s * 0.30f
                val a = cx - s * 0.34f
                val b = cx + s * 0.04f
                val a2 = cy - s * 0.34f
                val b2 = cy + s * 0.04f
                val r = s * 0.07f
                c.drawRoundRect(RectF(a, a2, a + sz, a2 + sz), r, r, p)
                c.drawRoundRect(RectF(b, a2, b + sz, a2 + sz), r, r, p)
                c.drawRoundRect(RectF(a, b2, a + sz, b2 + sz), r, r, p)
                c.drawRoundRect(RectF(b, b2, b + sz, b2 + sz), r, r, p)
            }
            2 -> {
                c.drawCircle(cx, cy, s * 0.14f, p)
                c.drawCircle(cx, cy, s * 0.27f, p)
                for (i in 0 until 8) {
                    val ang = Math.toRadians(i * 45.0)
                    val ca = cos(ang).toFloat()
                    val sa = sin(ang).toFloat()
                    c.drawLine(cx + ca * s * 0.31f, cy + sa * s * 0.31f, cx + ca * s * 0.41f, cy + sa * s * 0.41f, p)
                }
            }
            3 -> {
                c.drawCircle(cx, cy, s * 0.38f, p)
                c.drawLine(cx, cy, cx, cy - s * 0.22f, p)
                c.drawLine(cx, cy, cx + s * 0.16f, cy + s * 0.08f, p)
            }
            4 -> {
                c.drawRoundRect(RectF(cx - s * 0.36f, cy - s * 0.32f, cx + s * 0.36f, cy + s * 0.38f), s * 0.08f, s * 0.08f, p)
                c.drawLine(cx - s * 0.36f, cy - s * 0.08f, cx + s * 0.36f, cy - s * 0.08f, p)
                c.drawLine(cx - s * 0.17f, cy - s * 0.42f, cx - s * 0.17f, cy - s * 0.24f, p)
                c.drawLine(cx + s * 0.17f, cy - s * 0.42f, cx + s * 0.17f, cy - s * 0.24f, p)
            }
            5 -> {
                c.drawLine(cx - s * 0.30f, cy - s * 0.28f, cx + s * 0.30f, cy - s * 0.28f, p)
                c.drawLine(cx, cy - s * 0.28f, cx, cy + s * 0.34f, p)
            }
            6 -> {
                c.drawRoundRect(RectF(cx - s * 0.38f, cy - s * 0.32f, cx + s * 0.38f, cy + s * 0.32f), s * 0.08f, s * 0.08f, p)
                val path = Path()
                path.moveTo(cx - s * 0.30f, cy + s * 0.22f)
                path.lineTo(cx - s * 0.08f, cy - s * 0.02f)
                path.lineTo(cx + s * 0.06f, cy + s * 0.12f)
                path.lineTo(cx + s * 0.16f, cy + s * 0.02f)
                path.lineTo(cx + s * 0.30f, cy + s * 0.22f)
                c.drawPath(path, p)
                c.drawCircle(cx + s * 0.18f, cy - s * 0.14f, s * 0.05f, p)
            }
            7 -> {
                val r = s * 0.30f
                c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -50f, 280f, false, p)
                val px = cx + r * cos(Math.toRadians(-50.0)).toFloat()
                val py = cy + r * sin(Math.toRadians(-50.0)).toFloat()
                c.drawLine(px, py, px + s * 0.18f, py + s * 0.02f, p)
                c.drawLine(px, py, px + s * 0.02f, py - s * 0.17f, p)
            }
            8 -> {
                c.drawLine(cx + s * 0.22f, cy, cx - s * 0.26f, cy, p)
                c.drawLine(cx - s * 0.26f, cy, cx - s * 0.02f, cy - s * 0.24f, p)
                c.drawLine(cx - s * 0.26f, cy, cx - s * 0.02f, cy + s * 0.24f, p)
            }
            9 -> {
                c.drawLine(cx - s * 0.3f, cy, cx + s * 0.3f, cy, p)
                c.drawLine(cx, cy - s * 0.3f, cx, cy + s * 0.3f, p)
            }
            11 -> {
                c.drawLine(cx - s * 0.28f, cy, cx - s * 0.08f, cy + s * 0.2f, p)
                c.drawLine(cx - s * 0.08f, cy + s * 0.2f, cx + s * 0.3f, cy - s * 0.2f, p)
            }
            else -> {
                c.drawLine(cx - s * 0.3f, cy, cx + s * 0.3f, cy, p)
            }
        }
    }
}

/** Touch-up screen: paint to erase parts of the cut-out or bring back parts that were removed by mistake. */
internal class MaskEditView(ctx: Context, private val bg: Bitmap, val fg: Bitmap) : View(ctx) {
    var erase = true
    var brush = 40f
    private val fgCanvas = Canvas(fg)
    private val undoStack = ArrayList<Bitmap>()
    private val m = Matrix()
    private val inv = Matrix()
    private val pts = FloatArray(2)
    private var lx = 0f
    private var ly = 0f
    private var vx = 0f
    private var vy = 0f
    private var scaleNow = 1f
    private var touching = false
    private val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 70 }
    private val fgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val back = Paint().apply { color = 0xFF101010.toInt() }
    private val cursor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 3f
    }
    private val clearMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private val strokeErase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        xfermode = clearMode
    }
    private val fillErase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = clearMode
    }
    private val strokeBack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        shader = BitmapShader(bg, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    private val fillBack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = BitmapShader(bg, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    private fun fit() {
        val s = min(width.toFloat() / bg.width, height.toFloat() / bg.height)
        scaleNow = s
        m.reset()
        m.postScale(s, s)
        m.postTranslate((width - bg.width * s) / 2f, (height - bg.height * s) / 2f)
        m.invert(inv)
    }

    override fun onDraw(c: Canvas) {
        fit()
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), back)
        c.drawBitmap(bg, m, bgPaint)
        c.drawBitmap(fg, m, fgPaint)
        if (touching) c.drawCircle(vx, vy, brush * scaleNow / 2f, cursor)
    }

    private fun dot(x: Float, y: Float) {
        fgCanvas.drawCircle(x, y, brush / 2f, if (erase) fillErase else fillBack)
    }

    private fun line(x0: Float, y0: Float, x1: Float, y1: Float) {
        val p = if (erase) strokeErase else strokeBack
        p.strokeWidth = brush
        fgCanvas.drawLine(x0, y0, x1, y1, p)
    }

    private fun pushUndo() {
        undoStack.add(fg.copy(Bitmap.Config.ARGB_8888, false))
        if (undoStack.size > 6) undoStack.removeAt(0).recycle()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val b = undoStack.removeAt(undoStack.size - 1)
        fgCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
        fgCanvas.drawBitmap(b, 0f, 0f, null)
        b.recycle()
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        fit()
        pts[0] = e.x
        pts[1] = e.y
        inv.mapPoints(pts)
        val px = pts[0]
        val py = pts[1]
        vx = e.x
        vy = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pushUndo()
                lx = px
                ly = py
                touching = true
                dot(px, py)
            }
            MotionEvent.ACTION_MOVE -> {
                line(lx, ly, px, py)
                lx = px
                ly = py
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> touching = false
        }
        invalidate()
        return true
    }
}

/** One template card. idx -1 = "my design" (the current settings). */
internal class TemplateCard(
    ctx: Context,
    val idx: Int,
    private val title: String,
    private val prefs: SharedPreferences,
    accent: Int,
    onAccent: Int
) : View(ctx) {
    private val renderer = DepthRenderer(ctx.applicationContext)
    private val scrim = Paint()
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }
    private val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = onAccent
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun cfg(): Cfg {
        val m = HashMap<String, Any>()
        for ((k, v) in prefs.all) {
            if (v == null) continue
            if (idx < 0 || !isStyleKey(k)) m[k] = v
        }
        if (idx >= 0) {
            for ((k, v) in TEMPLATES[idx].cfg) m[k] = v
        }
        m[KEY_MOTION] = false
        return Cfg(m)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        scrim.shader = LinearGradient(
            0f, h * 0.72f, 0f, h.toFloat(),
            0x00000000, 0xCC000000.toInt(), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        renderer.draw(c, width, height, 0f, 0f, cfg())
        if (idx < 0) {
            c.drawRect(0f, 0f, w, h, scrim)
            namePaint.textSize = w * 0.085f
            c.drawText(title, w * 0.07f, h - w * 0.07f, namePaint)
        }
        if (prefs.getInt("tpl", -1) == idx) {
            val r = w * 0.065f
            val bx = w - r * 1.6f
            val by = r * 1.6f
            c.drawCircle(bx, by, r, badge)
            tick.strokeWidth = r * 0.28f
            c.drawLine(bx - r * 0.42f, by, bx - r * 0.1f, by + r * 0.34f, tick)
            c.drawLine(bx - r * 0.1f, by + r * 0.34f, bx + r * 0.45f, by - r * 0.32f, tick)
        }
    }
}

// ---------------------------------------------------------------- activity

class MainActivity : ComponentActivity() {

    private val th: AppTheme by lazy {
        THEMES[getSharedPreferences(PREFS, MODE_PRIVATE).getInt("theme", 0).coerceIn(0, THEMES.size - 1)]
    }
    private val cBg: Int get() = th.bg
    private val cNav: Int get() = th.nav
    private val cCard: Int get() = th.card
    private val cPanel: Int get() = th.panel
    private val cRow: Int get() = th.row
    private val cChip: Int get() = th.chip
    private val cPill: Int get() = th.pill
    private val cText: Int get() = th.text
    private val cMuted: Int get() = th.muted
    private val cAccent: Int get() = th.accent
    private val cOnAccent: Int get() = th.onAccent

    private val palette = intArrayOf(
        Color.WHITE, Color.BLACK,
        0xFFFFE066.toInt(), 0xFF7FDBFF.toInt(), 0xFFFF8FB1.toInt(), 0xFFB7C3FF.toInt(),
        0xFFFF6B4A.toInt(), 0xFF7CFFB2.toInt(), 0xFFFFA94D.toInt(), 0xFFE056FD.toInt(),
        0xFF4ADEDE.toInt(), 0xFFB0B0B0.toInt()
    )

    private val prefs: SharedPreferences by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private lateinit var status: TextView
    private lateinit var mainView: LinearLayout
    private lateinit var editorRoot: LinearLayout
    private lateinit var pagesBox: FrameLayout
    private lateinit var galleryPage: View
    private lateinit var settingsPage: View
    private lateinit var preview: PreviewView
    private lateinit var fullPreview: PreviewView
    private lateinit var rootFrame: FrameLayout
    private var maskOverlay: View? = null
    private var maskView: MaskEditView? = null
    private lateinit var editorTitle: TextView
    private lateinit var chipsBox: LinearLayout
    private lateinit var tabsBox: LinearLayout
    private lateinit var ctlBox: LinearLayout

    private val cards = ArrayList<TemplateCard>()
    private val navPills = ArrayList<FrameLayout>()
    private val navIcons = ArrayList<IconView>()
    private val navLabels = ArrayList<TextView>()
    private var modIdx = 0
    private var subIdx = 0
    private var catIdx = 0
    private lateinit var catsBox: LinearLayout
    private lateinit var gridHolder: LinearLayout
    private val cats = arrayOf("همه", "ساده", "رنگی", "ویژه", "انیمه")
    private val catLists = arrayOf<List<Int>?>(null, listOf(0, 2, 3, 10), listOf(1, 5, 6, 7), listOf(4, 8, 9, 11, 12), listOf(13, 14, 15, 16, 17))

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) process(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fontAssets = applicationContext.assets
        window.statusBarColor = cBg
        window.navigationBarColor = cBg
        if (th.light) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        val frame = FrameLayout(this)
        rootFrame = frame
        frame.setBackgroundColor(cBg)

        mainView = LinearLayout(this)
        mainView.orientation = LinearLayout.VERTICAL
        mainView.setBackgroundColor(cBg)

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(20), dp(14), dp(16), dp(6))
        header.addView(tv("DepthClock", 28f, cText, true, Gravity.LEFT or Gravity.CENTER_VERTICAL), LinearLayout.LayoutParams(0, WRAP, 1f))
        header.addView(pill("انتخاب عکس", true) { picker.launch("image/*") }, lp(WRAP, dp(40)))
        mainView.addView(header, lp(MATCH, WRAP))

        pagesBox = FrameLayout(this)
        mainView.addView(pagesBox, LinearLayout.LayoutParams(MATCH, 0, 1f))
        mainView.addView(buildNav(), lp(MATCH, WRAP))

        buildEditor()
        editorRoot.visibility = View.GONE

        galleryPage = buildGalleryPage()
        settingsPage = buildSettingsPage()
        pagesBox.addView(galleryPage, FrameLayout.LayoutParams(MATCH, MATCH))
        pagesBox.addView(settingsPage, FrameLayout.LayoutParams(MATCH, MATCH))

        frame.addView(mainView, FrameLayout.LayoutParams(MATCH, MATCH))
        frame.addView(editorRoot, FrameLayout.LayoutParams(MATCH, MATCH))
        fullPreview = PreviewView(this)
        fullPreview.visibility = View.GONE
        fullPreview.setOnClickListener { fullPreview.visibility = View.GONE }
        frame.addView(fullPreview, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(frame)
        showTab(0)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (maskOverlay != null) {
                    closeMask(false)
                } else if (fullPreview.visibility == View.VISIBLE) {
                    fullPreview.visibility = View.GONE
                } else if (editorRoot.visibility == View.VISIBLE) {
                    closeEditor()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // ------------------------------------------------------------ prefs helpers

    private fun gi(k: String): Int = prefs.getInt(k, (DEFAULTS[k] as? Int) ?: 0)
    private fun gb(k: String): Boolean = prefs.getBoolean(k, (DEFAULTS[k] as? Boolean) ?: false)
    private fun gs(k: String): String = prefs.getString(k, (DEFAULTS[k] as? String) ?: "") ?: ""

    private fun put(k: String, v: Any) {
        val e = prefs.edit()
        when (v) {
            is Int -> e.putInt(k, v)
            is Boolean -> e.putBoolean(k, v)
            is String -> e.putString(k, v)
        }
        e.apply()
        if (this::preview.isInitialized) preview.invalidate()
        if (this::fullPreview.isInitialized) fullPreview.invalidate()
    }

    /** Clears the style settings, then applies template i (if valid). */
    private fun applyTemplate(i: Int) {
        val e = prefs.edit()
        for (k in STYLE_KEYS) e.remove(k)
        if (i >= 0 && i < TEMPLATES.size) {
            for ((k, v) in TEMPLATES[i].cfg) {
                when (v) {
                    is Int -> e.putInt(k, v)
                    is Boolean -> e.putBoolean(k, v)
                    is String -> e.putString(k, v)
                }
            }
        }
        e.putInt("tpl", i)
        e.apply()
        if (this::preview.isInitialized) preview.invalidate()
        refreshCards()
    }

    private fun refreshCards() {
        for (c in cards) c.invalidate()
    }

    // ------------------------------------------------------------ ui helpers

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(w, h)

    private fun rounded(color: Int, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(r).toFloat()
        return g
    }

    private fun roundedTop(color: Int, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        val f = dp(r).toFloat()
        g.cornerRadii = floatArrayOf(f, f, f, f, 0f, 0f, 0f, 0f)
        return g
    }

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false, gravity: Int = Gravity.RIGHT or Gravity.CENTER_VERTICAL): TextView {
        val t = TextView(this)
        t.text = text
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        t.setTextColor(color)
        t.gravity = gravity
        t.textDirection = View.TEXT_DIRECTION_RTL
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun pill(label: String, filled: Boolean, onClick: () -> Unit): TextView {
        val t = tv(label, 14f, if (filled) cOnAccent else cText, true, Gravity.CENTER)
        t.setPadding(dp(22), dp(8), dp(22), dp(8))
        t.background = rounded(if (filled) cAccent else cChip, 24)
        t.setOnClickListener { onClick() }
        return t
    }

    private fun column(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        return l
    }

    private fun hscroll(child: View): HorizontalScrollView {
        val h = HorizontalScrollView(this)
        h.isHorizontalScrollBarEnabled = false
        h.addView(child)
        return h
    }

    private fun say(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun roundButton(icon: Int, onClick: () -> Unit): FrameLayout {
        val f = FrameLayout(this)
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(cChip)
        f.background = bg
        f.addView(IconView(this, icon, cText), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        f.setOnClickListener { onClick() }
        return f
    }

    private fun tint(on: Int, off: Int): ColorStateList = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(on, off)
    )

    // ------------------------------------------------------------ controls

    private fun controlView(c: Ctl): View = when (c) {
        is Tgl -> toggleView(c)
        is Sld -> sliderView(c)
        is Chp -> chipsView(c.label, c.key, c.options, false)
        is Fnt -> chipsView(c.label, c.key, FONT_NAMES, true)
        is Clr -> colorView(c)
        is Txt -> textView(c)
        is Btn -> pill(c.label, true) { if (c.id == 0) picker.launch("image/*") else openMaskEditor() }
        is Prs -> pairView(c)
        is Grd -> grdView(c)
    }

    private fun pairView(c: Prs): View {
        val col = column()
        col.addView(tv(c.label, 14f, cText), lp(MATCH, WRAP))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for (p in PAIRS) {
            val item = column()
            item.gravity = Gravity.CENTER_HORIZONTAL
            item.background = rounded(cChip, 14)
            item.setPadding(dp(8), dp(8), dp(8), dp(6))
            val sw = LinearLayout(this)
            sw.orientation = LinearLayout.HORIZONTAL
            val a = View(this)
            a.setBackgroundColor(p.second)
            val b = View(this)
            b.setBackgroundColor(p.third)
            sw.addView(a, LinearLayout.LayoutParams(dp(24), dp(26)))
            sw.addView(b, LinearLayout.LayoutParams(dp(24), dp(26)))
            roundClip(sw, dp(8).toFloat())
            item.addView(sw, lp(WRAP, WRAP))
            item.addView(tv(p.first, 11f, cMuted, false, Gravity.CENTER), lp(WRAP, WRAP))
            item.setOnClickListener {
                put(c.k1, p.second)
                put(c.k2, p.third)
                rebuildControls()
            }
            val l = lp(WRAP, WRAP)
            l.rightMargin = dp(8)
            row.addView(item, l)
        }
        val l2 = lp(MATCH, WRAP)
        l2.topMargin = dp(8)
        col.addView(hscroll(row), l2)
        return col
    }

    private fun grdView(c: Grd): View {
        val col = column()
        col.addView(tv(c.label, 14f, cText), lp(MATCH, WRAP))
        val box = column()
        for (r in 0..2) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (q in 0..2) {
                val cell = FrameLayout(this)
                cell.background = rounded(cChip, 12)
                val dot = View(this)
                val g = GradientDrawable()
                g.shape = GradientDrawable.OVAL
                g.setColor(cMuted)
                dot.background = g
                cell.addView(dot, FrameLayout.LayoutParams(dp(8), dp(8), Gravity.CENTER))
                cell.setOnClickListener {
                    put(c.kx, c.xs[q])
                    put(c.ky, c.ys[r])
                    rebuildControls()
                }
                val l = LinearLayout.LayoutParams(0, dp(44), 1f)
                l.setMargins(dp(3), dp(3), dp(3), dp(3))
                row.addView(cell, l)
            }
            box.addView(row, lp(MATCH, WRAP))
        }
        val l2 = lp(MATCH, WRAP)
        l2.topMargin = dp(6)
        col.addView(box, l2)
        return col
    }

    private fun toggleView(c: Tgl): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val sw = Switch(this)
        sw.isChecked = gb(c.key)
        sw.thumbTintList = tint(cAccent, cMuted)
        sw.trackTintList = tint(cPill, cChip)
        sw.setOnCheckedChangeListener { _, on -> put(c.key, on) }
        row.addView(sw, lp(WRAP, WRAP))
        row.addView(tv(c.label, 15f, cText), LinearLayout.LayoutParams(0, WRAP, 1f))
        return row
    }

    private fun sliderView(c: Sld): View {
        val col = column()
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        val value = tv("", 13f, cAccent, true, Gravity.LEFT or Gravity.CENTER_VERTICAL)
        head.addView(value, lp(WRAP, WRAP))
        head.addView(tv(c.label, 14f, cText), LinearLayout.LayoutParams(0, WRAP, 1f))
        col.addView(head, lp(MATCH, WRAP))

        val seek = SeekBar(this)
        seek.max = c.max - c.min
        seek.progress = (gi(c.key) - c.min).coerceIn(0, c.max - c.min)
        seek.progressTintList = ColorStateList.valueOf(cAccent)
        seek.thumbTintList = ColorStateList.valueOf(cAccent)
        seek.progressBackgroundTintList = ColorStateList.valueOf(cChip)
        value.text = "${c.min + seek.progress - c.offset}${c.unit}"
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                put(c.key, c.min + p)
                value.text = "${c.min + p - c.offset}${c.unit}"
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        val step = max(1, (c.max - c.min) / 50)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(roundButton(10) { seek.progress = max(0, seek.progress - step) }, lp(dp(34), dp(34)))
        row.addView(seek, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(roundButton(9) { seek.progress = min(seek.max, seek.progress + step) }, lp(dp(34), dp(34)))
        col.addView(row, lp(MATCH, WRAP))
        return col
    }

    private fun chipsView(label: String, key: String, opts: List<String>, fonts: Boolean): View {
        val col = column()
        col.addView(tv(label, 14f, cText), lp(MATCH, WRAP))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val items = ArrayList<TextView>()
        fun refresh() {
            val cur = gi(key)
            for (i in items.indices) {
                items[i].background = rounded(if (i == cur) cPill else cChip, 18)
                items[i].setTextColor(if (i == cur) cAccent else cMuted)
            }
        }
        for (i in opts.indices) {
            val t = tv(opts[i], 13f, cMuted, false, Gravity.CENTER)
            if (fonts) t.typeface = fontFor(i)
            t.setPadding(dp(14), dp(8), dp(14), dp(8))
            t.setOnClickListener {
                put(key, i)
                refresh()
            }
            items.add(t)
            val l = lp(WRAP, WRAP)
            l.rightMargin = dp(8)
            row.addView(t, l)
        }
        refresh()
        val sc = hscroll(row)
        val l2 = lp(MATCH, WRAP)
        l2.topMargin = dp(8)
        col.addView(sc, l2)
        return col
    }

    private fun colorView(c: Clr): View {
        val col = column()
        col.addView(tv(c.label, 14f, cText), lp(MATCH, WRAP))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val dots = ArrayList<View>()
        fun refresh() {
            val cur = gi(c.key)
            for (i in dots.indices) {
                val g = GradientDrawable()
                g.shape = GradientDrawable.OVAL
                g.setColor(palette[i])
                g.setStroke(dp(if (palette[i] == cur) 3 else 1), if (palette[i] == cur) cAccent else cChip)
                dots[i].background = g
            }
        }
        for (i in palette.indices) {
            val d = View(this)
            d.setOnClickListener {
                put(c.key, palette[i])
                refresh()
            }
            dots.add(d)
            val l = lp(dp(34), dp(34))
            l.rightMargin = dp(8)
            row.addView(d, l)
        }
        val hex = tv("#", 16f, cText, true, Gravity.CENTER)
        hex.background = rounded(cChip, 17)
        hex.setOnClickListener { askHex(c.key) { refresh() } }
        row.addView(hex, lp(dp(34), dp(34)))
        refresh()
        val l2 = lp(MATCH, WRAP)
        l2.topMargin = dp(8)
        col.addView(hscroll(row), l2)
        return col
    }

    private fun askHex(key: String, done: () -> Unit) {
        val e = EditText(this)
        e.hint = "مثلاً FF8800"
        e.setSingleLine()
        AlertDialog.Builder(this)
            .setTitle("کد رنگ")
            .setView(e)
            .setPositiveButton("تأیید") { _, _ ->
                try {
                    val s = e.text.toString().trim().removePrefix("#")
                    put(key, Color.parseColor("#$s"))
                    done()
                } catch (ex: Exception) {
                    say("کد رنگ درست نیست")
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun textView(c: Txt): View {
        val col = column()
        col.addView(tv(c.label, 14f, cText), lp(MATCH, WRAP))
        val e = EditText(this)
        e.setText(gs(c.key))
        e.hint = "متن را اینجا بنویس"
        e.setTextColor(cText)
        e.setHintTextColor(cMuted)
        e.background = rounded(cChip, 14)
        e.setPadding(dp(14), dp(10), dp(14), dp(10))
        e.inputType = InputType.TYPE_CLASS_TEXT
        e.setSingleLine()
        e.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                put(c.key, s?.toString() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
        })
        val l = lp(MATCH, WRAP)
        l.topMargin = dp(8)
        col.addView(e, l)
        return col
    }

    // ------------------------------------------------------------ editor

    private fun buildEditor() {
        editorRoot = LinearLayout(this)
        editorRoot.orientation = LinearLayout.VERTICAL
        editorRoot.setBackgroundColor(cBg)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(10), dp(10), dp(10), dp(6))
        bar.addView(roundButton(8) { closeEditor() }, lp(dp(40), dp(40)))
        editorTitle = tv("استودیو", 17f, cText, true, Gravity.LEFT or Gravity.CENTER_VERTICAL)
        editorTitle.setPadding(dp(10), 0, 0, 0)
        bar.addView(editorTitle, LinearLayout.LayoutParams(0, WRAP, 1f))

        val bm = dp(6)
        val b1 = lp(dp(40), dp(40))
        b1.leftMargin = bm
        bar.addView(roundButton(0) { pickTemplate() }, b1)
        val pv = pill("پیش‌نمایش", false) { fullPreview.visibility = View.VISIBLE }
        pv.setPadding(dp(12), dp(6), dp(12), dp(6))
        val b2 = lp(WRAP, dp(40))
        b2.leftMargin = bm
        bar.addView(pv, b2)
        val b3 = lp(dp(40), dp(40))
        b3.leftMargin = bm
        bar.addView(roundButton(7) {
            applyTemplate(gi("tpl"))
            rebuildControls()
        }, b3)
        val ok = roundButton(11) { setWallpaper() }
        val okBg = GradientDrawable()
        okBg.shape = GradientDrawable.OVAL
        okBg.setColor(cAccent)
        ok.background = okBg
        (ok.getChildAt(0) as IconView).setTint(cOnAccent)
        val b4 = lp(dp(40), dp(40))
        b4.leftMargin = bm
        bar.addView(ok, b4)
        editorRoot.addView(bar, lp(MATCH, WRAP))

        val dm = resources.displayMetrics
        val ph = (dm.heightPixels * 0.36f).toInt()
        val pw = (ph.toFloat() * dm.widthPixels / dm.heightPixels).toInt()
        preview = PreviewView(this)
        roundClip(preview, dp(22).toFloat())
        val pl = lp(pw, ph)
        pl.gravity = Gravity.CENTER_HORIZONTAL
        pl.topMargin = dp(4)
        pl.bottomMargin = dp(10)
        editorRoot.addView(preview, pl)

        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.background = roundedTop(cPanel, 26)
        panel.setPadding(dp(14), dp(4), dp(14), 0)

        tabsBox = LinearLayout(this)
        tabsBox.orientation = LinearLayout.HORIZONTAL
        panel.addView(hscroll(tabsBox), lp(MATCH, WRAP))
        val line = View(this)
        line.setBackgroundColor(cChip)
        panel.addView(line, lp(MATCH, 1))

        chipsBox = LinearLayout(this)
        chipsBox.orientation = LinearLayout.HORIZONTAL
        chipsBox.gravity = Gravity.CENTER_VERTICAL
        val cl = lp(MATCH, WRAP)
        cl.topMargin = dp(10)
        cl.bottomMargin = dp(8)
        panel.addView(hscroll(chipsBox), cl)

        ctlBox = column()
        ctlBox.setPadding(0, dp(4), 0, dp(24))
        val sc = ScrollView(this)
        sc.isVerticalScrollBarEnabled = false
        sc.addView(ctlBox)
        panel.addView(sc, LinearLayout.LayoutParams(MATCH, 0, 1f))

        editorRoot.addView(panel, LinearLayout.LayoutParams(MATCH, 0, 1f))
    }

    private fun pickTemplate() {
        val names = Array<CharSequence>(TEMPLATES.size) { TEMPLATES[it].name }
        AlertDialog.Builder(this)
            .setTitle("قالب‌ها")
            .setItems(names) { _, which ->
                applyTemplate(which)
                rebuildControls()
            }
            .show()
    }

    private fun openEditor() {
        mainView.visibility = View.GONE
        editorRoot.visibility = View.VISIBLE
        modIdx = 0
        subIdx = 0
        selectModule(0)
    }

    private fun closeEditor() {
        editorRoot.visibility = View.GONE
        mainView.visibility = View.VISIBLE
        refreshCards()
    }

    private fun selectModule(i: Int) {
        val oldName = MODULES[modIdx].subs.getOrNull(subIdx)?.name
        modIdx = i
        val subs = MODULES[i].subs
        var ni = 0
        for (k in subs.indices) {
            if (subs[k].name == oldName) ni = k
        }
        subIdx = ni
        rebuildChips()
        rebuildTabs()
        rebuildControls()
    }

    private fun rebuildChips() {
        chipsBox.removeAllViews()
        for (i in MODULES.indices) {
            val m = MODULES[i]
            val sel = i == modIdx
            val item = LinearLayout(this)
            item.orientation = LinearLayout.HORIZONTAL
            item.gravity = Gravity.CENTER
            item.background = rounded(if (sel) cPill else cChip, 22)
            item.setPadding(dp(14), dp(8), dp(16), dp(8))
            item.setOnClickListener { selectModule(i) }
            item.addView(IconView(this, m.icon, if (sel) cAccent else cMuted), lp(dp(20), dp(20)))
            val l = lp(WRAP, WRAP)
            l.leftMargin = dp(6)
            item.addView(tv(m.name, 13f, if (sel) cAccent else cMuted, sel, Gravity.CENTER), l)
            val il = lp(WRAP, WRAP)
            il.rightMargin = dp(8)
            chipsBox.addView(item, il)
        }
    }

    private fun rebuildTabs() {
        tabsBox.removeAllViews()
        val subs = MODULES[modIdx].subs
        for (i in subs.indices) {
            val sel = i == subIdx
            val item = column()
            item.gravity = Gravity.CENTER_HORIZONTAL
            val t = tv(subs[i].name, 14f, if (sel) cAccent else cMuted, sel, Gravity.CENTER)
            t.setPadding(dp(14), dp(12), dp(14), dp(10))
            item.addView(t, lp(WRAP, WRAP))
            val u = View(this)
            u.setBackgroundColor(if (sel) cAccent else 0)
            item.addView(u, lp(MATCH, dp(3)))
            item.setOnClickListener {
                subIdx = i
                rebuildTabs()
                rebuildControls()
            }
            tabsBox.addView(item, lp(WRAP, WRAP))
        }
    }

    private fun rebuildControls() {
        ctlBox.removeAllViews()
        for (c in MODULES[modIdx].subs[subIdx].ctls) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.background = rounded(cRow, 16)
            card.setPadding(dp(14), dp(12), dp(14), dp(12))
            card.addView(controlView(c), lp(MATCH, WRAP))
            val l = lp(MATCH, WRAP)
            l.bottomMargin = dp(10)
            ctlBox.addView(card, l)
        }
    }

    // ------------------------------------------------------------ gallery + settings + nav

    private fun buildGalleryPage(): View {
        val sc = ScrollView(this)
        sc.isVerticalScrollBarEnabled = false
        val col = column()
        col.setPadding(dp(16), 0, dp(16), dp(24))

        catsBox = LinearLayout(this)
        catsBox.orientation = LinearLayout.HORIZONTAL
        col.addView(catsBox, lp(MATCH, WRAP))
        val line = View(this)
        line.setBackgroundColor(cChip)
        col.addView(line, lp(MATCH, 1))

        status = tv("یک عکس انتخاب کنید (بهتر است عکس یک آدم باشد).", 13f, cMuted)
        status.setPadding(dp(4), dp(14), dp(4), dp(12))
        col.addView(status, lp(MATCH, WRAP))

        gridHolder = column()
        col.addView(gridHolder, lp(MATCH, WRAP))

        val wl = lp(MATCH, dp(50))
        wl.topMargin = dp(8)
        col.addView(pill("تنظیم به‌عنوان والپیپر", true) { setWallpaper() }, wl)

        rebuildCats()
        fillGrid()
        sc.addView(col)
        return sc
    }

    private fun rebuildCats() {
        catsBox.removeAllViews()
        for (i in cats.indices) {
            val sel = i == catIdx
            val item = column()
            item.gravity = Gravity.CENTER_HORIZONTAL
            val t = tv(cats[i], 15f, if (sel) cAccent else cMuted, sel, Gravity.CENTER)
            t.setPadding(0, dp(12), 0, dp(10))
            item.addView(t, lp(WRAP, WRAP))
            val u = View(this)
            u.setBackgroundColor(if (sel) cAccent else 0)
            item.addView(u, lp(MATCH, dp(3)))
            item.setOnClickListener {
                catIdx = i
                rebuildCats()
                fillGrid()
            }
            catsBox.addView(item, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
    }

    private fun fillGrid() {
        gridHolder.removeAllViews()
        cards.clear()
        val dm = resources.displayMetrics
        val cw = (dm.widthPixels - dp(16) * 2 - dp(12)) / 2
        val ch = (cw * dm.heightPixels.toFloat() / dm.widthPixels).toInt()
        val items = ArrayList<Int>()
        val sel = catLists[catIdx]
        if (sel == null) {
            items.add(-1)
            for (i in TEMPLATES.indices) items.add(i)
        } else {
            for (i in sel) items.add(i)
        }
        var i = 0
        while (i < items.size) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (j in 0..1) {
                val k = i + j
                if (k < items.size) {
                    val idx = items[k]
                    val card = TemplateCard(this, idx, if (idx < 0) "طرح من" else TEMPLATES[idx].name, prefs, cAccent, cOnAccent)
                    roundClip(card, dp(28).toFloat())
                    card.setOnClickListener {
                        if (idx >= 0) applyTemplate(idx) else put("tpl", -1)
                        refreshCards()
                        openEditor()
                    }
                    cards.add(card)
                    val l = lp(cw, ch)
                    if (j == 0) l.rightMargin = dp(12)
                    row.addView(card, l)
                }
            }
            val rl = lp(MATCH, WRAP)
            rl.bottomMargin = dp(12)
            gridHolder.addView(row, rl)
            i += 2
        }
    }

    private fun buildSettingsPage(): View {
        val sc = ScrollView(this)
        sc.isVerticalScrollBarEnabled = false
        val col = column()
        col.setPadding(dp(14), dp(8), dp(14), dp(16))

        val c1 = column()
        c1.background = rounded(cCard, 18)
        c1.setPadding(dp(16), dp(14), dp(16), dp(14))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val sw = Switch(this)
        sw.isChecked = prefs.getBoolean(KEY_MOTION, false)
        sw.thumbTintList = tint(cAccent, cMuted)
        sw.trackTintList = tint(cPill, cChip)
        sw.setOnCheckedChangeListener { _, on ->
            put(KEY_MOTION, on)
            refreshCards()
        }
        row.addView(sw, lp(WRAP, WRAP))
        row.addView(tv("حرکت با کج کردن گوشی", 15f, cText), LinearLayout.LayoutParams(0, WRAP, 1f))
        c1.addView(row, lp(MATCH, WRAP))
        c1.addView(tv("اگر خاموش باشد، والپیپر ثابت است و حس سه‌بعدی فقط از لایه‌بندی می‌آید. روشن بودنش باتری بیشتری مصرف می‌کند.", 12f, cMuted), lp(MATCH, WRAP))
        col.addView(c1, lp(MATCH, WRAP))

        val cT = column()
        cT.background = rounded(cCard, 18)
        cT.setPadding(dp(16), dp(14), dp(16), dp(14))
        cT.addView(tv("تم برنامه", 15f, cText, true), lp(MATCH, WRAP))
        cT.addView(tv("رنگ و ظاهر کل برنامه را انتخاب کن.", 12f, cMuted), lp(MATCH, WRAP))
        val themeRow = LinearLayout(this)
        themeRow.orientation = LinearLayout.HORIZONTAL
        val curTheme = prefs.getInt("theme", 0).coerceIn(0, THEMES.size - 1)
        for (i in THEMES.indices) {
            val t = THEMES[i]
            val item = column()
            item.gravity = Gravity.CENTER_HORIZONTAL
            val dot = View(this)
            val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(t.bg, t.accent))
            g.shape = GradientDrawable.OVAL
            g.setStroke(dp(if (i == curTheme) 3 else 1), if (i == curTheme) cAccent else cChip)
            dot.background = g
            item.addView(dot, lp(dp(46), dp(46)))
            item.addView(tv(t.name, 11f, if (i == curTheme) cAccent else cMuted, i == curTheme, Gravity.CENTER), lp(WRAP, WRAP))
            item.setOnClickListener {
                prefs.edit().putInt("theme", i).apply()
                recreate()
            }
            val il = lp(WRAP, WRAP)
            il.rightMargin = dp(14)
            themeRow.addView(item, il)
        }
        val tl = lp(MATCH, WRAP)
        tl.topMargin = dp(12)
        cT.addView(hscroll(themeRow), tl)
        col.addView(cT, lp(MATCH, WRAP))

        val c2 = column()
        c2.background = rounded(cCard, 18)
        c2.setPadding(dp(16), dp(14), dp(16), dp(14))
        c2.addView(tv("بازنشانی همه تنظیمات", 15f, cText), lp(MATCH, WRAP))
        c2.addView(tv("فونت‌ها، رنگ‌ها و موقعیت‌ها به حالت اول برمی‌گردند. عکس شما پاک نمی‌شود.", 12f, cMuted), lp(MATCH, WRAP))
        val rl = lp(WRAP, dp(40))
        rl.topMargin = dp(10)
        rl.gravity = Gravity.RIGHT
        c2.addView(pill("بازنشانی", false) {
            val keepTheme = prefs.getInt("theme", 0)
            prefs.edit().clear().putInt("theme", keepTheme).apply()
            refreshCards()
            say("همه تنظیمات بازنشانی شد")
        }, rl)
        val l2 = lp(MATCH, WRAP)
        l2.topMargin = dp(12)
        col.addView(c2, l2)

        val c3 = column()
        c3.background = rounded(cCard, 18)
        c3.setPadding(dp(16), dp(14), dp(16), dp(14))
        c3.addView(tv("نکته‌ها", 15f, cText, true), lp(MATCH, WRAP))
        c3.addView(tv("• جداسازی سوژه فقط برای آدم کار می‌کند و آفلاین است.\n• ساعت پشت سوژه قرار می‌گیرد، پس موقعیتش را طوری تنظیم کن که بخشی از آن پشت سر یا بدن باشد.\n• عکس‌های واضح و با پس‌زمینه ساده بهتر بریده می‌شوند.", 12f, cMuted), lp(MATCH, WRAP))
        val l3 = lp(MATCH, WRAP)
        l3.topMargin = dp(12)
        col.addView(c3, l3)

        sc.addView(col)
        return sc
    }

    private fun buildNav(): View {
        val wrap = FrameLayout(this)
        wrap.setBackgroundColor(cBg)
        wrap.setPadding(dp(40), dp(8), dp(40), dp(16))
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER
        bar.background = rounded(cNav, 34)
        bar.setPadding(dp(8), dp(8), dp(8), dp(8))
        val icons = intArrayOf(0, 2)
        for (i in 0..1) {
            val pillBox = FrameLayout(this)
            val icon = IconView(this, icons[i], cMuted)
            pillBox.addView(icon, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
            pillBox.setOnClickListener { showTab(i) }
            navPills.add(pillBox)
            navIcons.add(icon)
            bar.addView(pillBox, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        wrap.addView(bar, FrameLayout.LayoutParams(MATCH, WRAP))
        return wrap
    }

    private fun showTab(i: Int) {
        galleryPage.visibility = if (i == 0) View.VISIBLE else View.GONE
        settingsPage.visibility = if (i == 1) View.VISIBLE else View.GONE
        for (k in navPills.indices) {
            val sel = k == i
            navPills[k].background = if (sel) rounded(cAccent, 24) else null
            navIcons[k].setTint(if (sel) cOnAccent else cMuted)
        }
    }

    // ------------------------------------------------------------ wallpaper + photo processing

    // ------------------------------------------------------------ manual cut-out touch-up

    private fun openMaskEditor() {
        if (maskOverlay != null) return
        val bf = File(filesDir, BG_FILE)
        val ff = File(filesDir, FG_FILE)
        if (!bf.exists() || !ff.exists()) {
            say("اول یک عکس انتخاب کن")
            return
        }
        val o = BitmapFactory.Options()
        o.inMutable = true
        val bg = BitmapFactory.decodeFile(bf.path)
        val fg = BitmapFactory.decodeFile(ff.path, o)
        if (bg == null || fg == null) {
            say("خواندن عکس ممکن نشد")
            return
        }
        val mv = MaskEditView(this, bg, fg)
        maskView = mv

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(cBg)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(10), dp(10), dp(12), dp(8))
        bar.addView(roundButton(8) { closeMask(false) }, lp(dp(40), dp(40)))
        val title = tv("اصلاح دستی برش", 17f, cText, true, Gravity.LEFT or Gravity.CENTER_VERTICAL)
        title.setPadding(dp(12), 0, 0, 0)
        bar.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        bar.addView(pill("ذخیره", true) { closeMask(true) }, lp(WRAP, dp(40)))
        root.addView(bar, lp(MATCH, WRAP))

        root.addView(mv, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val panel = column()
        panel.background = roundedTop(cPanel, 26)
        panel.setPadding(dp(16), dp(14), dp(16), dp(16))

        val modes = LinearLayout(this)
        modes.orientation = LinearLayout.HORIZONTAL
        val eraseBtn = pill("پاک کردن", true) {}
        val backBtn = pill("برگرداندن", false) {}
        val undoBtn = pill("واگرد", false) { mv.undo() }
        fun refreshModes() {
            eraseBtn.background = rounded(if (mv.erase) cAccent else cChip, 24)
            eraseBtn.setTextColor(if (mv.erase) cOnAccent else cText)
            backBtn.background = rounded(if (mv.erase) cChip else cAccent, 24)
            backBtn.setTextColor(if (mv.erase) cText else cOnAccent)
        }
        eraseBtn.setOnClickListener {
            mv.erase = true
            refreshModes()
        }
        backBtn.setOnClickListener {
            mv.erase = false
            refreshModes()
        }
        refreshModes()
        val ml = lp(WRAP, dp(40))
        ml.rightMargin = dp(8)
        modes.addView(eraseBtn, ml)
        val ml2 = lp(WRAP, dp(40))
        ml2.rightMargin = dp(8)
        modes.addView(backBtn, ml2)
        modes.addView(undoBtn, lp(WRAP, dp(40)))
        panel.addView(modes, lp(MATCH, WRAP))

        val sl = lp(MATCH, WRAP)
        sl.topMargin = dp(12)
        panel.addView(tv("اندازه قلم", 14f, cText), sl)
        val seek = SeekBar(this)
        seek.max = 194
        seek.progress = 34
        seek.progressTintList = ColorStateList.valueOf(cAccent)
        seek.thumbTintList = ColorStateList.valueOf(cAccent)
        seek.progressBackgroundTintList = ColorStateList.valueOf(cChip)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                mv.brush = (p + 6).toFloat()
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        panel.addView(seek, lp(MATCH, WRAP))
        panel.addView(
            tv("با «پاک کردن» قسمت‌های اضافه را پاک کن. با «برگرداندن» قسمت‌هایی که اشتباهی پاک شده‌اند را برگردان. قسمت کم‌رنگ، بخش بریده‌شده است.", 12f, cMuted),
            lp(MATCH, WRAP)
        )
        root.addView(panel, lp(MATCH, WRAP))

        rootFrame.addView(root, FrameLayout.LayoutParams(MATCH, MATCH))
        maskOverlay = root
    }

    private fun closeMask(save: Boolean) {
        val root = maskOverlay ?: return
        val mv = maskView
        if (save && mv != null) {
            say("در حال ذخیره...")
            val fgBmp = mv.fg
            Thread {
                try {
                    val tmp = File(filesDir, "fg.tmp")
                    FileOutputStream(tmp).use { fgBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    tmp.renameTo(File(filesDir, FG_FILE))
                    runOnUiThread {
                        rootFrame.removeView(root)
                        maskOverlay = null
                        maskView = null
                        refreshCards()
                        preview.invalidate()
                        say("ذخیره شد")
                    }
                } catch (e: Throwable) {
                    runOnUiThread { say("خطا در ذخیره: ${e.message}") }
                }
            }.start()
        } else {
            rootFrame.removeView(root)
            maskOverlay = null
            maskView = null
        }
    }

    private fun setWallpaper() {
        val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, DepthWallpaperService::class.java)
        )
        try {
            startActivity(i)
        } catch (e: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    private fun process(uri: Uri) {
        status.text = "در حال پردازش عکس..."
        Thread {
            try {
                val bmp = loadScaled(uri, 1280)
                runOnUiThread { segment(bmp) }
            } catch (e: Throwable) {
                runOnUiThread { status.text = "خطا در خواندن عکس: ${e.message}" }
            }
        }.start()
    }

    /** First try the general "subject" model (people, animals, characters, objects), then fall back to people-only. */
    private fun segment(bmp: Bitmap) {
        status.text = "در حال جداسازی سوژه... (بار اول مدل دانلود می‌شود و اینترنت لازم است)"
        val sOpts = SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .build()
        SubjectSegmentation.getClient(sOpts)
            .process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { res ->
                val fm = res.foregroundConfidenceMask
                val n = bmp.width * bmp.height
                if (fm != null && fm.remaining() >= n) {
                    fm.rewind()
                    val conf = FloatArray(n)
                    fm.get(conf, 0, n)
                    var hit = 0
                    for (v in conf) if (v > 0.5f) hit++
                    if (hit > n / 100) {
                        Thread { save(bmp, bmp.width, bmp.height, conf) }.start()
                    } else {
                        segmentPerson(bmp)
                    }
                } else {
                    segmentPerson(bmp)
                }
            }
            .addOnFailureListener { segmentPerson(bmp) }
    }

    private fun segmentPerson(bmp: Bitmap) {
        val opts = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .enableRawSizeMask()
            .build()
        Segmentation.getClient(opts)
            .process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { mask ->
                val mw = mask.width
                val mh = mask.height
                val buf = mask.buffer
                buf.rewind()
                val conf = FloatArray(mw * mh) { buf.float }
                Thread { save(bmp, mw, mh, conf) }.start()
            }
            .addOnFailureListener { status.text = "خطا در جداسازی سوژه: ${it.message}" }
    }

    private fun save(bmp: Bitmap, mw: Int, mh: Int, conf: FloatArray) {
        try {
            val pix = IntArray(mw * mh) { i ->
                val t = ((conf[i] - 0.35f) / 0.3f).coerceIn(0f, 1f)
                val a = (t * t * (3f - 2f * t) * 255f).toInt()
                (a shl 24) or 0x00FFFFFF
            }
            val m = Bitmap.createBitmap(mw, mh, Bitmap.Config.ARGB_8888)
            m.setPixels(pix, 0, mw, 0, 0, mw, mh)
            val ms = Bitmap.createScaledBitmap(m, bmp.width, bmp.height, true)

            val cut = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(cut)
            c.drawBitmap(bmp, 0f, 0f, null)
            val p = Paint()
            p.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            c.drawBitmap(ms, 0f, 0f, p)

            // Write temp files first, then rename, so the wallpaper never reads a half-written file.
            val bgTmp = File(filesDir, "bg.tmp")
            val fgTmp = File(filesDir, "fg.tmp")
            FileOutputStream(bgTmp).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            FileOutputStream(fgTmp).use { cut.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bgTmp.renameTo(File(filesDir, BG_FILE))
            fgTmp.renameTo(File(filesDir, FG_FILE))

            runOnUiThread {
                status.text = "آماده است. یک قالب انتخاب کن و بعد «تنظیم به‌عنوان والپیپر» را بزن."
                refreshCards()
                preview.invalidate()
            }
        } catch (e: Throwable) {
            runOnUiThread { status.text = "خطا در ذخیره: ${e.message}" }
        }
    }

    private fun loadScaled(uri: Uri, maxSide: Int): Bitmap {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        val o = BitmapFactory.Options()
        o.inSampleSize = sample
        var bmp = contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, o)!! }
        val rot = try {
            contentResolver.openInputStream(uri)!!.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                    6 -> 90f
                    3 -> 180f
                    8 -> 270f
                    else -> 0f
                }
            }
        } catch (e: Exception) {
            0f
        }
        val sc = min(1f, maxSide.toFloat() / max(bmp.width, bmp.height))
        if (rot != 0f || sc < 1f) {
            val m = Matrix()
            m.postRotate(rot)
            m.postScale(sc, sc)
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        return bmp
    }
}
