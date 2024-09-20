package com.github.windsekirun.gakumasscorehelper.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.windsekirun.gakumasscorehelper.Constants
import com.github.windsekirun.gakumasscorehelper.preference.DataPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreRepository(private val context: Context) {

    object PreferencesKeys {
        val BASIC_SCORE = intPreferencesKey(Constants.PREFERENCE_KEY_BASIC_SCORE)
        val PARAMETER_MULTIPLIER = doublePreferencesKey(Constants.PREFERENCE_KEY_PARAMETER_MULTIPLIER)
        val EXAM_SCORE_MULTIPLIER_0_5000 = doublePreferencesKey(Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_0_5000)
        val EXAM_SCORE_MULTIPLIER_5000_10000 =
            doublePreferencesKey(Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_5000_10000)
        val EXAM_SCORE_MULTIPLIER_10000_20000 =
            doublePreferencesKey(Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_10000_20000)
        val EXAM_SCORE_MULTIPLIER_20000_30000 =
            doublePreferencesKey(Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_20000_30000)
        val EXAM_SCORE_MULTIPLIER_30000_40000 =
            doublePreferencesKey(Constants.PREFERENCE_KEY_EXAM_SCORE_MULTIPLIER_30000_40000)
        val CRITERIA_A = intPreferencesKey(Constants.PREFERENCE_KEY_CRITERIA_A)
        val CRITERIA_A_PLUS = intPreferencesKey(Constants.PREFERENCE_KEY_CRITERIA_A_PLUS)
        val CRITERIA_S = intPreferencesKey(Constants.PREFERENCE_KEY_CRITERIA_S)
        val CRITERIA_S_PLUS = intPreferencesKey(Constants.PREFERENCE_KEY_CRITERIA_S_PLUS)
        val USE_OVERLAY = booleanPreferencesKey(Constants.PREFERENCE_KEY_OVERLAY_USE)
        val USE_MASTER = booleanPreferencesKey(Constants.PREFERENCE_KEY_MASTER_USE)
        val OVERLAY_X = intPreferencesKey(Constants.PREFERENCE_KEY_OVERLAY_X)
        val OVERLAY_Y = intPreferencesKey(Constants.PREFERENCE_KEY_OVERLAY_Y)
    }

    val dataPreferencesFlow: Flow<DataPreference> = context.dataStore.data
        .map { preferences ->
            DataPreference(
                basicScore = preferences[PreferencesKeys.BASIC_SCORE] ?: 1700,
                parameterMultiplier = preferences[PreferencesKeys.PARAMETER_MULTIPLIER] ?: 2.3,
                examScoreMultiplier_0_5000 = preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_0_5000] ?: 0.3,
                examScoreMultiplier_5000_10000 = preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_5000_10000] ?: 0.15,
                examScoreMultiplier_10000_20000 = preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_10000_20000]
                    ?: 0.08,
                examScoreMultiplier_20000_30000 = preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_20000_30000]
                    ?: 0.04,
                examScoreMultiplier_30000_40000 = preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_30000_40000]
                    ?: 0.02,
                criteriaA = preferences[PreferencesKeys.CRITERIA_A] ?: 10001,
                criteriaAPlus = preferences[PreferencesKeys.CRITERIA_A_PLUS] ?: 11501,
                criteriaS = preferences[PreferencesKeys.CRITERIA_S] ?: 13001,
                criteriaSPlus = preferences[PreferencesKeys.CRITERIA_S_PLUS] ?: 14501,
                useMaster = preferences[PreferencesKeys.USE_MASTER] ?: false,
            )
        }

    suspend fun updatePreferences(update: DataPreference.() -> DataPreference) {
        context.dataStore.edit { preferences ->
            val updatedData = DataPreference().update()
            preferences[PreferencesKeys.BASIC_SCORE] = updatedData.basicScore
            preferences[PreferencesKeys.PARAMETER_MULTIPLIER] = updatedData.parameterMultiplier
            preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_0_5000] = updatedData.examScoreMultiplier_0_5000
            preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_5000_10000] = updatedData.examScoreMultiplier_5000_10000
            preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_10000_20000] = updatedData.examScoreMultiplier_10000_20000
            preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_20000_30000] = updatedData.examScoreMultiplier_20000_30000
            preferences[PreferencesKeys.EXAM_SCORE_MULTIPLIER_30000_40000] = updatedData.examScoreMultiplier_30000_40000
            preferences[PreferencesKeys.CRITERIA_A] = updatedData.criteriaA
            preferences[PreferencesKeys.CRITERIA_A_PLUS] = updatedData.criteriaAPlus
            preferences[PreferencesKeys.CRITERIA_S] = updatedData.criteriaS
            preferences[PreferencesKeys.CRITERIA_S_PLUS] = updatedData.criteriaSPlus
            preferences[PreferencesKeys.USE_MASTER] = updatedData.useMaster
        }
    }

    suspend fun updatePreferenceValue(key: String, value: Any) {
        context.dataStore.edit { preferences ->
            when (value) {
                is Int -> preferences[intPreferencesKey(key)] = value
                is Double -> preferences[doublePreferencesKey(key)] = value
                is Boolean -> preferences[booleanPreferencesKey(key)] = value
            }
        }
    }
}