package com.mhss.app.mybrain.content.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhss.app.mybrain.content.domain.ContentItem
import com.mhss.app.mybrain.content.domain.ContentLibraryRepository
import com.mhss.app.mybrain.content.domain.ContentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ContentLibraryViewModel(
    private val repository: ContentLibraryRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedType = MutableStateFlow<ContentType?>(null)

    val uiState = combine(query, selectedType, ::ContentRequest)
        .flatMapLatest { request ->
            repository.observeItems(
                type = request.type,
                query = request.query
            ).map { items ->
                ContentLibraryUiState(
                    items = items,
                    query = request.query,
                    selectedType = request.type
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ContentLibraryUiState()
        )

    fun updateQuery(value: String) {
        query.value = value
    }

    fun selectType(type: ContentType?) {
        selectedType.value = type
    }
}

data class ContentLibraryUiState(
    val items: List<ContentItem> = emptyList(),
    val query: String = "",
    val selectedType: ContentType? = null
)

private data class ContentRequest(
    val query: String,
    val type: ContentType?
)
