package com.github.windsekirun.gakumasscorehelper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.windsekirun.gakumasscorehelper.preference.DataPreference
import com.github.windsekirun.gakumasscorehelper.repository.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataViewModel @Inject constructor(
    private val repository: DataStoreRepository
) : ViewModel() {

    val dataPreferencesFlow = repository.dataPreferencesFlow

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.updatePreferences {
                DataPreference()
            }
        }
    }

    fun updateValues(key: String, value: Any) {
        viewModelScope.launch {
            repository.updatePreferenceValue(key, value)
        }
    }
}