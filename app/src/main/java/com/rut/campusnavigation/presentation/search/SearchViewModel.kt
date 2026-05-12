package com.rut.campusnavigation.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rut.campusnavigation.domain.usecase.SearchResult
import com.rut.campusnavigation.domain.usecase.SearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchUseCase
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Результаты поиска с задержкой 250мс, чтобы не запрашивать на каждый символ. */
    val results: StateFlow<List<SearchResult>> = _query
        .debounce(250)
        .distinctUntilChanged()
        .map { searchUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChanged(q: String) { _query.value = q }
}
