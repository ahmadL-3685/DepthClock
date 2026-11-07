package com.example.depthclock

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
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

class MainActivity : ComponentActivity() {

    private lateinit var status: TextView
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private val colors = intArrayOf(
        Color.WHITE, Color.BLACK,
        0xFFFFE066.toInt(), 0xFF7FDBFF.toInt(), 0xFFFF8FB1.toInt()
    )

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) process(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(px(12), px(12), px(12), px(12))
        }

        val preview = PreviewView(this)
        root.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        status = TextView(this).apply {
            text = "یک عکس انتخاب کنید (بهتر است عکس یک آدم باشد)."
            setTextColor(Color.DKGRAY)
            setPadding(0, px(8), 0, px(4))
        }
        root.addView(status)

        root.addView(button("انتخاب عکس") { picker.launch("image/*") })
        addSeek(root, "اندازه ساعت", KEY_SIZE, 50)
        addSeek(root, "جای ساعت (بالا و پایین)", KEY_Y, 30)
        root.addView(button("تغییر رنگ ساعت") {
            val cur = prefs.getInt(KEY_COLOR, Color.WHITE)
            val next = colors[(colors.indexOf(cur) + 1).mod(colors.size)]
            prefs.edit().putInt(KEY_COLOR, next).apply()
        })
        root.addView(button("تنظیم به‌عنوان والپیپر") { setWallpaper() })

        setContentView(root)
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun addSeek(root: LinearLayout, label: String, key: String, def: Int) {
        root.addView(TextView(this).apply { text = label; setTextColor(Color.DKGRAY) })
        root.addView(SeekBar(this).apply {
            max = 100
            progress = prefs.getInt(key, def)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    prefs.edit().putInt(key, p).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
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
