// CartViewModel.kt

package com.capztone.seafishfy.ui.activities.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capztone.seafishfy.ui.activities.adapters.CartAdapter
import com.capztone.seafishfy.ui.activities.models.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var userId: String = auth.currentUser?.uid ?: ""
    private val _totalAmount = MutableLiveData<Int>()
    val totalAmount: LiveData<Int> get() = _totalAmount



    init {
        calculateTotal()
    }


    private fun calculateTotal() {
        val userId = auth.currentUser?.uid ?: ""
        val cartItemsReference = database.reference.child("user").child(userId).child("cartItems")

        cartItemsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var total = 0.0
                for (cartSnapshot in snapshot.children) {
                    val quantity = cartSnapshot.child("quantity").getValue(Int::class.java) ?: 1 // Default quantity is 1
                    val foodPriceString = cartSnapshot.child("foodPrice").getValue(String::class.java) ?: "0.0"
                    val foodPrice = foodPriceString.toDoubleOrNull() ?: 0.0
                    total += quantity * foodPrice
                }
                _totalAmount.value = total.toInt()// Convert total to integer and then back to double
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error appropriately
            }
        })
    }



    fun retrieveCartItems(
        cartItemsCallback: (foodNames: MutableList<String>, foodPrices: MutableList<String>, foodDescriptions: MutableList<String>, foodIngredients: MutableList<String>, foodImageUri: MutableList<String>, paths: MutableList<String>, quantity: MutableList<Int>) -> Unit
    ) {
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
                        }
                    }
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
}