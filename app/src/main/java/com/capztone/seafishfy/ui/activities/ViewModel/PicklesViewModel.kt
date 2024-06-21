package com.capztone.seafishfy.ui.activities.ViewModel



import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PicklesViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()


    val menuItemsLiveData: MutableLiveData<List<MenuItem>> = MutableLiveData()


    fun retrieveMenuItems() {
        val userId = auth.currentUser?.uid ?: return

        // Fetch user language first
        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage =
                        languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"

                    val userLocationRef = database.getReference("Locations").child(userId)

                    userLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val shopNamesString = snapshot.child("shopname").getValue(String::class.java)
                            shopNamesString?.let { shopNames ->
                                val shopNamesList = shopNames.split(",").map { it.trim() }
                                val menuItems = mutableListOf<MenuItem>()

                                shopNamesList.forEach { shopName ->
                                    val menuRefs = listOf(
                                        database.getReference(shopName).child("menu2"),

                                        // Add more menu references as needed
                                    )

                                    menuRefs.forEach { menuRef ->
                                        menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(menuSnapshot: DataSnapshot) {
                                                if (menuSnapshot.exists()) {
                                                    for (itemSnapshot in menuSnapshot.children) {
                                                        val menuItem =
                                                            itemSnapshot.getValue(MenuItem::class.java)
                                                        menuItem?.let {
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
                                                // Post updated menu items to LiveData after processing all menuRefs
                                                menuItemsLiveData.postValue(menuItems)
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                // Handle onCancelled
                                            }
                                        })
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle onCancelled
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled
                }
            })
    }
}
