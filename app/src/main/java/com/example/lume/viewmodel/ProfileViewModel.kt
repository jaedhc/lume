package com.example.lume.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lume.data.db.LumeDatabase
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            transactionDao.clearAllUserData()
            onComplete()
        }
    }
}
