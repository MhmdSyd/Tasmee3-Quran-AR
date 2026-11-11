package com.tasmee3.app.speech

sealed interface ModelStatus {
    data object Loading : ModelStatus
    data object Missing : ModelStatus
    data class Ready(val loadMillis: Long) : ModelStatus
    data class Failed(val message: String) : ModelStatus
}
