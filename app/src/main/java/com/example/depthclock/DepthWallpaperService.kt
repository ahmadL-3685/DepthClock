package com.example.depthclock

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class DepthWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DepthEngine()

    inner class DepthEngine : Engine(), SensorEventListener {
        private val handler = Handler(Looper.getMainLooper())
        private var renderer: DepthRenderer? = null
        private var shown = false
        private var tx = 0f
        private var ty = 0f
        private var cx = 0f
        private var cy = 0f

        private val frame = object : Runnable {
            override fun run() {
                drawFrame()
                if (!shown) return
                if (motionOn()) {
                    handler.postDelayed(this, 40)
                } else {
                    // no motion: redraw only when the minute changes
                    val now = System.currentTimeMillis()
                    handler.postDelayed(this, 60000L - now % 60000L + 50L)
                }
            }
        }

        private fun motionOn(): Boolean =
            applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_MOTION, false)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            renderer = DepthRenderer(applicationContext)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            shown = visible
            val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (visible) {
                if (motionOn()) {
                    sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                        sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                }
                handler.removeCallbacks(frame)
                handler.post(frame)
            } else {
                sm.unregisterListener(this)
                handler.removeCallbacks(frame)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onDestroy() {
            shown = false
            handler.removeCallbacks(frame)
            (getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(this)
            super.onDestroy()
        }

        override fun onSensorChanged(e: SensorEvent) {
            tx = (-e.values[0] / 4f).coerceIn(-1f, 1f)
            ty = ((e.values[1] - 6f) / 4f).coerceIn(-1f, 1f)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        private fun drawFrame() {
            val r = renderer ?: return
            cx += (tx - cx) * 0.15f
            cy += (ty - cy) * 0.15f
            val holder = surfaceHolder
            val canvas = try {
                holder.lockCanvas()
            } catch (e: Exception) {
                null
            } ?: return
            try {
                r.draw(canvas, canvas.width, canvas.height, cx, cy)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
