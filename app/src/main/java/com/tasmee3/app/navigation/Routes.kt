package com.tasmee3.app.navigation

object Routes {
    const val READER = "reader"
    const val RECITATION = "recitation/{page}"
    const val SUMMARY = "summary"
    const val SETTINGS = "settings"
    const val MODEL_TEST = "model_test"

    fun recitation(page: Int) = "recitation/$page"

    val all = listOf(READER, RECITATION, SUMMARY, SETTINGS, MODEL_TEST)
}
