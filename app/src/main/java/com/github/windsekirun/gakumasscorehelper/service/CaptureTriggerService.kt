package com.github.windsekirun.gakumasscorehelper.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.core.view.setPadding
import com.github.windsekirun.gakumasscorehelper.Constants
import com.github.windsekirun.gakumasscorehelper.R
import com.github.windsekirun.gakumasscorehelper.repository.DataStoreRepository
import com.github.windsekirun.gakumasscorehelper.repository.dataStore
import com.github.windsekirun.gakumasscorehelper.ui.activity.CaptureTriggerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

@AndroidEntryPoint
class CaptureTriggerService : Service(), CoroutineScope {
    private val windowManager by lazy { getSystemService<WindowManager>()!! }

    private var view: View? = null

    @Inject
    lateinit var dataStoreRepository: DataStoreRepository

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + SupervisorJob()

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        launch {
            val useOverlay = dataStore.data.first()[DataStoreRepository.PreferencesKeys.USE_OVERLAY] ?: true
            val overlayX = dataStore.data.first()[DataStoreRepository.PreferencesKeys.OVERLAY_X] ?: 0
            val overlayY = dataStore.data.first()[DataStoreRepository.PreferencesKeys.OVERLAY_Y] ?: 0
            startForegroundService()

            if (useOverlay) {
                startOverlay(overlayX, overlayY)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        view?.parent?.let {
            windowManager.removeView(view)
        }
        cancel()
    }

    private fun startForegroundService() {
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            100,
            createNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                Constants.CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                enableLights(false)
                setShowBadge(false)
                enableVibration(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val triggerIntent = Intent(this, CaptureTriggerActivity::class.java).run {
            PendingIntent.getActivity(this@CaptureTriggerService, 0, this, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopIntent = Intent(this, CaptureTriggerActivity::class.java).apply {
            action = Constants.INTENT_ACTION_STOP
        }.run {
            PendingIntent.getActivity(this@CaptureTriggerService, 1, this, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.app_desc))
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(triggerIntent)
            .addAction(R.drawable.hatsuboshi_icon, getString(R.string.stop_service), stopIntent)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startOverlay(overlayX: Int, overlayY: Int) {
        val params = WindowManager.LayoutParams(
            32.toPx(),
            32.toPx(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = overlayX
            y = overlayY
        }

        view = ImageButton(this).apply {
            setImageResource(R.drawable.hatsuboshi_icon)
            setBackgroundResource(R.drawable.overlay_button_background)
            setPadding(4.toPx())
            scaleType = ImageView.ScaleType.FIT_CENTER

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isClick = true

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()

                        if (abs(dx) > THRESHOLD_OVERLAY_MOVE || abs(dy) > THRESHOLD_OVERLAY_MOVE) {
                            isClick = false
                        }

                        params.x = initialX + dx
                        params.y = initialY + dy
                        updateOverlayPreference(params.x, params.y)
                        windowManager.updateViewLayout(view, params)
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            val intent = Intent(this@CaptureTriggerService, CaptureTriggerActivity::class.java).apply {
                                addFlags(FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        windowManager.addView(view, params)
    }

    private fun updateOverlayPreference(overlayX: Int, overlayY: Int) {
        launch {
            dataStoreRepository.updatePreferenceValue(Constants.PREFERENCE_KEY_OVERLAY_X, overlayX)
            dataStoreRepository.updatePreferenceValue(Constants.PREFERENCE_KEY_OVERLAY_Y, overlayY)
        }
    }

    private fun Int.toPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density + 0.5f).toInt()
    }

    companion object {
        const val THRESHOLD_OVERLAY_MOVE = 5
    }

}