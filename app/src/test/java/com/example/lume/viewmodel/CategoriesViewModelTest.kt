package com.example.lume.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.lume.data.db.CategoryEntity
import com.example.lume.data.db.LumeDatabase
import com.example.lume.data.db.TransactionDao
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val app: Application = mockk(relaxed = true)
    private val database: LumeDatabase = mockk()
    private val dao: TransactionDao = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock the static Database getter
        mockkStatic(LumeDatabase::class)
        every { LumeDatabase.getDatabase(any()) } returns database
        every { database.transactionDao() } returns dao
        
        // Default behavior for DAO
        every { dao.getAllCategories() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `init should fetch categories and filter out finanzas`() = runTest {
        // Given a list with 'finanzas' and others
        val categories = listOf(
            CategoryEntity("comida", "Comida", "icon", "color", 0),
            CategoryEntity("finanzas", "Finanzas", "icon", "color", 1)
        )
        every { dao.getAllCategories() } returns flowOf(categories)

        // When ViewModel is initialized
        val viewModel = CategoriesViewModel(app)
        
        // Then uiState should contain only 'comida'
        // We use testDispatcher.scheduler.advanceUntilIdle() because of viewModelScope.launch in init
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.categories.size)
            assertEquals("comida", state.categories[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchQueryChange updates the state`() = runTest {
        val viewModel = CategoriesViewModel(app)
        
        viewModel.onSearchQueryChange("test")
        
        assertEquals("test", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `saveCategory should show error if name duplicate`() = runTest {
        val existing = listOf(CategoryEntity("1", "Comida", "icon", "color", 0))
        every { dao.getAllCategories() } returns flowOf(existing)
        
        val viewModel = CategoriesViewModel(app)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.saveCategory("Comida", "icon", "color")
        
        assertTrue(viewModel.uiState.value.nameExistsError)
        coVerify(exactly = 0) { dao.insertCategory(any()) }
    }
}

// Extension to avoid repeating assertTrue with msg
fun assertTrue(value: Boolean) = assertEquals(true, value)
