package com.mhss.app.tracking.presentation.csv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.tracking.data.csv.TrackingCsvCounts
import com.mhss.app.tracking.data.csv.TrackingCsvErrorReason
import com.mhss.app.tracking.data.csv.TrackingCsvException
import com.mhss.app.tracking.data.csv.TrackingCsvImportPreview
import com.mhss.app.tracking.data.csv.TrackingCsvManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class TrackingCsvUiError(
    val reason: TrackingCsvErrorReason,
    val lineNumber: Int?,
    val detail: String?
)

sealed interface TrackingCsvStatus {
    data class Exported(
        val fileName: String,
        val counts: TrackingCsvCounts
    ) : TrackingCsvStatus

    data class Imported(val counts: TrackingCsvCounts) : TrackingCsvStatus
}

data class TrackingCsvUiState(
    val isWorking: Boolean = false,
    val preview: TrackingCsvImportPreview? = null,
    val status: TrackingCsvStatus? = null,
    val error: TrackingCsvUiError? = null
)

@KoinViewModel
class TrackingCsvViewModel(
    private val manager: TrackingCsvManager
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(TrackingCsvUiState())
    val uiState: StateFlow<TrackingCsvUiState> = mutableUiState.asStateFlow()

    fun export(directoryUri: String) = runOperation {
        val result = manager.export(directoryUri)
        mutableUiState.update {
            it.copy(
                status = TrackingCsvStatus.Exported(result.fileName, result.counts),
                error = null
            )
        }
    }

    fun previewImport(fileUri: String) = runOperation {
        val preview = manager.preview(fileUri)
        mutableUiState.update {
            it.copy(preview = preview, status = null, error = null)
        }
    }

    fun confirmImport() {
        val preview = mutableUiState.value.preview ?: return
        runOperation {
            val counts = manager.import(preview)
            mutableUiState.update {
                it.copy(
                    preview = null,
                    status = TrackingCsvStatus.Imported(counts),
                    error = null
                )
            }
        }
    }

    fun dismissPreview() {
        if (!mutableUiState.value.isWorking) {
            mutableUiState.update { it.copy(preview = null) }
        }
    }

    fun dismissMessage() {
        mutableUiState.update { it.copy(status = null, error = null) }
    }

    private fun runOperation(block: suspend () -> Unit) {
        if (mutableUiState.value.isWorking) return
        mutableUiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: TrackingCsvException) {
                mutableUiState.update {
                    it.copy(
                        error = TrackingCsvUiError(
                            reason = exception.reason,
                            lineNumber = exception.lineNumber,
                            detail = exception.detail
                        )
                    )
                }
            } catch (exception: Exception) {
                mutableUiState.update {
                    it.copy(
                        error = TrackingCsvUiError(
                            reason = TrackingCsvErrorReason.READ_FAILED,
                            lineNumber = null,
                            detail = exception.message
                        )
                    )
                }
            } finally {
                mutableUiState.update { it.copy(isWorking = false) }
            }
        }
    }
}
