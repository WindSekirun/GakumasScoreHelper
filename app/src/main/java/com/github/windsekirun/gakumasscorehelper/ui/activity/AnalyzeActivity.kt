package com.github.windsekirun.gakumasscorehelper.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.windsekirun.gakumasscorehelper.R
import com.github.windsekirun.gakumasscorehelper.preference.DataPreference
import com.github.windsekirun.gakumasscorehelper.ui.component.TextFieldTableItem
import com.github.windsekirun.gakumasscorehelper.ui.theme.GakumasScoreHelperTheme
import com.github.windsekirun.gakumasscorehelper.viewmodel.DataViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.floor
import kotlin.math.min

@AndroidEntryPoint
class AnalyzeActivity : ComponentActivity() {
    private val viewModel by viewModels<DataViewModel>()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenshotPath = intent.getStringExtra(SCREENSHOT_PATH)
        val image = screenshotPath?.let {
            InputImage.fromFilePath(this, Uri.fromFile(File(it)))
        }

        setContent {
            GakumasScoreHelperTheme {
                val dataPreference by viewModel.dataPreferencesFlow.collectAsState(initial = DataPreference())
                var predictionResult by remember { mutableStateOf<PredictionResult?>(null) }
                var calculatedScores by remember { mutableStateOf<List<AnalyzeType>>(emptyList()) }
                if (image == null) {
                    Text(stringResource(R.string.analyze_fail))
                    return@GakumasScoreHelperTheme
                }

                LaunchedEffect(image) {
                    predictionResult = extractScoresFromBitmap(image)
                }

                if (calculatedScores.isNotEmpty()) {
                    ScoreOverlay(scores = calculatedScores, onClose = {
                        finish()
                    })
                } else if (predictionResult != null) {
                    ConfirmOverlay(
                        useMaster = dataPreference.useMaster,
                        predictionResult!!,
                        onClose = { vo, da, di, useMaster ->
                            calculatedScores = calculateScores(
                                dataPreference,
                                useMaster,
                                listOf(vo, da, di),
                                targetScores = mapOf(
                                    "S+" to dataPreference.criteriaSPlus,
                                    "S" to dataPreference.criteriaS,
                                    "A+" to dataPreference.criteriaAPlus,
                                    "A" to dataPreference.criteriaA
                                )
                            )
                        },
                        onRetry = {
                            val intent = Intent(this@AnalyzeActivity, CaptureTriggerActivity::class.java)
                            startActivity(intent)
                            finishAffinity()
                        })
                }
            }
        }
    }

    private suspend fun extractScoresFromBitmap(image: InputImage): PredictionResult {
        fun List<Text.TextBlock>.getIndex(index: Int): Int {
            return this.getOrNull(index)?.text?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        }

        return suspendCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (result.textBlocks.isNotEmpty()) {
                        val blocks = result.textBlocks
                        cont.resume(
                            PredictionResult(
                                result.text.replace("\n", "\\n"),
                                blocks.getIndex(0),
                                blocks.getIndex(1),
                                blocks.getIndex(2)
                            )
                        )
                    } else {
                        cont.resume(PredictionResult("", 0, 0, 0))
                    }
                    Log.d("AnalyzeActivity", "visionText: $result")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.analyze_fail), Toast.LENGTH_SHORT).show()
                    Log.e("AnalyzeActivity", "Error happened", e)
                    cont.resumeWithException(e)
                }
        }
    }

    private fun calculateScores(
        preference: DataPreference,
        useMaster: Boolean,
        extractedScores: List<Int>,
        targetScores: Map<String, Int>
    ): List<AnalyzeType> {
        val (vo, da, vi) = extractedScores
        return targetScores.entries.map { (grade, targetScore) ->
            calculateMinScoreToReach(preference, useMaster, vo, da, vi, grade, targetScore)
        }
    }

    private fun calculateMinScoreToReach(
        preference: DataPreference,
        useMaster: Boolean,
        vo: Int,
        da: Int,
        vi: Int,
        grade: String,
        score: Int
    ): AnalyzeType {
        // 1.1.1 : 最終試験1位パラメータ30点追加
        val maxScore = if (useMaster) 1800 else 1500
        fun Int.plusAdditionalParameter() = min(this + 30, maxScore)

        val parameterValue =
            preference.basicScore + floor((vo.plusAdditionalParameter() + da.plusAdditionalParameter() + vi.plusAdditionalParameter()) * preference.parameterMultiplier).toInt()
        val targetScore = score - parameterValue

        var requiredScore = 0

        val ranges = listOf(
            5000 to preference.examScoreMultiplier_0_5000,
            5000 to preference.examScoreMultiplier_5000_10000,
            10000 to preference.examScoreMultiplier_10000_20000,
            10000 to preference.examScoreMultiplier_20000_30000,
            10000 to preference.examScoreMultiplier_30000_40000,
        )

        var accumulatedScore = 0.0

        for ((range, multiplier) in ranges) {
            if (accumulatedScore >= targetScore) break
            val remainingTarget = targetScore - accumulatedScore
            val applicableRange = minOf(range.toDouble(), remainingTarget / multiplier)
            accumulatedScore += applicableRange * multiplier
            requiredScore += applicableRange.toInt()
        }

        return when {
            accumulatedScore < targetScore -> AnalyzeType.Impossible(grade)
            requiredScore == 0 -> AnalyzeType.Already(grade)
            requiredScore < 7000 -> AnalyzeType.Already(grade)
            else -> AnalyzeType.Value(grade, requiredScore)
        }
    }

    companion object {
        private const val SCREENSHOT_PATH = "screenshotPath"
    }
}

