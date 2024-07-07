package com.github.windsekirun.gakumasscorehelper.service

import android.app.Activity.RESULT_OK
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.github.windsekirun.gakumasscorehelper.Constants
import com.github.windsekirun.gakumasscorehelper.R
import com.github.windsekirun.gakumasscorehelper.ui.activity.AnalyzeActivity
import java.io.File
import java.io.FileOutputStream

class CaptureService : Service() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService<MediaProjectionManager>()!!
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaProjectionData = intent?.getParcelableExtra<Intent>("mediaProjectionData")
        startForegroundService()

        mediaProjection = mediaProjectionManager.getMediaProjection(RESULT_OK, mediaProjectionData!!)
        mediaProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopSelf()
            }
        }, null)
        captureAndSaveScreenshot()

        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        ServiceCompat.startForeground(
            this,
            101,
            createNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            },
        )
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.capturing_progress))
            .setSmallIcon(R.drawable.notification_icon)
            .build()
    }

    private fun captureAndSaveScreenshot() {
        val metrics = resources.displayMetrics
        val imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        val surface = imageReader.surface

        mediaProjection.createVirtualDisplay(
            "Screenshot",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            0,
            surface,
            null,
            null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                val width = it.width
                val height = it.height
                val planes = it.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                val cacheDir = File(externalCacheDir, "screenshots")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val file = File(cacheDir, "screenshot.png")
                FileOutputStream(file).use { out ->
                    val newBitmap = cropBitmap(bitmap)
                    newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }

                val analyzeIntent = Intent(this, AnalyzeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("screenshotPath", file.absolutePath)
                }
                startActivity(analyzeIntent)

                imageReader.close()
                mediaProjection.stop()
                stopSelf()
            }
        }, null)
    }

    private fun cropBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val left = (width * 0.2).toInt()
        val top = (height * 0.5).toInt()
        val right = (width * 0.8).toInt()
        val bottom = (height * 0.7).toInt()

        val cropWidth = right - left
        val cropHeight = bottom - top

        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}