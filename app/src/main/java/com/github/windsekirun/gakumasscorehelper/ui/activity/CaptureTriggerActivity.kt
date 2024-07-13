package com.github.windsekirun.gakumasscorehelper.ui.activity

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import com.github.windsekirun.gakumasscorehelper.Constants.INTENT_ACTION_STOP
import com.github.windsekirun.gakumasscorehelper.service.CaptureService
import com.github.windsekirun.gakumasscorehelper.service.CaptureTriggerService

class CaptureTriggerActivity : ComponentActivity() {

    private val mediaProjectionPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    val serviceIntent = Intent(this, CaptureService::class.java).apply {
                        putExtra("mediaProjectionData", data)
                    }
                    startForegroundService(serviceIntent)
                    finish()
                }
            } else {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == INTENT_ACTION_STOP) {
            val intent = Intent(this, CaptureTriggerService::class.java)
            stopService(intent)
            finishAffinity()
            return
        }

        val mediaProjectionManager = getSystemService<MediaProjectionManager>()!!
        val mediaProjectionIntent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionPermissionLauncher.launch(mediaProjectionIntent)
    }

}