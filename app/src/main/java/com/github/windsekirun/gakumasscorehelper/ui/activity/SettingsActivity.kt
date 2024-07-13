package com.github.windsekirun.gakumasscorehelper.ui.activity

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.github.windsekirun.gakumasscorehelper.Constants
import com.github.windsekirun.gakumasscorehelper.R
import com.github.windsekirun.gakumasscorehelper.preference.DataPreference
import com.github.windsekirun.gakumasscorehelper.service.CaptureTriggerService
import com.github.windsekirun.gakumasscorehelper.ui.component.TextFieldTableItem
import com.github.windsekirun.gakumasscorehelper.ui.component.TitleAndDesc
import com.github.windsekirun.gakumasscorehelper.ui.theme.GakumasScoreHelperTheme
import com.github.windsekirun.gakumasscorehelper.viewmodel.DataViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    private val viewModel by viewModels<DataViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GakumasScoreHelperTheme {
                val dataPreference by viewModel.dataPreferencesFlow.collectAsState(initial = DataPreference())
                SettingsTop(
                    dataPreference,
                    resetToDefaults = { viewModel.resetToDefaults() },
                    clickStartService = { startForegroundService(it) },
                    clickStopService = {
                        stopForegroundService()
                    },
                    onOverlayChange = { value -> viewModel.updateValues(Constants.PREFERENCE_KEY_OVERLAY_USE, value) },
                    updateValues = { key, value -> viewModel.updateValues(key, value) })
            }
        }
    }

    private fun startForegroundService(useOverlayChecked: Boolean) {
        val notificationManager = getSystemService<NotificationManager>()
        if (notificationManager?.areNotificationsEnabled() != true) {
            showAlertDialog(getString(R.string.notification_not_enabled)) {
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            }
            return
        }

        if (useOverlayChecked && !Settings.canDrawOverlays(this)) {
            showAlertDialog(getString(R.string.overlay_not_enabled)) {
                val intent = Intent().apply {
                    action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            }
            return
        }

        val intent = Intent(this, CaptureTriggerService::class.java)
        startForegroundService(intent)
        finishAffinity()
    }

    private fun stopForegroundService() {
        val intent = Intent(this, CaptureTriggerService::class.java)
        stopService(intent)
        finishAffinity()
    }

    private fun showAlertDialog(message: String, positive: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
                positive()
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.close) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

@Composable
fun SettingsTop(
    preference: DataPreference,
    clickStartService: (useOverlayChecked: Boolean) -> Unit,
    clickStopService: () -> Unit,
    resetToDefaults: () -> Unit,
    updateValues: (key: String, value: Any) -> Unit,
    onOverlayChange: (checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyList = listOf(
        Constants.PREFERENCE_KEY_CRITERIA_A,
        Constants.PREFERENCE_KEY_CRITERIA_A_PLUS,
        Constants.PREFERENCE_KEY_CRITERIA_S,
        Constants.PREFERENCE_KEY_BASIC_SCORE,
        Constants.PREFERENCE_KEY_PARAMETER_MULTIPLIER,
        Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_0_5000,
        Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_5000_10000,
        Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_10000_20000,
        Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_20000_30000,
        Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_30000_40000,
    )
    val valueList = listOf(
        "A" to preference.criteriaA.toString(),
        "A+" to preference.criteriaAPlus.toString(),
        "S" to preference.criteriaS.toString(),
        stringResource(R.string.basic_first_score) to preference.basicScore.toString(),
        stringResource(R.string.parameter_multiplier) to preference.parameterMultiplier.toString(),
        "0 ~ 5000 →" to preference.examScoreMultiplier_0_5000.toString(),
        "5000 ~ 10000 →" to preference.examScoreMultiplier_5000_10000.toString(),
        "10000 ~ 20000 →" to preference.examScoreMultiplier_10000_20000.toString(),
        "20000 ~ 30000 →" to preference.examScoreMultiplier_20000_30000.toString(),
        "30000 ~ 40000 →" to preference.examScoreMultiplier_30000_40000.toString(),
    )

    var useOverlayChecked by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(modifier = Modifier.padding(horizontal = 10.dp)) {
            item(key = "app-title") {
                TitleAndDesc(
                    title = stringResource(id = R.string.app_name),
                    desc = stringResource(R.string.app_desc),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            item(key = "start-service") {
                Column {
                    TitleAndDesc(
                        title = stringResource(id = R.string.start_service_title),
                        desc = stringResource(id = R.string.start_service_desc),
                        modifier = Modifier.padding(top = 32.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useOverlayChecked,
                            onCheckedChange = {
                                useOverlayChecked = it
                                onOverlayChange(it)
                            }
                        )
                        Text(
                            stringResource(R.string.use_overlay_button)
                        )
                    }
                    Text(
                        stringResource(R.string.use_overlay_desc),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            item(key = "start-service-button") {
                Button(
                    onClick = { clickStartService(useOverlayChecked) },
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {
                    Text(stringResource(R.string.start_service_button))
                }
            }
            item(key = "stop-service-button") {
                Button(
                    onClick = clickStopService,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                ) {
                    Text(stringResource(R.string.stop_service))
                }
            }
            item(key = "preference_title") {
                TitleAndDesc(
                    title = stringResource(R.string.preference_title),
                    desc = stringResource(R.string.preference_desc),
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
            item(key = "reset-to-defaults") {
                Button(
                    onClick = resetToDefaults,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {
                    Text(stringResource(R.string.reset_to_defaults))
                }
            }
            itemsIndexed(valueList, key = { _, row -> row.first }) { index, row ->
                TextFieldTableItem(index, row, onValueChange = { i, value ->
                    if (keyList[index].contains("multiplier")) {
                        updateValues(keyList[i], value.toDouble())
                    } else {
                        updateValues(keyList[i], value.toInt())
                    }
                })
            }

            item(key = "bottom-spacer") {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}


@Preview(locale = "ko")
@Preview(locale = "en")
@Preview(locale = "ja")
@Preview(locale = "ko", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(locale = "ja", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreview() {
    GakumasScoreHelperTheme {
        SettingsTop(
            DataPreference(),
            resetToDefaults = {},
            clickStartService = {},
            clickStopService = {},
            onOverlayChange = {},
            updateValues = { _, _ -> })
    }
}