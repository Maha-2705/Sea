package com.capztone.seafishfy.ui.activities.adapters

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.databinding.CartItemBinding
import com.capztone.seafishfy.ui.activities.Utils.ToastHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartAdapter(
    private val context: Context,
    private val cartItems: MutableList<String>,
    private val cartItemPrices: MutableList<String>,
    private var cartItemDescriptions: MutableList<String>,
    private val cartImages: MutableList<String>,
    private var cartItemQuantitys: MutableList<Int>,
    private var cartItemIngredients: MutableList<String>
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    // instance Firebase
    private val auth = FirebaseAuth.getInstance()

    init {
        // Initialize Firebase
        val database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        val cartItemNumber = cartItems.size

        itemQuantities = IntArray(cartItemNumber)
        cartItemsReference = database.reference.child("user").child(userId).child("cartItems")

    }

    companion object {
        private var itemQuantities: IntArray = intArrayOf()
        private lateinit var cartItemsReference: DatabaseReference
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = cartItems.size

    // Get updated quantity
    fun getUpdatedItemsQuantities(): MutableList<Int> {
        val itemsQuantity = mutableListOf<Int>()
        itemsQuantity.addAll(cartItemQuantitys)
        return itemsQuantity
    }

    inner class CartViewHolder(private val binding: CartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                plusImageButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        increaseQuantity(position)
                    }
                }

                minusImageButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        decreaseQuantity(position)
                    }
                }

                deteleImageButton.setOnClickListener {
                    val itemPosition = adapterPosition
                    if (itemPosition != RecyclerView.NO_POSITION) {
                        deleteItem(itemPosition)


                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                cartFoodNameTextView.text = cartItems[position]
                carItemPriceTextView.text = cartItemPrices[position]
                val uriString = cartImages[position]
                val uri = Uri.parse(uriString)
                Glide.with(context).load(uri).into(cartImageView)

                // Retrieve quantity from Firebase asynchronously
                getQuantityFromFirebase(position) { quantity ->
                    binding.actualAuantity.text = quantity.toString()
                }
            }
        }

        private fun getQuantityFromFirebase(position: Int, onComplete: (Int) -> Unit) {
            getUniqueKeyAtPosition(position) { uniqueKey ->
                cartItemsReference.child(uniqueKey).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val quantity = snapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                        onComplete(quantity)
                        // Call notifyItemChanged(position) after updating the quantity
                        notifyItemChanged(position)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
            }
        }

        private fun increaseQuantity(position: Int) {
            if (position < 0 || position >= cartItemQuantitys.size) return

            // Fetch the current quantity from Firebase
            getQuantityFromFirebase(position) { currentQuantity ->
                // Increment the current quantity by 1
                val updatedQuantity = currentQuantity + 1

                // Update the quantity in the local list and UI
                cartItemQuantitys[position] = updatedQuantity
                binding.quantityTextView.text = updatedQuantity.toString()

                // Update the quantity in Firebase
                updateQuantityInFirebase(position, updatedQuantity)
                updateHomeCartQuantity(cartItems[position], updatedQuantity)
                // Notify the adapter of the change
                notifyItemChanged(position)
            }
        }

        private fun decreaseQuantity(position: Int) {
            if (position < 0 || position >= cartItemQuantitys.size) return

            // Fetch the current quantity from Firebase
            getQuantityFromFirebase(position) { currentQuantity ->
                // Ensure quantity does not go below 1
                if (currentQuantity > 1) {
                    // Decrement the current quantity by 1
                    val updatedQuantity = currentQuantity - 1

                    // Update the quantity in the local list and UI
                    cartItemQuantitys[position] = updatedQuantity
                    binding.quantityTextView.text = updatedQuantity.toString()

                    // Update the quantity in Firebase
                    updateQuantityInFirebase(position, updatedQuantity)
                    updateHomeCartQuantity(cartItems[position], updatedQuantity)

                    // Notify the adapter of the change
                    notifyItemChanged(position)
                } else {
                    ToastHelper.showCustomToast(context, "Quantity cannot be less than 1")
                }
            }
        }

        private fun updateQuantityInFirebase(position: Int, quantity: Int) {
            val currentUser = FirebaseAuth.getInstance().currentUser

            currentUser?.let { user ->
                getUniqueKeyAtPosition(position) { uniqueKey ->
                    // Get the reference to the specific item push key under "cartItems"
                    val itemReference = cartItemsReference.child(uniqueKey)
                    // Update the quantity for that specific item
                    itemReference.child("foodQuantity").setValue(quantity)
                }
            }
        }

        private fun updateHomeCartQuantity(foodName: String, quantity: Int) {
            val userId = auth.currentUser?.uid ?: return
            val homeCartRef =
                FirebaseDatabase.getInstance().reference.child("Home").child(userId).child("cartItems")

            homeCartRef.orderByChild("foodName").equalTo(foodName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (cartSnapshot in dataSnapshot.children) {
                            cartSnapshot.ref.child("foodQuantity").setValue(quantity)
                                .addOnSuccessListener {
                                    // Handle success (optional)
                                }.addOnFailureListener { e ->
                                    Log.e(ContentValues.TAG, "Failed to update foodQuantity: ${e.message}")
                                    // Handle failure (optional)
                                }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e(ContentValues.TAG, "Database error: ${databaseError.message}")
                        // Handle onCancelled (optional)
                    }
                })
        }

        private fun deleteItem(position: Int) {
            getUniqueKeyAtPosition(position) { uniqueKey ->
                val foodNameToDelete = cartItems[position]

                // Delete from user cartItems path
                cartItemsReference.child(uniqueKey).removeValue().addOnSuccessListener {
                    // Remove item from the local lists
                    cartItems.removeAt(position)
                    cartItemPrices.removeAt(position)
                    cartItemDescriptions.removeAt(position)
                    cartImages.removeAt(position)
                    cartItemQuantitys.removeAt(position)
                    cartItemIngredients.removeAt(position)

                    // Notify the adapter
                    notifyItemRemoved(position)
                    ToastHelper.showCustomToast(context, "Item Deleted")

                    // Also remove from Home --> cartItems path
                    deleteFromHomeCartItems(foodNameToDelete)
                }.addOnFailureListener {
                    ToastHelper.showCustomToast(context, "Failed to Delete Item")
                }
            }
        }

        private fun deleteFromHomeCartItems(foodName: String) {
            val userId = auth.currentUser?.uid ?: return
            val homeCartRef = FirebaseDatabase.getInstance().reference.child("Home").child(userId).child("cartItems")

            homeCartRef.orderByChild("foodName").equalTo(foodName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (cartSnapshot in dataSnapshot.children) {
                            cartSnapshot.ref.removeValue().addOnSuccessListener {
                                Log.d(ContentValues.TAG, "Deleted item from Home cartItems")
                            }.addOnFailureListener { e ->
                                Log.e(ContentValues.TAG, "Failed to delete item from Home cartItems: ${e.message}")
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e(ContentValues.TAG, "Database error: ${databaseError.message}")
                    }
                })
        }



        private fun getUniqueKeyAtPosition(positionRetrieve: Int, onComplete: (String) -> Unit) {
            cartItemsReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var uniqueKey: String? = null
                    // loop for snapshot children
                    snapshot.children.forEachIndexed { index, dataSnapshot ->
                        if (index == positionRetrieve) {
                            uniqueKey = dataSnapshot.key
                            return@forEachIndexed
                        }
                    }
                    uniqueKey?.let { onComplete(it) }
                }

                override fun onCancelled(error: DatabaseError) {
                    ToastHelper.showCustomToast(context, "Failed to fetch data")
                }
            })
        }
    }

    private fun fetchItemPath(
        itemName: String,
        paths: List<String>,
        onComplete: (String?) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()

        // Iterate through each shop path
        for (shopPath in paths) {
            val shopReference = database.reference.child(shopPath)
            val childPaths = listOf("menu", "menu1", "menu2", "discount")
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->

                // Iterate through each child path within the shop
                for (childPath in childPaths) {
                    val childReference = shopReference.child(childPath)
                    childReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            // Check if the item exists in this child path under the shop
                            snapshot.children.forEach { shopSnapshot ->
                                if (shopSnapshot.child("foodName").value == itemName) {
                                    onComplete("$shopPath")
                                    return
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                        }
                    })
                }

            }
            // If item not found in any path, onComplete will be invoked with null
            onComplete(null)
        }
    }
}