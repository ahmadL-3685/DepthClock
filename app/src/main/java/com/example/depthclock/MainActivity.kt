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
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

// ---------------------------------------------------------------- editor model

internal sealed class Ctl(val label: String)
internal class Tgl(label: String, val key: String) : Ctl(label)
internal class Sld(label: String, val key: String, val min: Int, val max: Int, val offset: Int = 0, val unit: String = "") : Ctl(label)
internal class Chp(label: String, val key: String, val options: List<String>) : Ctl(label)
internal class Fnt(label: String, val key: String) : Ctl(label)
internal class Clr(label: String, val key: String) : Ctl(label)
internal class Txt(label: String, val key: String) : Ctl(label)
internal class Btn(label: String, val id: Int) : Ctl(label)

internal class Sub(val name: String, val ctls: List<Ctl>)
internal class EdModule(val name: String, val icon: Int, val subs: List<Sub>)

internal val MODULES: List<EdModule> = listOf(
    EdModule("ساعت", 3, listOf(
        Sub("قالب‌بندی", listOf(
            Tgl("نمایش ساعت", "clk_on"),
            Chp("فرمت ساعت", "clk_hf", listOf("سیستم", "۲۴ ساعته", "۱۲ ساعته")),
            Chp("چیدمان", "clk_layout", listOf("دو خطی", "یک خطی"))
        )),
        Sub("تایپوگرافی", listOf(
            Fnt("فونت", "clk_font"),
            Sld("اندازه", "clk_size", 10, 100),
            Sld("فاصله حروف", "clk_ls", 0, 100),
            Sld("فاصله خطوط", "clk_lh", 0, 100)
        )),
        Sub("رنگ‌ها", listOf(
            Clr("رنگ ساعت", "clk_c1"),
            Clr("رنگ دقیقه", "clk_c2"),
            Sld("شفافیت", "clk_op", 10, 100, 0, "%")
        )),
        Sub("موقعیت", listOf(
            Sld("افقی", "clk_x", 0, 100),
            Sld("عمودی", "clk_y", 0, 100)
        )),
        Sub("افکت‌ها", listOf(
            Tgl("سایه", "clk_sh"),
            Sld("شدت سایه", "clk_shs", 0, 100),
            Sld("ضخامت حاشیه", "clk_ow", 0, 100),
            Clr("رنگ حاشیه", "clk_oc")
        )),
        Sub("تبدیل", listOf(
            Sld("چرخش", "clk_rot", 0, 90, 45, "°"),
            Sld("کشیدگی", "clk_str", 50, 150, 0, "%")
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
            Sld("اندازه", "dt_size", 10, 100),
            Sld("فاصله حروف", "dt_ls", 0, 100),
            Sld("فاصله از ساعت", "dt_gap", 0, 100)
        )),
        Sub("رنگ‌ها", listOf(
            Clr("رنگ تاریخ", "dt_c")
        ))
    )),
    EdModule("متن", 5, listOf(
        Sub("محتوا", listOf(
            Tgl("نمایش متن", "tx_on"),
            Txt("متن دلخواه", "tx_text"),
            Fnt("فونت", "tx_font")
        )),
        Sub("ظاهر", listOf(
            Sld("اندازه", "tx_size", 10, 100),
            Clr("رنگ", "tx_c"),
            Sld("شفافیت", "tx_op", 10, 100, 0, "%")
        )),
        Sub("موقعیت", listOf(
            Sld("افقی", "tx_x", 0, 100),
            Sld("عمودی", "tx_y", 0, 100)
        ))
    )),
    EdModule("عکس", 6, listOf(
        Sub("انتخاب", listOf(
            Btn("انتخاب عکس جدید", 0)
        )),
        Sub("رنگ", listOf(
            Sld("رنگ‌مایه (Hue)", "ph_hue", 0, 360, 180, "°"),
            Sld("اشباع", "ph_sat", 0, 200, 0, "%"),
            Sld("روشنایی", "ph_bri", 0, 200, 0, "%"),
            Sld("کنتراست", "ph_con", 50, 150, 0, "%"),
            Sld("تیرگی پس‌زمینه", "ph_dim", 0, 80, 0, "%")
        )),
        Sub("موقعیت", listOf(
            Sld("بزرگ‌نمایی", "ph_zoom", 0, 100),
            Sld("جابه‌جایی افقی", "ph_px", 0, 100),
            Sld("جابه‌جایی عمودی", "ph_py", 0, 100)
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
            else -> {
                c.drawLine(cx - s * 0.3f, cy, cx + s * 0.3f, cy, p)
            }
        }
    }
}

/** One template card. idx -1 = "my design" (the current settings). */
internal class TemplateCard(
    ctx: Context,
    val idx: Int,
    private val title: String,
    private val prefs: SharedPreferences
) : View(ctx) {
    private val renderer = DepthRenderer(ctx.applicationContext)
    private val scrim = Paint()
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }
    private val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB7C3FF.toInt() }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF141B3A.toInt()
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
        c.drawRect(0f, 0f, w, h, scrim)
        namePaint.textSize = w * 0.085f
        c.drawText(title, w * 0.07f, h - w * 0.07f, namePaint)
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

    private val cBg = 0xFF0D1017.toInt()
    private val cNav = 0xFF141824.toInt()
    private val cCard = 0xFF171B25.toInt()
    private val cPanel = 0xFF1A1E27.toInt()
    private val cRow = 0xFF151922.toInt()
    private val cChip = 0xFF262A33.toInt()
    private val cPill = 0xFF2B3556.toInt()
    private val cText = 0xFFF1F3F8.toInt()
    private val cMuted = 0xFF8D94A6.toInt()
    private val cAccent = 0xFFB7C3FF.toInt()
    private val cOnAccent = 0xFF141B3A.toInt()

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

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) process(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = cBg
        window.navigationBarColor = cNav

        val frame = FrameLayout(this)
        frame.setBackgroundColor(cBg)

        mainView = LinearLayout(this)
        mainView.orientation = LinearLayout.VERTICAL
        mainView.setBackgroundColor(cBg)

        val header = tv("DepthClock", 20f, cText, true, Gravity.LEFT or Gravity.CENTER_VERTICAL)
        header.setPadding(dp(18), dp(14), dp(18), dp(6))
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
        setContentView(frame)
        showTab(0)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (editorRoot.visibility == View.VISIBLE) {
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
        is Btn -> pill(c.label, true) { if (c.id == 0) picker.launch("image/*") }
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
        bar.setPadding(dp(10), dp(10), dp(12), dp(6))
        bar.addView(roundButton(8) { closeEditor() }, lp(dp(40), dp(40)))
        editorTitle = tv("", 16f, cText, true, Gravity.CENTER)
        bar.addView(editorTitle, LinearLayout.LayoutParams(0, WRAP, 1f))
        bar.addView(pill("اعمال", true) { setWallpaper() }, lp(WRAP, dp(38)))
        editorRoot.addView(bar, lp(MATCH, WRAP))

        val dm = resources.displayMetrics
        val ph = (dm.heightPixels * 0.38f).toInt()
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
        panel.setPadding(dp(14), dp(14), dp(14), 0)

        val chipRow = LinearLayout(this)
        chipRow.orientation = LinearLayout.HORIZONTAL
        chipRow.gravity = Gravity.CENTER_VERTICAL
        chipsBox = LinearLayout(this)
        chipsBox.orientation = LinearLayout.HORIZONTAL
        chipsBox.gravity = Gravity.CENTER_VERTICAL
        chipsBox.background = rounded(cRow, 26)
        chipsBox.setPadding(dp(4), dp(4), dp(4), dp(4))
        chipRow.addView(chipsBox, LinearLayout.LayoutParams(0, WRAP, 1f))
        val rl = lp(dp(44), dp(44))
        rl.leftMargin = dp(10)
        chipRow.addView(roundButton(7) {
            applyTemplate(gi("tpl"))
            rebuildControls()
        }, rl)
        panel.addView(chipRow, lp(MATCH, WRAP))

        tabsBox = LinearLayout(this)
        tabsBox.orientation = LinearLayout.HORIZONTAL
        val tl = lp(MATCH, WRAP)
        tl.topMargin = dp(10)
        tl.bottomMargin = dp(6)
        panel.addView(hscroll(tabsBox), tl)

        ctlBox = column()
        ctlBox.setPadding(0, dp(4), 0, dp(24))
        val sc = ScrollView(this)
        sc.isVerticalScrollBarEnabled = false
        sc.addView(ctlBox)
        panel.addView(sc, LinearLayout.LayoutParams(MATCH, 0, 1f))

        editorRoot.addView(panel, LinearLayout.LayoutParams(MATCH, 0, 1f))
    }

    private fun openEditor() {
        mainView.visibility = View.GONE
        editorRoot.visibility = View.VISIBLE
        selectModule(0)
    }

    private fun closeEditor() {
        editorRoot.visibility = View.GONE
        mainView.visibility = View.VISIBLE
        refreshCards()
    }

    private fun selectModule(i: Int) {
        modIdx = i
        subIdx = 0
        editorTitle.text = MODULES[i].name
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
            if (sel) item.background = rounded(cPill, 22)
            item.setPadding(dp(if (sel) 14 else 12), dp(8), dp(if (sel) 16 else 12), dp(8))
            item.setOnClickListener { selectModule(i) }
            item.addView(IconView(this, m.icon, if (sel) cAccent else cMuted), lp(dp(22), dp(22)))
            if (sel) {
                val l = lp(WRAP, WRAP)
                l.leftMargin = dp(6)
                item.addView(tv(m.name, 13f, cAccent, true, Gravity.CENTER), l)
            }
            chipsBox.addView(item, lp(WRAP, WRAP))
        }
    }

    private fun rebuildTabs() {
        tabsBox.removeAllViews()
        val subs = MODULES[modIdx].subs
        for (i in subs.indices) {
            val sel = i == subIdx
            val t = tv(subs[i].name, 13f, if (sel) cAccent else cMuted, sel, Gravity.CENTER)
            t.setPadding(dp(14), dp(8), dp(14), dp(8))
            if (sel) t.background = rounded(cPill, 18)
            t.setOnClickListener {
                subIdx = i
                rebuildTabs()
                rebuildControls()
            }
            val l = lp(WRAP, WRAP)
            l.rightMargin = dp(6)
            tabsBox.addView(t, l)
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
        col.setPadding(dp(14), dp(8), dp(14), dp(16))

        val banner = column()
        val bg = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFF2B3556.toInt(), 0xFF1B2140.toInt()))
        bg.cornerRadius = dp(22).toFloat()
        banner.background = bg
        banner.setPadding(dp(18), dp(16), dp(18), dp(16))
        banner.addView(tv("ساعت پشت سوژه", 18f, cText, true), lp(MATCH, WRAP))
        banner.addView(tv("یک عکس انتخاب کن، قالب را برگزین و شخصی‌سازی کن.", 13f, cMuted), lp(MATCH, WRAP))
        val bl = lp(WRAP, dp(40))
        bl.topMargin = dp(12)
        bl.gravity = Gravity.RIGHT
        banner.addView(pill("انتخاب عکس", true) { picker.launch("image/*") }, bl)
        col.addView(banner, lp(MATCH, WRAP))

        status = tv("یک عکس انتخاب کنید (بهتر است عکس یک آدم باشد).", 13f, cMuted)
        status.setPadding(dp(4), dp(12), dp(4), dp(10))
        col.addView(status, lp(MATCH, WRAP))

        col.addView(buildGrid(), lp(MATCH, WRAP))

        val wl = lp(MATCH, dp(48))
        wl.topMargin = dp(6)
        col.addView(pill("تنظیم به‌عنوان والپیپر", true) { setWallpaper() }, wl)

        sc.addView(col)
        return sc
    }

    private fun buildGrid(): View {
        val grid = column()
        val dm = resources.displayMetrics
        val cw = (dm.widthPixels - dp(14) * 2 - dp(10)) / 2
        val ch = (cw * 1.55f).toInt()
        val items = ArrayList<Int>()
        items.add(-1)
        for (i in TEMPLATES.indices) items.add(i)
        var i = 0
        while (i < items.size) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (j in 0..1) {
                val k = i + j
                if (k < items.size) {
                    val idx = items[k]
                    val card = TemplateCard(this, idx, if (idx < 0) "طرح من" else TEMPLATES[idx].name, prefs)
                    roundClip(card, dp(20).toFloat())
                    card.setOnClickListener {
                        if (idx >= 0) applyTemplate(idx) else put("tpl", -1)
                        refreshCards()
                        openEditor()
                    }
                    cards.add(card)
                    val l = lp(cw, ch)
                    if (j == 0) l.rightMargin = dp(10)
                    row.addView(card, l)
                }
            }
            val rl = lp(MATCH, WRAP)
            rl.bottomMargin = dp(10)
            grid.addView(row, rl)
            i += 2
        }
        return grid
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

        val c2 = column()
        c2.background = rounded(cCard, 18)
        c2.setPadding(dp(16), dp(14), dp(16), dp(14))
        c2.addView(tv("بازنشانی همه تنظیمات", 15f, cText), lp(MATCH, WRAP))
        c2.addView(tv("فونت‌ها، رنگ‌ها و موقعیت‌ها به حالت اول برمی‌گردند. عکس شما پاک نمی‌شود.", 12f, cMuted), lp(MATCH, WRAP))
        val rl = lp(WRAP, dp(40))
        rl.topMargin = dp(10)
        rl.gravity = Gravity.RIGHT
        c2.addView(pill("بازنشانی", false) {
            prefs.edit().clear().apply()
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
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setBackgroundColor(cNav)
        bar.setPadding(dp(8), dp(8), dp(8), dp(10))
        val icons = intArrayOf(0, 2)
        val names = arrayOf("قالب‌ها", "تنظیمات")
        for (i in 0..1) {
            val item = LinearLayout(this)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER_HORIZONTAL
            val pillBox = FrameLayout(this)
            val icon = IconView(this, icons[i], cMuted)
            pillBox.addView(icon, FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
            item.addView(pillBox, lp(dp(60), dp(32)))
            val label = tv(names[i], 12f, cMuted, false, Gravity.CENTER)
            item.addView(label, lp(WRAP, WRAP))
            item.setOnClickListener { showTab(i) }
            navPills.add(pillBox)
            navIcons.add(icon)
            navLabels.add(label)
            bar.addView(item, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        return bar
    }

    private fun showTab(i: Int) {
        galleryPage.visibility = if (i == 0) View.VISIBLE else View.GONE
        settingsPage.visibility = if (i == 1) View.VISIBLE else View.GONE
        for (k in navPills.indices) {
            val sel = k == i
            navPills[k].background = if (sel) rounded(cPill, 16) else null
            navIcons[k].setTint(if (sel) cAccent else cMuted)
            navLabels[k].setTextColor(if (sel) cAccent else cMuted)
        }
    }

    // ------------------------------------------------------------ wallpaper + photo processing

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

    private fun segment(bmp: Bitmap) {
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
