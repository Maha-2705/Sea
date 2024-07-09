package com.capztone.seafishfy.ui.activities.adapters

import android.content.Context
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
import com.capztone.seafishfy.databinding.NearItemBinding
import com.capztone.seafishfy.ui.activities.Utils.ToastHelper
import com.capztone.seafishfy.ui.activities.models.CartItems
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.concurrent.CountDownLatch

class NearItemAdapter(
    private var menuItems: MutableList<MenuItem>,
    private var cartItems: MutableList<CartItems>,
    private val context: Context
) : RecyclerView.Adapter<NearItemAdapter.ViewHolder>() {
    private val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    private val sharedPreferences = context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
    private val seenFoodNames = mutableSetOf<String>()
    private val favoriteItemsKey = "favoriteItems"
    private var foodDescription: String? = null
    private val databaseReference = FirebaseDatabase.getInstance().reference
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
        val binding = NearItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menuItem = menuItems[position]
        val cartItem = cartItems.find { it.foodName == "${menuItem.foodName?.getOrNull(0)} ${menuItem.foodName?.getOrNull(1)}" }
        holder.bind(menuItem, cartItem)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun updateData(newItems: List<MenuItem>) {
        menuItems = newItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return menuItems.size
    }

    inner class ViewHolder(private val binding: NearItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val quantityLiveData = MutableLiveData<Int>()
        private var quantityListener: ValueEventListener? = null
        init {
            quantityLiveData.value = 0

            binding.quantityy.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: 0
                if (currentQuantity == 0) {
                    quantityLiveData.value = 1
                    binding.plusImageButton.visibility = View.VISIBLE
                    binding.minusImageButton.visibility = View.VISIBLE
                    addItemToCart()
                }
            }

            binding.plusImageButton.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: 0
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

            quantityLiveData.observe(context as LifecycleOwner, Observer { quantity ->
                updateQuantityText(quantity)
            })
        }

        private fun updateCartItemQuantity(newQuantity: Int) {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val foodName = binding.menuFoodName1.text.toString() + binding.menuFoodName2.text.toString()

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
        private fun saveQuantity(foodName: String, quantity: Int) {
            val editor = sharedPreferences.edit()
            editor.putInt("quantity_$foodName", quantity)
            editor.apply()
        }

        private fun getSavedQuantity(foodName: String): Int {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            var savedQuantity = 0

            if (userId != null) {
                val cartItemsRef = FirebaseDatabase.getInstance().reference.child("user").child(userId).child("cartItems")
                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (cartSnapshot in dataSnapshot.children) {
                                val cartFoodName = cartSnapshot.child("foodName").getValue(String::class.java)
                                if (cartFoodName == foodName) {
                                    savedQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 0
                                    quantityLiveData.value = savedQuantity
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("NearItemAdapter", "Failed to read quantity", databaseError.toException())
                        }
                    })
            }

            return savedQuantity
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

                            val positiveButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_positive_button)
                            val negativeButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_negative_button)

                            positiveButton.setOnClickListener {
                                cartItemsRef.removeValue().addOnSuccessListener {
                                    addItemToCartWithoutCheck1()
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
        private fun addItemToCartWithoutCheck1() {
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


        fun bind(menuItem: MenuItem, cartItem: CartItems?) {
            binding.menuFoodName1.text = menuItem.foodName?.getOrNull(0) ?: ""
            binding.menuFoodName2.text = menuItem.foodName?.getOrNull(1) ?: ""
            binding.menuPrice1.text = "₹${menuItem.foodPrice}"
            binding.nearImage.tag = menuItem.foodImage
            foodDescription = menuItem.foodDescription

            binding.shopname.text = menuItem.path ?: ""
            binding.Qty.text=menuItem.productQuantity
            binding.fav.setImageResource(if (menuItem.favorite) R.drawable.baseline_favorite_24 else R.drawable.favourite)


            Glide.with(binding.root.context)
                .load(menuItem.foodImage)
                .into(binding.nearImage)
            val foodName = menuItem.foodName?.getOrNull(0) ?: ""
            val savedQuantity = getSavedQuantity(foodName)
            quantityLiveData.value = savedQuantity
            updateQuantityText(savedQuantity)

            quantityLiveData.value = savedQuantity
            updateQuantityText(savedQuantity)


            // Listen for changes in foodQuantity in Firebase
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val foodName1 = binding.menuFoodName1.text.toString()
            val foodName2 = binding.menuFoodName2.text.toString()
            val combinedFoodName = "$foodName1 $foodName2"

            if (userId != null && combinedFoodName.isNotEmpty()) {
                val cartItemsRef = FirebaseDatabase.getInstance().reference.child("user").child(userId).child("cartItems")

                quantityListener?.let {
                    cartItemsRef.orderByChild("foodName").equalTo(combinedFoodName).removeEventListener(it)
                }

                quantityListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (cartSnapshot in dataSnapshot.children) {
                            val cartFoodName = cartSnapshot.child("foodName").getValue(String::class.java)
                            if (cartFoodName == combinedFoodName) {
                                val foodQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 0
                                quantityLiveData.value = foodQuantity
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("NearItemAdapter", "Failed to read quantity", databaseError.toException())
                    }
                }

                cartItemsRef.orderByChild("foodName").equalTo(combinedFoodName).addValueEventListener(quantityListener!!)
            }

            quantityLiveData.observeForever { quantity ->
                updateQuantityText(quantity)
            }
            binding.fav.setOnClickListener {
                toggleFavorite(adapterPosition)
            }


            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsActivity(it, position)
                }
            }
        }

        private fun updateQuantityText(quantity: Int) {
            binding.quantityy.text = if (quantity > 0) quantity.toString() else "Add"
            binding.plusImageButton.visibility = if (quantity > 0) View.VISIBLE else View.GONE
            binding.minusImageButton.visibility = if (quantity > 0) View.VISIBLE else View.GONE
        }

        fun unbind() {
            quantityListener?.let {
                FirebaseDatabase.getInstance().reference.removeEventListener(it)
            }
            quantityLiveData.removeObservers(itemView.context as LifecycleOwner)
        }
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
    private fun toggleFavorite(position: Int) {
        val menuItem = menuItems[position]
        menuItem.favorite = !menuItem.favorite
        menuItem.foodId?.let { saveFavoriteState(it, menuItem.favorite) }
        updateFavoriteStateInFirebase(menuItem)
        notifyItemChanged(position)
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
    private fun updateFavoriteStateInFirebase(menuItem: MenuItem) {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        currentUserID?.let { userId ->
            val databaseRef =
                FirebaseDatabase.getInstance().getReference("Favourite").child(userId)
            if (menuItem.firebaseKey != null) {
                databaseRef.child(menuItem.firebaseKey!!).child("favorite")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentFavoriteState =
                                snapshot.getValue(Boolean::class.java) ?: false
                            val newFavoriteState = !currentFavoriteState
                            databaseRef.child(menuItem.firebaseKey!!).child("favorite")
                                .setValue(newFavoriteState)
                                .addOnSuccessListener {
                                    Log.d(
                                        "Firebase",
                                        "Item favorite state toggled: ${menuItem.foodId}"
                                    )
                                }.addOnFailureListener { e ->
                                    Log.e("Firebase", "Failed to toggle item favorite state", e)
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Firebase", "Database error: ${error.message}")
                        }
                    })
            } else {
                val newFavoriteRef = databaseRef.push()
                newFavoriteRef.setValue(menuItem)
                    .addOnSuccessListener {
                        menuItem.firebaseKey = newFavoriteRef.key
                        Log.d("Firebase", "New item added to favorites: ${menuItem.foodId}")
                    }.addOnFailureListener { e ->
                        Log.e("Firebase", "Failed to add new item to favorites", e)
                    }
            }
        }
    }
}