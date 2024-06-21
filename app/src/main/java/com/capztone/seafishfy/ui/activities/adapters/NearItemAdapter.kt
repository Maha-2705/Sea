package com.capztone.seafishfy.ui.activities.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.NearItemBinding
import com.capztone.seafishfy.ui.activities.DetailsActivity
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.capztone.seafishfy.ui.activities.models.ProductItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NearItemAdapter(
    private var menuItems: MutableList<MenuItem>,
    private val context: Context,


    ) : RecyclerView.Adapter<NearItemAdapter.ViewHolder>() {
    private val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
    private var originalMenuItems: MutableList<MenuItem> = mutableListOf()
    private val seenFoodNames = mutableSetOf<String>()
    private val favoriteItemsKey = "favoriteItems"
    init {
        // Save original menu items for reference
        originalMenuItems.addAll(menuItems)
        loadFavoriteItems()
    }
    fun updateMenuItems(newMenuItems: List<MenuItem>) {
        // Clear previous items and seen names
        menuItems.clear()
        seenFoodNames.clear()

        // Add only unique items based on foodName
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
        holder.bind(menuItem)
    }

    fun updateData(newItems: List<MenuItem>) {
        menuItems = newItems.toMutableList()
        loadFavoriteItems()
        notifyDataSetChanged()

    }
    override fun getItemCount(): Int {
        return menuItems.size
    }


    inner class ViewHolder(private val binding: NearItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private var currentQty = 0


        init {
            binding.plusImageButton.setOnClickListener {
                currentQty++
                updateQuantityText(adapterPosition)
                updateFirebaseCartItem()
                setupFirebaseListener()

                saveQuantityToPreferences(adapterPosition, currentQty)
                Handler(Looper.getMainLooper()).postDelayed({
                    openDetailsActivity(position)
                }, 1500)
            }


            binding.minusImageButton.setOnClickListener {
                if (currentQty > 0) {
                    currentQty--
                    updateQuantityText(adapterPosition)
                    updateFirebaseCartItem1()

                    saveQuantityToPreferences(adapterPosition, currentQty)
                }
            }

        }
        private fun setupFirebaseListener() {
            val menuItem = menuItems[adapterPosition]
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->
                val databaseRef = FirebaseDatabase.getInstance().getReference("Home").child(uid)
                    .child("cartItems")

                databaseRef.orderByChild("foodName").equalTo(menuItem.foodName?.getOrNull(0) ?: "")
                    .addChildEventListener(object : ChildEventListener {
                        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                            // Handle quantity change
                            val updatedItem = snapshot.getValue(ProductItem::class.java)
                            updatedItem?.let {
                                if (it.foodName == menuItem.foodName?.getOrNull(0)) {
                                    currentQty = it.foodQuantity ?: 0
                                    updateQuantityText(adapterPosition)
                                }
                            }
                        }

                        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}

                        override fun onChildRemoved(snapshot: DataSnapshot) {}

                        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Firebase", "Database error: ${error.message}")
                        }
                    })
            }
        }
        private fun updateQuantityText(position: Int) {
            if (currentQty > 0) {
                binding.quantityy.text = "Add($currentQty)"
            } else {
                binding.quantityy.text = "Add"
            }
        }


        private fun updateFirebaseCartItem() {
            val menuItem = menuItems[adapterPosition]
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->
                val databaseRef = FirebaseDatabase.getInstance().getReference("Home").child(uid)
                    .child("cartItems")

                // Query to check if the item already exists
                databaseRef.orderByChild("foodName").equalTo(menuItem.foodName?.getOrNull(0) ?: "")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                // Item exists, update its quantity
                                snapshot.children.forEach { itemSnapshot ->
                                    val existingItemKey = itemSnapshot.key
                                    val existingItem =
                                        itemSnapshot.getValue(ProductItem::class.java)
                                    existingItem?.let {
                                        val currentQuantity = it.foodQuantity ?: 0
                                        val updatedQuantity = currentQuantity + 1 // Increment by 1

                                        if (updatedQuantity > 0) {
                                            // Update the quantity
                                            databaseRef.child(existingItemKey!!)
                                                .child("foodQuantity").setValue(updatedQuantity)
                                                .addOnSuccessListener {
                                                    Log.d(
                                                        "Firebase",
                                                        "Existing item quantity updated successfully"
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e(
                                                        "Firebase",
                                                        "Failed to update existing item quantity",
                                                        e
                                                    )
                                                }
                                        } else {
                                            // Delete item if quantity becomes 0
                                            databaseRef.child(existingItemKey!!).removeValue()
                                                .addOnSuccessListener {
                                                    Log.d("Firebase", "Item deleted successfully")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Firebase", "Failed to delete item", e)
                                                }
                                        }
                                    }
                                }
                            } else {
                                // Item does not exist, create a new entry with quantity 1
                                val cartItemRef = databaseRef.push()

                                val itemDetails = mutableMapOf<String, Any>()
                                itemDetails["foodName"] = menuItem.foodName?.getOrNull(0) ?: ""
                                itemDetails["foodPrice"] = menuItem.foodPrice!!.toInt()
                                itemDetails["foodImage"] = menuItem.foodImage ?: ""
                                itemDetails["foodQuantity"] = 1 // Start with quantity 1
                                itemDetails["foodDescription"] = menuItem.foodDescription ?: ""
                                itemDetails["shopName"] = menuItem.path ?: "" // Add shopName

                                // Set the value in the Firebase database
                                cartItemRef.setValue(itemDetails)
                                    .addOnSuccessListener {
                                        Log.d(
                                            "Firebase",
                                            "New cart item added successfully: ${cartItemRef.key}"
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firebase", "Failed to add new cart item", e)
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Firebase", "Database error: ${error.message}")
                        }
                    })
            }
        }

        private fun updateFirebaseCartItem1() {
            val menuItem = menuItems[adapterPosition]
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->
                val databaseRef = FirebaseDatabase.getInstance().getReference("Home").child(uid)
                    .child("cartItems")

                // Query to check if the item already exists
                databaseRef.orderByChild("foodName").equalTo(menuItem.foodName?.getOrNull(0) ?: "")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                // Item exists, update its quantity
                                snapshot.children.forEach { itemSnapshot ->
                                    val existingItemKey = itemSnapshot.key
                                    val existingItem =
                                        itemSnapshot.getValue(ProductItem::class.java)
                                    existingItem?.let {
                                        val currentQuantity = it.foodQuantity ?: 0
                                        val updatedQuantity = currentQuantity - 1 // Decrement by 1

                                        if (updatedQuantity > 0) {
                                            // Update the quantity
                                            databaseRef.child(existingItemKey!!)
                                                .child("foodQuantity").setValue(updatedQuantity)
                                                .addOnSuccessListener {
                                                    Log.d(
                                                        "Firebase",
                                                        "Existing item quantity updated successfully"
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e(
                                                        "Firebase",
                                                        "Failed to update existing item quantity",
                                                        e
                                                    )
                                                }
                                        } else {
                                            // Delete item if quantity becomes 0
                                            databaseRef.child(existingItemKey!!).removeValue()
                                                .addOnSuccessListener {
                                                    Log.d("Firebase", "Item deleted successfully")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Firebase", "Failed to delete item", e)
                                                }
                                        }
                                    }
                                }
                            } else {
                                // Item does not exist, create a new entry with quantity 1
                                val cartItemRef = databaseRef.push()

                                val itemDetails = mutableMapOf<String, Any>()
                                itemDetails["foodName"] = menuItem.foodName?.getOrNull(0) ?: ""
                                itemDetails["foodPrice"] = menuItem.foodPrice!!.toInt()
                                itemDetails["foodImage"] = menuItem.foodImage ?: ""
                                itemDetails["foodQuantity"] = 1 // Start with quantity 1
                                itemDetails["foodDescription"] = menuItem.foodDescription ?: ""
                                itemDetails["shopName"] = menuItem.path ?: "" // Add shopName

                                // Set the value in the Firebase database
                                cartItemRef.setValue(itemDetails)
                                    .addOnSuccessListener {
                                        Log.d(
                                            "Firebase",
                                            "New cart item added successfully: ${cartItemRef.key}"
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firebase", "Failed to add new cart item", e)
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Firebase", "Database error: ${error.message}")
                        }
                    })
            }
        }



        private fun saveQuantityToPreferences(position: Int, quantity: Int) {
            val editor = sharedPreferences.edit()
            editor.putInt("item_$position", quantity)
            editor.apply()
        }

        private fun getQuantityFromPreferences(position: Int): Int {
            return sharedPreferences.getInt("item_$position", 0)
        }

        fun bind(menuItem: MenuItem) {
            currentQty = getQuantityFromPreferences(adapterPosition)
            updateQuantityText(adapterPosition)


            binding.apply {
                val foodName =
                    menuItem.foodName?.toString()?.replace("[", "")?.replace("]", "") ?: ""
                val slashIndex = foodName.indexOf("/")
                if (slashIndex != -1 && slashIndex < foodName.length - 1) {
                    menuFoodName1.text = foodName.substring(0, slashIndex + 1).trim()
                    menuFoodName2.text = foodName.substring(slashIndex + 1).trim()
                } else {
                    menuFoodName1.text = foodName
                    menuFoodName2.text = ""
                }

                Qty.text = "${menuItem.productQuantity}"
                menuPrice1.text = "₹${menuItem.foodPrice}"
                Glide.with(context).load(Uri.parse(menuItem.foodImage)).into(nearImage)
                fav.setImageResource(if (menuItem.favorite) R.drawable.baseline_favorite_24 else R.drawable.favourite)
            }

            // Check Firebase to see if the item is in the cart
            checkFirebaseForItem(menuItem) { quantity ->
                currentQty = quantity
                updateQuantityText(position)
            }


            binding.root.setOnClickListener {
                openDetailsActivity(adapterPosition)
            }

            binding.fav.setOnClickListener {
                toggleFavorite(adapterPosition)
            }
        }

    }
    private fun checkFirebaseForItem(menuItem: MenuItem, callback: (Int) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { uid ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("Home").child(uid)
                .child("cartItems")

            databaseRef.orderByChild("foodName").equalTo(menuItem.foodName?.getOrNull(0) ?: "")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Item exists, get the quantity
                            val item = snapshot.children.first().getValue(ProductItem::class.java)
                            val quantity = item?.foodQuantity ?: 0
                            callback(quantity)
                        } else {
                            // Item does not exist, set quantity to 0
                            callback(0)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Database error: ${error.message}")
                        callback(0) // Assume item is not in the cart if there's an error
                    }
                })
        }
    }
    private fun openDetailsActivity(position: Int) {
        val menuItem = menuItems[position]
        val intent = Intent(context, DetailsActivity::class.java).apply {
            putExtra("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "")
            putExtra("MenuItemPrice", menuItem.foodPrice)
            putExtra("MenuItemDescription", menuItem.foodDescription)
            putExtra("MenuItemImage", menuItem.foodImage)
            putExtra("MenuQuantity", menuItem.productQuantity)
            putExtra("ShopName", menuItem.path) // Add ShopName to Intent
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
        (context as Activity).startActivityForResult(intent, REQUEST_CODE_DETAILS)
    }

    private fun toggleFavorite(position: Int) {
        val menuItem = menuItems[position]
        menuItem.favorite = !menuItem.favorite

        // Update favorite state in SharedPreferences
        menuItem.foodId?.let { saveFavoriteState(it, menuItem.favorite) }

        // Update favorite state in Firebase
        updateFavoriteStateInFirebase(menuItem)

        // Notify adapter of the change
        notifyItemChanged(position)
    }

    private fun saveFavoriteState(itemId: String, isFavorite: Boolean) {
        val favoritesSet = sharedPreferences.getStringSet(favoriteItemsKey, mutableSetOf()) ?: mutableSetOf()

        if (isFavorite) {
            favoritesSet.add(itemId)
        } else {
            favoritesSet.remove(itemId)
        }
        sharedPreferences.edit().putStringSet(favoriteItemsKey, favoritesSet).apply()
    }

    fun loadFavoriteItems() {
        val favoritesSet = sharedPreferences.getStringSet(favoriteItemsKey, mutableSetOf()) ?: mutableSetOf()

        menuItems.forEach { item ->
            item.favorite = favoritesSet.contains(item.foodId)
        }

        notifyDataSetChanged()
    }

    private fun updateFavoriteStateInFirebase(menuItem: MenuItem) {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        currentUserID?.let { userId ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)

            if (menuItem.firebaseKey != null) {
                // Toggle existing favorite item's 'favorite' field
                databaseRef.child(menuItem.firebaseKey!!).child("favorite").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentFavoriteState = snapshot.getValue(Boolean::class.java) ?: false
                        val newFavoriteState = !currentFavoriteState

                        // Update Firebase with new favorite state
                        databaseRef.child(menuItem.firebaseKey!!).child("favorite").setValue(newFavoriteState)
                            .addOnSuccessListener {
                                Log.d("Firebase", "Item favorite state toggled: ${menuItem.foodId}")
                            }.addOnFailureListener { e ->
                                Log.e("Firebase", "Failed to toggle item favorite state", e)
                            }

                      }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Query cancelled", error.toException())
                    }
                })
            } else {
                // Fetch all items under the user's "Favourite" node
                databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var itemUpdated = false
                        for (childSnapshot in snapshot.children) {
                            val existingItem = childSnapshot.getValue(MenuItem::class.java)
                            existingItem?.let {
                                // Check if existing item matches any foodName in menuItem
                                for (foodName in menuItem.foodName!!) {
                                    if (existingItem.foodName!!.contains(foodName)) {
                                        existingItem.favorite = menuItem.favorite
                                        databaseRef.child(childSnapshot.key!!).child("favorite").setValue(menuItem.favorite)
                                            .addOnSuccessListener {
                                                Log.d("Firebase", "Existing item favorite state updated: ${existingItem.foodId}")
                                                itemUpdated = true
                                            }.addOnFailureListener { e ->
                                                Log.e("Firebase", "Failed to update existing item favorite state", e)
                                            }
                                        return  // Exit after updating
                                    }
                                }
                            }
                        }

                        // If no item was updated, it means no existing item matched, add a new item
                        if (!itemUpdated) {
                            val favoriteRef = databaseRef.push()
                            menuItem.firebaseKey = favoriteRef.key // Store the key in the MenuItem
                            menuItem.favorite = true // Set default favorite state if adding new item
                            favoriteRef.setValue(menuItem).addOnSuccessListener {
                                Log.d("Firebase", "New item added to favorites: ${favoriteRef.key}")

                            }.addOnFailureListener { e ->
                                Log.e("Firebase", "Failed to add new item to favorites", e)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Query cancelled", error.toException())
                    }
                })
            }
        }

    }




    companion object {
        const val REQUEST_CODE_DETAILS = 1
    }
}