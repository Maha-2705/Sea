package com.capztone.fishfy.ui.activities.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.capztone.fishfy.ui.activities.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShopOneViewModel : ViewModel() {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private val _menuItems = MutableLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>>
        get() = _menuItems

    private val _menu1Items = MutableLiveData<List<MenuItem>>()
    val menu1Items: LiveData<List<MenuItem>>
        get() = _menu1Items

    private val _menu2Items = MutableLiveData<List<MenuItem>>()
    val menu2Items: LiveData<List<MenuItem>>
        get() = _menu2Items

    private val _menu3Items = MutableLiveData<List<MenuItem>>()
    val menu3Items: LiveData<List<MenuItem>>
        get() = _menu3Items

    private val _menu4Items = MutableLiveData<List<MenuItem>>()
    val menu4Items: LiveData<List<MenuItem>>
        get() = _menu4Items

    private val _menu5Items = MutableLiveData<List<MenuItem>>()
    val menu5Items: LiveData<List<MenuItem>>
        get() = _menu5Items

    private val _discountItems = MutableLiveData<List<DiscountItem>>()
    val discountItems: LiveData<List<DiscountItem>>
        get() = _discountItems

    fun retrieveData(shopName: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                val userRef = database.getReference("user").child(user.uid).child("language")

                val languageSnapshot = userRef.get().await()
                val userLanguage =
                    languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"

                retrieveShopData(shopName, userLanguage)
            }
        }
    }

    private suspend fun retrieveShopData(shopName: String, userLanguage: String) {
        try {
            val shopRef = database.getReference(shopName)
            val snapshot = shopRef.get().await()
            if (snapshot.exists()) {
                retrieveMenuItems(shopRef.child("menu"), _menuItems, userLanguage,shopName)
                retrieveMenuItems(shopRef.child("menu1"), _menu1Items, userLanguage,shopName)
                retrieveMenuItems(shopRef.child("menu2"), _menu2Items, userLanguage,shopName)
                retrieveMenuItems(shopRef.child("menu4"), _menu3Items, userLanguage,shopName)
                retrieveMenuItems(shopRef.child("menu3"), _menu4Items, userLanguage,shopName)
                retrieveMenuItems(shopRef.child("menu5"), _menu5Items, userLanguage,shopName)
                retrieveDiscountItems(shopRef.child("discount"),_discountItems,userLanguage,shopName)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    private suspend fun retrieveMenuItems(
        menuRef: DatabaseReference,
        menuLiveData: MutableLiveData<List<MenuItem>>,
        userLanguage: String,shopName: String
    ) {
        try {
            val snapshot = menuRef.get().await()
            val menuItems = mutableListOf<MenuItem>()
            for (itemSnapshot in snapshot.children) {
                val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                menuItem?.let {

                    it.path = shopName
                    val foodNamesList = it.foodName ?: arrayListOf()
                    val englishName = foodNamesList.getOrNull(0) ?: ""
                    val languageSpecificName = when (userLanguage) {
                        "tamil" -> foodNamesList.getOrNull(1) ?: ""
                        "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                        "telugu" -> foodNamesList.getOrNull(3) ?: ""
                        else -> englishName // Default to English
                    }

                    // Create a combined name with both English and language-specific names
                    val combinedName = if (userLanguage == "english") {
                        englishName
                    } else {
                        "$englishName / $languageSpecificName"
                    }

                    // Add the combined name to the foodName list
                    it.foodName = arrayListOf(combinedName)
                    menuItems.add(it)
                }
            }
            menuLiveData.postValue(menuItems)
        } catch (e: Exception) {
            // Handle error
        }
    }

    private suspend fun retrieveDiscountItems(shopRef: DatabaseReference, menuLiveData: MutableLiveData<List<DiscountItem>>,
                                              userLanguage: String,shopName: String) {
        try {
            val snapshot = shopRef.get().await()
            val menuItems = mutableListOf<DiscountItem>()
            for (itemSnapshot in snapshot.children) {
                val menuItem = itemSnapshot.getValue(DiscountItem::class.java)
                menuItem?.let {
                    it.path = shopName

                    val foodNamesList = it.foodNames ?: arrayListOf()
                    val englishName = foodNamesList.getOrNull(0) ?: ""
                    val languageSpecificName = when (userLanguage) {
                        "tamil" -> foodNamesList.getOrNull(1) ?: ""
                        "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                        "telugu" -> foodNamesList.getOrNull(3) ?: ""
                        else -> englishName // Default to English
                    }
                    val combinedName = "$englishName / $languageSpecificName"
                    it.foodNames = arrayListOf(combinedName)

                    menuItems.add(it)
                }
            }
            menuLiveData.postValue(menuItems)
        } catch (e: Exception) {
            // Handle error
        }
    }
}
