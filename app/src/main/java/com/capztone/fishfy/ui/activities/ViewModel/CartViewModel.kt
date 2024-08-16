package com.capztone.fishfy.ui.activities.ViewModel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capztone.fishfy.ui.activities.adapters.CartAdapter
import com.capztone.fishfy.ui.activities.models.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class CartViewModel(private val context: Context) : ViewModel() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var userId: String = auth.currentUser?.uid ?: ""
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun saveCartItemsToLocal(cartItems: List<CartItems>) {
        val json = gson.toJson(cartItems)
        sharedPreferences.edit().putString("cart_items", json).apply()
    }

    private fun retrieveCartItemsFromLocal(): List<CartItems> {
        val json = sharedPreferences.getString("cart_items", null) ?: return emptyList()
        val type = object : com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken<List<CartItems>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    fun retrieveCartItems(
        cartItemsCallback: (foodNames: MutableList<String>, foodPrices: MutableList<String>, foodDescriptions: MutableList<String>, foodIngredients: MutableList<String>, foodImageUri: MutableList<String>, paths: MutableList<String>, quantity: MutableList<Int>) -> Unit
    ) {
        if (isNetworkAvailable()) {
            viewModelScope.launch {
                val foodReferencer: DatabaseReference =
                    database.reference.child("user").child(userId).child("cartItems")

                val foodNames = mutableListOf<String>()
                val foodPrices = mutableListOf<String>()
                val foodDescriptions = mutableListOf<String>()
                val foodIngredients = mutableListOf<String>()
                val foodImageUri = mutableListOf<String>()
                val paths = mutableListOf<String>()
                val quantity = mutableListOf<Int>()

                foodReferencer.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val cartItemsList = mutableListOf<CartItems>()
                        for (foodSnapshot in snapshot.children) {
                            val cartItems = foodSnapshot.getValue(CartItems::class.java)
                            cartItems?.let {
                                foodNames.add(it.foodName.toString())
                                foodPrices.add("₹" + it.foodPrice.toString())
                                foodDescriptions.add(it.foodDescription.toString())
                                foodIngredients.add(it.foodIngredients.toString())
                                foodImageUri.add(it.foodImage.toString())
                                paths.add(it.path.toString()) // Add path to the list
                                it.foodQuantity?.let { it1 -> quantity.add(it1) }
                                cartItemsList.add(it)
                            }
                        }
                        // Save retrieved items to local storage
                        saveCartItemsToLocal(cartItemsList)

                        cartItemsCallback(
                            foodNames,
                            foodPrices,
                            foodDescriptions,
                            foodIngredients,
                            foodImageUri,
                            paths,
                            quantity
                        )
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
            }
        } else {
            // Retrieve items from local storage
            val localCartItems = retrieveCartItemsFromLocal()
            val foodNames = localCartItems.map { it.foodName.toString() }.toMutableList()
            val foodPrices = localCartItems.map { "₹" + it.foodPrice.toString() }.toMutableList()
            val foodDescriptions = localCartItems.map { it.foodDescription.toString() }.toMutableList()
            val foodIngredients = localCartItems.map { it.foodIngredients.toString() }.toMutableList()
            val foodImageUri = localCartItems.map { it.foodImage.toString() }.toMutableList()
            val paths = localCartItems.map { it.path.toString() }.toMutableList()
            val quantity = localCartItems.mapNotNull { it.foodQuantity }.toMutableList()

            cartItemsCallback(
                foodNames,
                foodPrices,
                foodDescriptions,
                foodIngredients,
                foodImageUri,
                paths,
                quantity
            )
        }
    }

    fun removeCartItem(itemId: String) {
        viewModelScope.launch {
            val itemReference: DatabaseReference =
                database.reference.child("user").child(userId).child("cartItems").child(itemId)
            itemReference.removeValue()
        }
    }

    fun isCartEmpty(cartItemsCallback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cartReference: DatabaseReference =
                database.reference.child("user").child(userId).child("cartItems")
            cartReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cartItemsCallback(!snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    fun getOrderItemsDetail(
        cartAdapter: CartAdapter,
        orderNowCallback: (foodName: MutableList<String>, foodPrice: MutableList<String>, foodDescription: MutableList<String>, foodIngredient: MutableList<String>, foodImage: MutableList<String>, foodQuantities: MutableList<Int>) -> Unit
    ) {
        viewModelScope.launch {
            val orderIdReference: DatabaseReference =
                database.reference.child("user").child(userId).child("cartItems")

            val foodQuantities = cartAdapter.getUpdatedItemsQuantities()
            val foodName = mutableListOf<String>()
            val foodPrice = mutableListOf<String>()
            val foodDescription = mutableListOf<String>()
            val foodIngredient = mutableListOf<String>()
            val foodImage = mutableListOf<String>()

            orderIdReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (foodSnapshot in snapshot.children) {
                        val orderItems = foodSnapshot.getValue(CartItems::class.java)
                        orderItems?.let {
                            foodName.add(it.foodName.toString())
                            foodPrice.add(it.foodPrice.toString())
                            foodDescription.add(it.foodDescription.toString())
                            foodIngredient.add(it.foodIngredients.toString())
                            foodImage.add(it.foodImage.toString())
                        }
                    }
                    orderNowCallback(
                        foodName,
                        foodPrice,
                        foodDescription,
                        foodIngredient,
                        foodImage,
                        foodQuantities
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    fun checkAndDeleteCartItem(cartItem: CartItems, quantityText: String) {
        val itemReference: DatabaseReference =
            cartItem.path?.let {
                database.reference.child("user").child(userId).child("cartItems").child(
                    it
                )
            }!!

        itemReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentQuantity = snapshot.child("foodQuantity").getValue(Int::class.java) ?: 0
                if (quantityText == "Add" && currentQuantity <= 0) {
                    itemReference.removeValue()
                        .addOnSuccessListener {
                            // Handle successful deletion
                        }
                        .addOnFailureListener {
                            // Handle failure
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}
