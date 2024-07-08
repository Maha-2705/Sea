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
import com.capztone.seafishfy.databinding.FreshFishBinding
import com.capztone.seafishfy.ui.activities.Utils.ToastHelper
import com.capztone.seafishfy.ui.activities.models.CartItems
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class  FreshFishAdapter(
    private var menuItems: MutableList<MenuItem>,

    private val context: Context
) : RecyclerView.Adapter<FreshFishAdapter.ViewHolder>() {
    private val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    private val sharedPreferences = context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
    private val seenFoodNames = mutableSetOf<String>()
    private val favoriteItemsKey = "favoriteItems"
    private var foodDescription: String? = null

    fun updateMenuItems(newMenuItems: List<MenuItem>) {
        menuItems.clear()
        seenFoodNames.clear()
        newMenuItems.forEach { menuItem ->
            if (!seenFoodNames.contains(menuItem.foodName?.getOrNull(0) ?: "")) {
                menuItems.add(menuItem)
                seenFoodNames.add(menuItem.foodName?.getOrNull(0) ?: "")
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =  FreshFishBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menuItem = menuItems[position]

        holder.bind(menuItem)
    }

    fun updateData(newItems: List<MenuItem>) {
        menuItems = newItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return menuItems.size
    }

    inner class ViewHolder(private val binding: FreshFishBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val quantityLiveData = MutableLiveData<Int>()

        init {
            val defaultQuantity = 0
            quantityLiveData.value = defaultQuantity

            binding.quantityy.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: defaultQuantity
                if (currentQuantity == 0) {
                    quantityLiveData.value = 1 // Set to 1 when clicking "Add" for the first time
                    binding.plusImageButton.visibility = View.VISIBLE
                    binding.minusImageButton.visibility = View.VISIBLE
                    addItemToCart()
                }
            }

            binding.plusImageButton.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: defaultQuantity
                val newQuantity = currentQuantity + 1
                quantityLiveData.value = newQuantity
                saveQuantity(binding.menuFoodName1.text.toString(), newQuantity)
                addItemToCart()
            }



            binding.minusImageButton.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: 0
                if (currentQuantity > 0) {
                    val newQuantity = currentQuantity - 1
                    quantityLiveData.value = newQuantity
                    saveQuantity(binding.menuFoodName1.text.toString(), newQuantity)
                    if (newQuantity == 0) {
                        removeItemFromCart()
                    } else {
                        updateCartItemQuantity(newQuantity)
                        addItemToCart1()
                    }
                }
            }


            // Ensure context is a LifecycleOwner before observing
            (context as? LifecycleOwner)?.let { lifecycleOwner ->
                quantityLiveData.observe(lifecycleOwner, Observer { quantity ->
                    updateQuantityText(quantity)
                })
            }

            // Initialize visibility based on initial quantity
            updateQuantityText(defaultQuantity)
        }

        private fun updateCartItemQuantity(newQuantity: Int) {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val foodName =
                binding.menuFoodName1.text.toString() + binding.menuFoodName2.text.toString()

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

        private fun removeItemFromCart() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val foodName =
                binding.menuFoodName1.text.toString() + binding.menuFoodName2.text.toString()

            if (userId != null && foodName.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Item in cart, remove it
                                for (cartSnapshot in dataSnapshot.children) {
                                    cartSnapshot.ref.removeValue()
                                        .addOnSuccessListener {
                                            ToastHelper.showCustomToast(
                                                context,
                                                "Item removed from cart successfully"
                                            )
                                        }.addOnFailureListener {
                                            ToastHelper.showCustomToast(
                                                context,
                                                "Failed to remove item"
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
            val cartItemsRef = currentUserID?.let {
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
                            val context = binding.root.context
                            val layoutInflater = LayoutInflater.from(context)
                            val customLayout = layoutInflater.inflate(R.layout.shop_dialog, null)

                            val dialog = AlertDialog.Builder(context, R.style.CustomDialogg)
                                .setView(customLayout)
                                .create()

                            // Set background to transparent
                            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                            val positiveButton =
                                customLayout.findViewById<AppCompatButton>(R.id.dialog_positive_button)
                            val negativeButton =
                                customLayout.findViewById<AppCompatButton>(R.id.dialog_negative_button)

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
                        } else {
                            addItemToCartWithoutCheck()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
            }
        }


        private fun addItemToCartWithoutCheck() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName = binding.shopname.text.toString()
            val foodName =
                binding.menuFoodName1.text.toString() + binding.menuFoodName2.text.toString()
            val foodPrice = binding.menuPrice1.text.toString().removePrefix("₹")
            val foodImage = binding.nearImage.tag.toString()
            val quantity = quantityLiveData.value ?: 1

            if (userId != null && foodName.isNotEmpty() && foodPrice.isNotEmpty() && foodImage.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Item already in cart, update quantity
                                for (cartSnapshot in dataSnapshot.children) {
                                    val currentQuantity =
                                        cartSnapshot.child("foodQuantity").getValue(Int::class.java)
                                            ?: 1
                                    val newQuantity = currentQuantity + 1
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
                            } else {
                                // Item not in cart, add new item

                                val cartItem = CartItems(
                                    shopName,
                                    foodName,
                                    foodPrice,
                                    foodDescription,
                                    foodImage,
                                    quantity
                                )
                                cartItemsRef.push().setValue(cartItem)
                                    .addOnSuccessListener {
                                        ToastHelper.showCustomToast(
                                            context,
                                            "Item added to cart successfully"
                                        )
                                    }
                                    .addOnFailureListener {
                                        ToastHelper.showCustomToast(
                                            context,
                                            "Failed to add item to cart"
                                        )
                                    }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                        }
                    })
            }
        }

        private fun addItemToCart1() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName = binding.shopname.text.toString()
            val foodName =
                binding.menuFoodName1.text.toString() + binding.menuFoodName2.text.toString()
            val foodPrice = binding.menuPrice1.text.toString().removePrefix("₹")
            val foodImage = binding.nearImage.tag.toString()
            val quantity = quantityLiveData.value ?: 1

            if (userId != null && foodName.isNotEmpty() && foodPrice.isNotEmpty() && foodImage.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Item already in cart, update quantity
                                for (cartSnapshot in dataSnapshot.children) {
                                    val currentQuantity =
                                        cartSnapshot.child("foodQuantity").getValue(Int::class.java)
                                            ?: 1
                                    val newQuantity = currentQuantity - 1
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


        fun bind(menuItem: MenuItem) {
            if (menuItem != null) {
                binding.apply {
                    menuFoodName1.text = menuItem.foodName?.getOrNull(0) ?: ""
                    menuFoodName2.text = menuItem.foodName?.getOrNull(1) ?: ""
                    Qty.text= menuItem.productQuantity


                    // Check if foodPrice is non-null and not empty
                    menuPrice1.text = "₹${menuItem.foodPrice}"

                    shopname.text = menuItem.path ?: ""
                    foodDescription = menuItem.foodDescription

                    // Check if foodImage is non-null and not empty
                    Glide.with(context).load(Uri.parse(menuItem.foodImage)).into(nearImage)
                    nearImage.tag = menuItem.foodImage

                    val foodName = menuItem.foodName?.getOrNull(0) ?: ""
                    val savedQuantity = getSavedQuantity(foodName)

                    quantityLiveData.value = savedQuantity
                    updateQuantityText(savedQuantity)
                }
                binding.root.setOnClickListener {
                    // Handle item click
                }

                val quantityRef = FirebaseDatabase.getInstance().reference
                    .child("user")
                    .child(currentUserID!!)
                    .child("cartItems")
                    .orderByChild("foodName")
                    .equalTo(menuItem.foodName?.getOrNull(0) ?: "")

                quantityRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (itemSnapshot in snapshot.children) {
                                val foodQuantity =
                                    itemSnapshot.child("foodQuantity").getValue(Int::class.java)
                                        ?: 0
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
                        openDetailsActivity(it, position)
                    }
                }


            }


        }


        private fun saveFavoriteState(itemId: String, isFavorite: Boolean) {
            val favoritesSet =
                sharedPreferences.getStringSet(favoriteItemsKey, mutableSetOf()) ?: mutableSetOf()
            if (isFavorite) {
                favoritesSet.add(itemId)
            } else {
                favoritesSet.remove(itemId)
            }
            sharedPreferences.edit().putStringSet(favoriteItemsKey, favoritesSet).apply()
        }

        private fun openDetailsActivity(view: View, position: Int) {
            val menuItem = menuItems[position]
            val bundle = Bundle().apply {
                putString("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "")
                putString("MenuItemPrice", menuItem.foodPrice)
                putString("MenuItemDescription", menuItem.foodDescription)
                putString("MenuItemImage", menuItem.foodImage)
                putString("MenuQuantity", menuItem.productQuantity)
            }

            // Navigate to the details fragment using NavController
            view.findNavController().navigate(R.id.action_homeFragment_to_detailsFragment, bundle)
        }

    }
}