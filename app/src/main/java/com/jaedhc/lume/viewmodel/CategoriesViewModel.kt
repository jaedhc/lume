package com.jaedhc.lume.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaedhc.lume.data.db.CategoryEntity
import com.jaedhc.lume.data.db.LumeDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val nameExistsError: Boolean = false
)

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    // Optimized filtering: only recalculates when categories or searchQuery change
    val filteredCategories: StateFlow<List<CategoryEntity>> = _uiState.map { state ->
        val query = state.searchQuery.trim()
        if (query.isEmpty()) {
            state.categories
        } else {
            state.categories.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value.categories)

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess: SharedFlow<Unit> = _saveSuccess.asSharedFlow()

    init {
        observeCategories()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            transactionDao.getAllCategories().map { list ->
                list.filter { it.id != "finanzas" }
            }.collect { categories ->
                _uiState.update { it.copy(categories = categories, isLoading = false) }
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(searchQuery = newQuery)
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.categories.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return
        
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        
        // Update state immediately for UI feedback
        _uiState.update { it.copy(categories = currentList) }
        
        // Save new order to database
        updateCategoriesOrder(currentList)
    }

    private fun updateCategoriesOrder(list: List<CategoryEntity>) {
        viewModelScope.launch {
            list.forEachIndexed { index, category ->
                transactionDao.updateCategory(category.copy(displayOrder = index))
            }
        }
    }

    fun saveCategory(name: String, icon: String, color: String) {
        val exists = _uiState.value.categories.any { it.name.equals(name, ignoreCase = true) }
        if (exists) {
            _uiState.update { it.copy(nameExistsError = true) }
            return
        }

        viewModelScope.launch {
            val currentMaxOrder = _uiState.value.categories.maxOfOrNull { it.displayOrder } ?: -1
            val newCategory = CategoryEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                icon = icon,
                color = color,
                displayOrder = currentMaxOrder + 1
            )
            transactionDao.insertCategory(newCategory)
            _uiState.update { it.copy(nameExistsError = false) }
            _saveSuccess.emit(Unit)
        }
    }

    fun clearErrors() {
        _uiState.update { it.copy(nameExistsError = false) }
    }
}
