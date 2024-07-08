package com.capztone.seafishfy.ui.activities.adapters

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.HomeDiscountBinding
import com.capztone.seafishfy.ui.activities.Utils.ToastHelper
import com.capztone.seafishfy.ui.activities.models.CartItems
import com.capztone.seafishfy.ui.activities.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeDiscountAdapter(
    private val context: Context
) : RecyclerView.Adapter<HomeDiscountAdapter.HomeDiscountViewHolder>() {

    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val locationsRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Locations").child(userId)
    private var shopNames: List<String> = emptyList()
    private var discountItems: MutableList<DiscountItem> = mutableListOf()
    private val sharedPreferences = context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)

    init {
        locationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shopNameString = snapshot.child("shopname").value?.toString() ?: ""
                shopNames = shopNameString.split(",").map { it.trim() }
                fetchDiscountItems()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeDiscountAdapter", "Failed to read shop names: ${error.message}")
            }
        })
    }

    private fun fetchDiscountItems() {
        for (shopName in shopNames) {
            val shopRef = FirebaseDatabase.getInstance().getReference(shopName)
            shopRef.child("discount").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    try {
                        val discountItem = snapshot.getValue(DiscountItem::class.java)
                        discountItem?.let {
                            discountItems.add(it)
                            it.path = shopName
                            notifyDataSetChanged()
                            Log.d("HomeDiscountAdapter", "Added item: $it")
                        }
                    } catch (e: Exception) {
                        Log.e("HomeDiscountAdapter", "Error parsing discount item: ${e.message}")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle child changed if needed
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // Handle child removed if needed
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle child moved if needed
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeDiscountAdapter", "Fetch discount items cancelled: ${error.message}")
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeDiscountViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = HomeDiscountBinding.inflate(inflater, parent, false)
        return HomeDiscountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeDiscountViewHolder, position: Int) {
        if (position < discountItems.size) {
            val discountItem = discountItems[position]
            holder.bind(discountItem)
        } else {
            Log.e("HomeDiscountAdapter", "Index out of bounds: position $position, size ${discountItems.size}")
        }
    }

    fun updateData(newDiscountItems: List<DiscountItem>) {
        discountItems = newDiscountItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return discountItems.size
    }

    inner class HomeDiscountViewHolder(private val binding: HomeDiscountBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val quantityLiveData = MutableLiveData<Int>()

        init {
            val defaultQuantity = 0
            quantityLiveData.value = defaultQuantity

            binding.quantityy.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: defaultQuantity
                if (currentQuantity == 0) {
                    quantityLiveData.value = 1
                    binding.plusImageButton.visibility = View.VISIBLE
                    binding.minusImageButton.visibility = View.VISIBLE
                    addItemToCart()
                }
            }

            binding.plusImageButton.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: defaultQuantity
                val newQuantity = currentQuantity + 1
                quantityLiveData.value = newQuantity
                saveQuantity(binding.name.text.toString(), newQuantity)
                addItemToCart()
            }

            binding.minusImageButton.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: 0
                if (currentQuantity > 0) {
                    val newQuantity = currentQuantity - 1
                    quantityLiveData.value = newQuantity
                    saveQuantity(binding.name.text.toString(), newQuantity)
                    if (newQuantity == 0) {
                        removeItemFromCart()
                    } else {
                        updateCartItemQuantity(newQuantity)
                        addItemToCart1()
                    }
                }
            }


            (context as? LifecycleOwner)?.let { lifecycleOwner ->
                quantityLiveData.observe(lifecycleOwner, Observer { quantity ->
                    updateQuantityText(quantity)
                })
            }

            updateQuantityText(defaultQuantity)
        }
        private fun updateCartItemQuantity(newQuantity: Int) {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val foodName = binding.name.text.toString()

            if (userId != null && foodName.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (cartSnapshot in dataSnapshot.children) {
                                    cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                        .addOnSuccessListener {
                                            ToastHelper.showCustomToast(
                                                context,
                                                "Item quantity updated in cart successfully"
                                            )
                                        }.addOnFailureListener {
                                            ToastHelper.showCustomToast(
                                                context,
                                                "Failed to update item quantity"
                                            )
                                        }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                        }
                    })
            }
        }
        fun bind(discountItem: DiscountItem) {
            binding.apply {
                // Retrieve user's language from Firebase
                val languageRef = FirebaseDatabase.getInstance().reference
                    .child("user")
                    .child(userId)
                    .child("language")

                languageRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val language = snapshot.getValue(String::class.java) ?: "english"
                        val foodNames = discountItem.foodNames ?: emptyList()

                        // Set food name based on language
                        val displayName = when (language.toLowerCase()) {
                            "english" -> foodNames.getOrNull(0)
                            "tamil" -> foodNames.getOrNull(1)
                            "malayalam" -> foodNames.getOrNull(2)
                            "telugu" -> foodNames.getOrNull(3)
                            else -> foodNames.getOrNull(0) // Default to English
                        }

                        // Bind other data
                        name.text = displayName ?: ""
                        menuPrice1.text = "₹${discountItem.foodPrices}"
                        shopname.text = discountItem.path ?: ""
                        discount.text = "${discountItem.discounts} Off"
                        Glide.with(context).load(Uri.parse(discountItem.foodImages)).into(freshfishImage)
                        freshfishImage.tag = discountItem.foodImages

                        // Set saved quantity if any
                        val foodName = discountItem.foodNames?.getOrNull(0) ?: ""
                        val savedQuantity = getSavedQuantity(foodName)
                        quantityLiveData.value = savedQuantity
                        updateQuantityText(savedQuantity)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HomeDiscountAdapter", "Failed to read language: ${error.message}")
                    }
                })
                binding.root.setOnClickListener {
                    // Handle item click
                }

                val quantityRef = FirebaseDatabase.getInstance().reference
                    .child("user")
                    .child(userId!!)
                    .child("cartItems")
                    .orderByChild("foodName")
                    .equalTo(discountItem.foodNames?.getOrNull(0) ?: "")

                quantityRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (itemSnapshot in snapshot.children) {
                                val foodQuantity =
                                    itemSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 0
                                quantityLiveData.value = foodQuantity
                            }
                        } else {
                            quantityLiveData.value = 0
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("NearItemAdapter", "Database error: ${error.message}")
                    }
                })



                binding.root.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        openDetailsActivity(discountItem,it)
                    }
                }


            }



        }


        private fun updateQuantityText(quantity: Int) {
            binding.quantityy.text = if (quantity > 0) quantity.toString() else "Add"
            binding.plusImageButton.visibility = if (quantity > 0) View.VISIBLE else View.GONE
            binding.minusImageButton.visibility = if (quantity > 0) View.VISIBLE else View.GONE
        }

        private fun saveQuantity(foodName: String, quantity: Int) {
            val editor = sharedPreferences.edit()
            editor.putInt("quantity_$foodName", quantity)
            editor.apply()
        }

        private fun getSavedQuantity(foodName: String): Int {
            return sharedPreferences.getInt("quantity_$foodName", 0)
        }

        private fun addItemToCart() {
            val currentShopName = binding.shopname.text.toString()
            val cartItemsRef = FirebaseAuth.getInstance().currentUser?.uid?.let {
                FirebaseDatabase.getInstance().reference.child("user").child(it).child("cartItems")
            }

            if (cartItemsRef != null) {
                cartItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        var differentShopFound = false

                        for (itemSnapshot in dataSnapshot.children) {
                            val shopName = itemSnapshot.child("path").value as String
                            if (shopName != currentShopName) {
                                differentShopFound = true
                                break
                            }
                        }

                        if (differentShopFound) {
                            showShopChangeDialog(cartItemsRef)
                        } else {
                            addItemToCartWithoutCheck()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("HomeDiscountAdapter", "Add to cart cancelled: ${databaseError.message}")
                    }
                })
            }
        }
        private fun addItemToCart1() {
            val currentShopName = binding.shopname.text.toString()
            val cartItemsRef = FirebaseAuth.getInstance().currentUser?.uid?.let {
                FirebaseDatabase.getInstance().reference.child("user").child(it).child("cartItems")
            }

            if (cartItemsRef != null) {
                cartItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        var differentShopFound = false

                        for (itemSnapshot in dataSnapshot.children) {
                            val shopName = itemSnapshot.child("path").value as String
                            if (shopName != currentShopName) {
                                differentShopFound = true
                                break
                            }
                        }

                        if (differentShopFound) {
                            showShopChangeDialog(cartItemsRef)
                        } else {
                            addItemToCartWithoutCheck1()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("HomeDiscountAdapter", "Add to cart cancelled: ${databaseError.message}")
                    }
                })
            }
        }


        private fun showShopChangeDialog(cartItemsRef: DatabaseReference) {
            val context = binding.root.context
            val layoutInflater = LayoutInflater.from(context)
            val customLayout = layoutInflater.inflate(R.layout.shop_dialog, null)

            val dialog = AlertDialog.Builder(context)
                .setView(customLayout)
                .create()

            val positiveButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_positive_button)
            val negativeButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_negative_button)

            positiveButton.setOnClickListener {
                cartItemsRef.removeValue().addOnSuccessListener {
                    addItemToCartWithoutCheck()
                    dialog.dismiss()
                }
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun addItemToCartWithoutCheck() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName =  binding.shopname.text.toString()
            val discountItem = discountItems.getOrNull(adapterPosition) ?: return
            val foodName = discountItem.foodNames?.getOrNull(0) ?: ""
            val foodPrice = discountItem.foodPrices ?: ""
            val foodImage = discountItem.foodImages ?: ""
            val quantity = quantityLiveData.value ?: 1

            if (userId != null && foodName.isNotEmpty() && foodPrice.isNotEmpty() && foodImage.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (cartSnapshot in dataSnapshot.children) {
                                    val currentQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java)
                                        ?: 1
                                    val newQuantity = currentQuantity + 1
                                    cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                        .addOnSuccessListener {
                                            ToastHelper.showCustomToast(context, "Item quantity updated in cart successfully")
                                        }.addOnFailureListener {
                                            ToastHelper.showCustomToast(context, "Failed to update item quantity")
                                        }
                                }
                            } else {
                                val cartItem = CartItems(
                                    shopName,
                                    foodName,
                                    foodPrice,
                                    discountItem.foodDescriptions,
                                    foodImage,
                                    quantity
                                )
                                cartItemsRef.push().setValue(cartItem)
                                    .addOnSuccessListener {
                                        ToastHelper.showCustomToast(context, "Item added to cart successfully")
                                    }.addOnFailureListener {
                                        ToastHelper.showCustomToast(context, "Failed to add item")
                                    }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("HomeDiscountAdapter", "Add to cart cancelled: ${databaseError.message}")
                        }
                    })
            }
        }
        private fun addItemToCartWithoutCheck1() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName =  binding.shopname.text.toString()
            val discountItem = discountItems.getOrNull(adapterPosition) ?: return
            val foodName = discountItem.foodNames?.getOrNull(0) ?: ""
            val foodPrice = discountItem.foodPrices ?: ""
            val foodImage = discountItem.foodImages ?: ""
            val quantity = quantityLiveData.value ?: 1

            if (userId != null && foodName.isNotEmpty() && foodPrice.isNotEmpty() && foodImage.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (cartSnapshot in dataSnapshot.children) {
                                    val currentQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java)
                                        ?: 1
                                    val newQuantity = currentQuantity - 1
                                    cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                        .addOnSuccessListener {
                                            ToastHelper.showCustomToast(context, "Item quantity updated in cart successfully")
                                        }.addOnFailureListener {
                                            ToastHelper.showCustomToast(context, "Failed to update item quantity")
                                        }
                                }
                            } else {
                                val cartItem = CartItems(
                                    shopName,
                                    foodName,
                                    foodPrice,
                                    discountItem.foodDescriptions,
                                    foodImage,
                                    quantity
                                )
                                cartItemsRef.push().setValue(cartItem)
                                    .addOnSuccessListener {
                                        ToastHelper.showCustomToast(context, "Item added to cart successfully")
                                    }.addOnFailureListener {
                                        ToastHelper.showCustomToast(context, "Failed to add item")
                                    }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("HomeDiscountAdapter", "Add to cart cancelled: ${databaseError.message}")
                        }
                    })
            }
        }

        private fun removeItemFromCart() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName = shopNames.getOrNull(adapterPosition) ?: ""
            val discountItem = discountItems.getOrNull(adapterPosition) ?: return
            val foodName = discountItem.foodNames?.getOrNull(0) ?: ""
            val cartItemsRef = userId?.let { database.child("user").child(it).child("cartItems") }

            if (cartItemsRef != null) {
                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (cartSnapshot in dataSnapshot.children) {
                                    val currentQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java)
                                        ?: 1
                                    if (currentQuantity > 1) {
                                        cartSnapshot.ref.child("foodQuantity").setValue(currentQuantity - 1)
                                            .addOnSuccessListener {
                                                ToastHelper.showCustomToast(context, "Item quantity updated in cart successfully")
                                            }.addOnFailureListener {
                                                ToastHelper.showCustomToast(context, "Failed to update item quantity")
                                            }
                                    } else {
                                        cartSnapshot.ref.removeValue()
                                            .addOnSuccessListener {
                                                ToastHelper.showCustomToast(context, "Item removed from cart successfully")
                                            }.addOnFailureListener {
                                                ToastHelper.showCustomToast(context, "Failed to remove item")
                                            }
                                    }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("HomeDiscountAdapter", "Remove from cart cancelled: ${databaseError.message}")
                        }
                    })
            }
        }
    }

    private fun openDetailsActivity(discountItem: DiscountItem, view: View) {
        val bundle = Bundle().apply {
            putString("DiscountItemName", discountItem.foodNames?.getOrNull(0) ?: "")
            putString("DiscountItemPrice", discountItem.foodPrices)
            putString("DiscountItemDescription", discountItem.foodDescriptions)
            putString("DiscountItemImage", discountItem.foodImages)
            putString("DiscountAmount", discountItem.discounts)
            putString("DiscountQuantity", discountItem.productQuantity)
        }

        view.findNavController().navigate(R.id.action_homeFragment_to_detailsFragment, bundle)
    }
}