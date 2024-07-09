package com.capztone.seafishfy.ui.activities.ViewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FreshFishViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    val menuItemsLiveData: MutableLiveData<List<MenuItem>> = MutableLiveData()

    fun retrieveMenuItems() {
        val userId = auth.currentUser?.uid ?: return

        // Fetch user data including category and language
        database.getReference("user").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val category = userSnapshot.child("category").getValue(String::class.java) ?: ""
                    val userLanguage = userSnapshot.child("language").getValue(String::class.java)?.toLowerCase()
                        ?: "english"

                    // Fetch shopnames string from Locations
                    val locationsRef = database.getReference("Locations").child(userId)
                    locationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(locationsSnapshot: DataSnapshot) {
                            val shopNamesString = locationsSnapshot.child("shopname").getValue(String::class.java)
                            shopNamesString?.let { shopNames ->
                                val shopNamesList = shopNames.split(",").map { it.trim() }
                                val menuItems = mutableListOf<MenuItem>()

                                // Iterate through each shop name to retrieve menu items based on category
                                shopNamesList.forEach { shopName ->
                                    val menuPath = getCategoryMenuPath(category)

                                    menuPath?.let { path ->
                                        // Fetch menu items from the determined menu path for each shop
                                        val menuRef = database.getReference(shopName).child(path)

                                        menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(menuSnapshot: DataSnapshot) {
                                                if (menuSnapshot.exists()) {
                                                    for (itemSnapshot in menuSnapshot.children) {
                                                        val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                                                        menuItem?.let {
                                                            it.path = shopName
                                                            // Retrieve the foodNames in English and user-selected language
                                                            val foodNamesList = it.foodName ?: arrayListOf("", "", "", "")
                                                            val englishName = foodNamesList.getOrNull(0) ?: ""
                                                            val languageSpecificName = when (userLanguage) {
                                                                "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                                                "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                                                "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                                                else -> englishName // Default to English
                                                            }

                                                            // Create a combined name with both English and language-specific names
                                                            val combinedName =
                                                                "$englishName / $languageSpecificName"

                                                            // Update the foodName list in MenuItem
                                                            it.foodName = arrayListOf(combinedName)

                                                            // Add the menuItem to the list
                                                            menuItems.add(it)
                                                        }
                                                    }
                                                }
                                                // Post updated menu items to LiveData after processing all menu items
                                                menuItemsLiveData.postValue(menuItems)
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                // Handle onCancelled
                                                Log.e("FreshFishViewModel", "Failed to retrieve menu items for $shopName: ${error.message}")
                                            }
                                        })
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle onCancelled
                            Log.e("FreshFishViewModel", "Failed to retrieve shop names: ${error.message}")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled
                    Log.e("FreshFishViewModel", "Failed to retrieve user data: ${error.message}")
                }
            })
    }

    // Helper function to get menu path based on category
    private fun getCategoryMenuPath(category: String): String? {
        // Map category to menu path according to your requirements
        return when (category.toLowerCase()) {
            "crabs" -> "menu4"
            "dry fish" -> "menu1"
            "pickle" -> "menu2"
            "shrimps" -> "menu3"
            "lobster" -> "menu5"
            "fresh fish" -> "menu"
            else -> null // Handle unknown category if needed
        }
    }
}
