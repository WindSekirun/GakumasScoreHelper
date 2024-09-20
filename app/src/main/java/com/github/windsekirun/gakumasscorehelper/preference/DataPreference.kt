package com.github.windsekirun.gakumasscorehelper.preference

@Suppress("PropertyName")
data class DataPreference(
    val basicScore: Int = 1700,
    val parameterMultiplier: Double = 2.3,
    val examScoreMultiplier_0_5000: Double = 0.3,
    val examScoreMultiplier_5000_10000: Double = 0.15,
    val examScoreMultiplier_10000_20000: Double = 0.08,
    val examScoreMultiplier_20000_30000: Double = 0.04,
    val examScoreMultiplier_30000_40000: Double = 0.02,
    val criteriaA: Int = 10001,
    val criteriaAPlus: Int = 11501,
    val criteriaS: Int = 13001,
    val criteriaSPlus: Int = 14501,
    val useMaster: Boolean = false,
)