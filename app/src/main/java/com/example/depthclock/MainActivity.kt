package com.example.depthclock

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

class MainActivity : ComponentActivity() {

    private lateinit var status: TextView
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private val cBg = Color.parseColor("#0E1014")
    private val cCard = Color.parseColor("#1A1D24")
    private val cChip = Color.parseColor("#262A33")
    private val cText = Color.parseColor("#F2F3F5")
    private val cMuted = Color.parseColor("#9AA0AA")
    private val cAccent = Color.parseColor("#7C6CFF")
    private val palette = intArrayOf(
        Color.WHITE, Color.BLACK,
        Color.parseColor("#FFE066"), Color.parseColor("#7FDBFF"),
        Color.parseColor("#FF8FB1"), Color.parseColor("#9BFFB0"),
        Color.parseColor("#FF6B4A")
    )
    private val fontChips = ArrayList<TextView>()
    private val swatches = ArrayList<View>()

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) process(uri)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun box(color: Int, radiusDp: Int, strokeColor: Int = Color.TRANSPARENT, strokeDp: Int = 0) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    private fun lp(w: Int, h: Int, top: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = dp(top) }

    private fun label(t: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun actionButton(t: String, primary: Boolean, onClick: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        background = box(if (primary) cAccent else cChip, 16)
        stateListAnimator = null
        minimumHeight = dp(54)
        setOnClickListener { onClick() }
    }

    private fun refreshChips() {
        val sel = prefs.getInt(KEY_FONT, 0)
        fontChips.forEachIndexed { i, v ->
            v.background = box(cChip, 12, if (i == sel) cAccent else Color.TRANSPARENT, 2)
        }
    }

    private fun refreshSwatches() {
        val sel = prefs.getInt(KEY_COLOR, Color.WHITE)
        swatches.forEach { v ->
            val c = v.tag as Int
            v.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(c)
                setStroke(dp(if (c == sel) 3 else 1), if (c == sel) cAccent else Color.parseColor("#454B57"))
            }
        }
    }

    private fun fontRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        for (i in 0 until FONT_COUNT) {
            val chip = TextView(this).apply {
                text = "12:34"
                typeface = fontFor(i)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTextColor(cText)
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(10), dp(16), dp(10))
                setOnClickListener {
                    prefs.edit().putInt(KEY_FONT, i).apply()
                    refreshChips()
                }
            }
            fontChips.add(chip)
            row.addView(chip, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = dp(8) })
        }
        refreshChips()
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            addView(row)
        }
    }

    private fun colorRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        for (c in palette) {
            val v = View(this)
            v.tag = c
            v.setOnClickListener {
                prefs.edit().putInt(KEY_COLOR, c).apply()
                refreshSwatches()
            }
            swatches.add(v)
            row.addView(v, LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })
        }
        refreshSwatches()
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            addView(row)
        }
    }

    private fun slider(parent: LinearLayout, title: String, key: String, def: Int) {
        parent.addView(label(title, 13f, cMuted), lp(WRAP, WRAP, 16))
        val sb = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt(key, def)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            progressTintList = ColorStateList.valueOf(cAccent)
            thumbTintList = ColorStateList.valueOf(cAccent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    prefs.edit().putInt(key, p).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        parent.addView(sb, lp(MATCH, WRAP, 4))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = cBg
        window.navigationBarColor = cBg

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(20), dp(18), dp(28))
        }
        col.addView(label("DepthClock", 26f, cText, true))
        col.addView(label("ساعت پشت سوژه‌ی عکس شما", 14f, cMuted), lp(WRAP, WRAP, 2))

        val dm = resources.displayMetrics
        val pw = (dm.widthPixels * 0.62f).toInt()
        val ph = (pw * dm.heightPixels.toFloat() / dm.widthPixels).toInt()
        val preview = PreviewView(this).apply {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(22).toFloat())
                }
            }
            clipToOutline = true
        }
        col.addView(preview, LinearLayout.LayoutParams(pw, ph).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(18)
        })

        status = label("برای شروع یک عکس انتخاب کنید. عکس یک آدم بهتر جواب می‌دهد.", 13f, cMuted)
        status.gravity = Gravity.CENTER
        col.addView(status, lp(MATCH, WRAP, 12))

        col.addView(actionButton("انتخاب عکس", false) { picker.launch("image/*") }, lp(MATCH, WRAP, 12))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(cCard, 20)
            setPadding(dp(16), dp(16), dp(16), dp(18))
        }
        card.addView(label("فونت ساعت", 13f, cMuted))
        card.addView(fontRow(), lp(MATCH, WRAP, 8))
        card.addView(label("رنگ ساعت", 13f, cMuted), lp(WRAP, WRAP, 16))
        card.addView(colorRow(), lp(MATCH, WRAP, 8))
        slider(card, "اندازه ساعت", KEY_SIZE, 50)
        slider(card, "جای ساعت (بالا و پایین)", KEY_Y, 30)

        val sw = Switch(this).apply {
            text = "حرکت سه‌بعدی با کج کردن گوشی"
            setTextColor(cText)
            isChecked = prefs.getBoolean(KEY_MOTION, false)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean(KEY_MOTION, on).apply() }
        }
        card.addView(sw, lp(MATCH, WRAP, 18))
        col.addView(card, lp(MATCH, WRAP, 16))

        col.addView(actionButton("تنظیم به‌عنوان والپیپر", true) { setWallpaper() }, lp(MATCH, WRAP, 16))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(cBg)
            addView(col)
        }
        setContentView(scroll)
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
            val p = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
            c.drawBitmap(ms, 0f, 0f, p)

            val bgTmp = File(filesDir, "bg.tmp")
            val fgTmp = File(filesDir, "fg.tmp")
            FileOutputStream(bgTmp).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            FileOutputStream(fgTmp).use { cut.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bgTmp.renameTo(File(filesDir, BG_FILE))
            fgTmp.renameTo(File(filesDir, FG_FILE))

            runOnUiThread { status.text = "آماده است. حالا «تنظیم به‌عنوان والپیپر» را بزنید." }
        } catch (e: Throwable) {
            runOnUiThread { status.text = "خطا در ذخیره: ${e.message}" }
        }
    }

    private fun loadScaled(uri: Uri, maxSide: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        val o = BitmapFactory.Options().apply { inSampleSize = sample }
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
