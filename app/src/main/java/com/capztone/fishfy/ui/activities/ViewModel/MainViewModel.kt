// MainViewModel.kt
package com.capztone.fishfy.ui.activities.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // LiveData to hold the user's address
    private val _userAddress = MutableLiveData<String>()
    val userAddress: LiveData<String>
        get() = _userAddress

    // Function to update the user's address using coroutine
    fun updateUserAddress(address: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _userAddress.value = address
        }
    }

    // Add more ViewModel logic as needed
}
