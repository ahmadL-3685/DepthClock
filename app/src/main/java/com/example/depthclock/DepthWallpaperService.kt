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
        private val prefs by lazy {
            applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
        private var shown = false
        private var motionOn = false
        private var sensorOn = false
        private var tx = 0f
        private var ty = 0f
        private var cx = 0f
        private var cy = 0f

        private val frame = object : Runnable {
            override fun run() {
                syncMotion()
                drawFrame()
                if (shown) handler.postDelayed(this, if (motionOn) 33L else 1000L)
            }
        }

        private fun syncMotion() {
            val want = shown && prefs.getBoolean(KEY_MOTION, false)
            motionOn = want
            val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (want && !sensorOn) {
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                    sensorOn = true
                }
            } else if (!want && sensorOn) {
                sm.unregisterListener(this)
                sensorOn = false
                tx = 0f
                ty = 0f
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            renderer = DepthRenderer(applicationContext)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            shown = visible
            handler.removeCallbacks(frame)
            if (visible) {
                handler.post(frame)
            } else {
                syncMotion()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onDestroy() {
            shown = false
            handler.removeCallbacks(frame)
            syncMotion()
            super.onDestroy()
        }

        override fun onSensorChanged(e: SensorEvent) {
            tx = (-e.values[0] / 4f).coerceIn(-1f, 1f)
            ty = ((e.values[1] - 6f) / 4f).coerceIn(-1f, 1f)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        private fun drawFrame() {
            val r = renderer ?: return
            if (motionOn) {
                cx += (tx - cx) * 0.15f
                cy += (ty - cy) * 0.15f
            } else {
                cx = 0f
                cy = 0f
            }
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