@Composable
fun ConfirmOverlay(
    useMaster: Boolean,
    predictionResult: PredictionResult,
    onClose: (vo: Int, da: Int, vi: Int, useMaster: Boolean) -> Unit,
    onRetry: () -> Unit
) {

    var editedVo by remember { mutableStateOf(predictionResult.vo.toString()) }
    var editedDa by remember { mutableStateOf(predictionResult.da.toString()) }
    var editedVi by remember { mutableStateOf(predictionResult.vi.toString()) }
    var useMasterState by remember { mutableStateOf(useMaster) }

    OverlayContent(
        onClose = {
            onClose(
                editedVo.toIntOrNull() ?: 0,
                editedDa.toIntOrNull() ?: 0,
                editedVi.toIntOrNull() ?: 0,
                useMasterState
            )
        },
        button = {
            Button(onClick = onRetry, modifier = Modifier.padding(end = 8.dp)) {
                Text(stringResource(R.string.retry))
            }
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(R.string.confirm_recognition), fontSize = 18.sp, color = Color.White)
            Text(text = "Result: ${predictionResult.text}", fontSize = 12.sp, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = useMasterState,
                    onCheckedChange = {
                        useMasterState = it
                    }
                )
                Text(text = stringResource(R.string.use_master_button), fontSize = 12.sp, color = Color.White)
            }
            TextFieldTableItem(
                index = 0,
                row = "Vo" to editedVo,
                labelColor = Color.White,
                onValueChange = { _, value -> editedVo = value }
            )
            TextFieldTableItem(
                index = 0,
                row = "Da" to editedDa,
                labelColor = Color.White,
                onValueChange = { _, value -> editedDa = value }
            )
            TextFieldTableItem(
                index = 0,
                row = "Vi" to editedVi,
                labelColor = Color.White,
                onValueChange = { _, value -> editedVi = value }
            )

        }
    }
}

@Composable
fun ScoreOverlay(scores: List<AnalyzeType>, onClose: () -> Unit) {
    OverlayContent(onClose = onClose) {
        scores.forEach {
            when (it) {
                is AnalyzeType.Value -> {
                    Row {
                        val builder = buildAnnotatedString {
                            append(it.grade)
                            append(" : ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(it.score.toString())
                            }
                        }
                        Text(text = builder, fontSize = 18.sp, color = Color.White)
                    }
                }

                is AnalyzeType.Impossible -> {
                    Text(
                        text = stringResource(R.string.impossible_result, it.grade),
                        style = TextStyle(textDecoration = TextDecoration.LineThrough),
                        color = Color.Gray
                    )
                }

                is AnalyzeType.Already -> {
                    Text(
                        text = stringResource(R.string.complete_result, it.grade),
                        style = TextStyle(textDecoration = TextDecoration.LineThrough),
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun OverlayContent(onClose: () -> Unit, button: @Composable () -> Unit = {}, body: @Composable () -> Unit) {
    Box {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .background(Color(0x80000000), shape = RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            body()
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                button()
                Button(onClick = onClose) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

@Preview(locale = "ko")
@Preview(locale = "en")
@Preview(locale = "ja")
@Composable
fun ConfirmOverlayPreview() {
    ConfirmOverlay(
        useMaster = true,
        predictionResult = PredictionResult("641 1500 1221", 641, 1550, 1221),
        onClose = { _, _, _, _ -> },
        onRetry = {})
}

@Preview(locale = "ko")
@Preview(locale = "en")
@Preview(locale = "ja")
@Composable
fun ScoreOverlayPreview() {
    val list = listOf(
        AnalyzeType.Impossible("S+"),
        AnalyzeType.Impossible("S"),
        AnalyzeType.Value("A+", 8793),
        AnalyzeType.Already("A")
    )
    ScoreOverlay(scores = list, onClose = { })
}

@Preview(locale = "ko")
@Preview(locale = "en")
@Preview(locale = "ja")
@Composable
fun ScoreOverlayPreview2() {
    val list = listOf(
        AnalyzeType.Value("S+", 49999),
        AnalyzeType.Value("S", 35673),
        AnalyzeType.Value("A+", 15800),
        AnalyzeType.Value("A", 9400)
    )
    ScoreOverlay(scores = list, onClose = { })
}

data class PredictionResult(val text: String, val vo: Int, val da: Int, val vi: Int)

sealed class AnalyzeType {
    data class Impossible(val grade: String) : AnalyzeType()
    data class Value(val grade: String, val score: Int) : AnalyzeType()
    data class Already(val grade: String) : AnalyzeType()
}