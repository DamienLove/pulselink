package com.RingerSong.free.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RingerViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RingerViewModel::class.java)) {
            return RingerViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
