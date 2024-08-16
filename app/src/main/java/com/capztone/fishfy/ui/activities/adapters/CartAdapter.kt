package com.capztone.fishfy.ui.activities.adapters

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.capztone.fishfy.R
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.fishfy.databinding.CartItemBinding
import com.capztone.fishfy.ui.activities.MainActivity
import com.capztone.fishfy.ui.activities.Utils.ToastHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartAdapter(
    private val context: Context,
    private val cartItems: MutableList<String>,
    private val cartItemPrices: MutableList<String>,
    private var cartItemDescriptions: MutableList<String>,
    private val cartImages: MutableList<String>,
    private var cartItemQuantities: MutableList<Int>,
    private val progressBarListener: ProgressBarListener,
    private var cartItemIngredients: MutableList<String>,
    
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    // Instance Firebase
    private val auth = FirebaseAuth.getInstance()
    interface ProgressBarListener {
        fun showProgressBar()
        fun hideProgressBar()
    }
    // Inside CartAdapter class
    interface CartActionListener {
        fun onCartAction()
    }

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
        itemsQuantity.addAll(cartItemQuantities)
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
                        progressBarListener.showProgressBar()

                         // Show progress bar
                    }
                }

                minusImageButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        decreaseQuantity(position)
                        progressBarListener.showProgressBar()
                         // Show progress bar
                    }
                }

                deteleImageButton.setOnClickListener {
                    val itemPosition = adapterPosition
                    if (itemPosition != RecyclerView.NO_POSITION) {
                        deleteItem(itemPosition)


                            // Handle delete click

                    }
                }
            }
        }

        fun bind(position: Int) {


            binding.apply {
                // Split foodName by "/"
                val foodNameParts = cartItems[position].split("/")

                // Display the first part before slash in the first TextView
                cartFoodNameTextView.text = foodNameParts.getOrNull(0) ?: ""

                // Display the second part after slash in the second TextView
                cartFoodNameTextView1.text = foodNameParts.getOrNull(1) ?: ""

                carItemPriceTextView.text = cartItemPrices[position]
                val uriString = cartImages[position]
                val uri = Uri.parse(uriString)
                Glide.with(context).load(uri).into(cartImageView)

                binding.root.setOnClickListener {

                    // Handle item click
                }

                // Set the quantity from local data
                binding.actualAuantity.text = cartItemQuantities[position].toString()
            }
        }

        private fun getQuantityFromFirebase(position: Int, onComplete: (Int) -> Unit) {
            getUniqueKeyAtPosition(position) { uniqueKey ->
                cartItemsReference.child(uniqueKey).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val quantity = snapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                        // Update local data
                        cartItemQuantities[position] = quantity
                        // Update UI
                        notifyItemChanged(position)
                        onComplete(quantity)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
            }
        }

        private fun increaseQuantity(position: Int) {
            if (position < 0 || position >= cartItemQuantities.size) return

            // Fetch the current quantity from Firebase
            getQuantityFromFirebase(position) { currentQuantity ->
                // Increment the current quantity by 1
                val updatedQuantity = currentQuantity + 1

                // Update the quantity in the local list
                cartItemQuantities[position] = updatedQuantity
                // Update the quantity in Firebase
                updateQuantityInFirebase(position, updatedQuantity)
                updateHomeCartQuantity(cartItems[position], updatedQuantity)
                progressBarListener.hideProgressBar()

            }
        }

        private fun decreaseQuantity(position: Int) {
            if (position < 0 || position >= cartItemQuantities.size) return

            // Fetch the current quantity from Firebase
            getQuantityFromFirebase(position) { currentQuantity ->
                // Ensure quantity does not go below 1
                if (currentQuantity > 1) {
                    // Decrement the current quantity by 1
                    val updatedQuantity = currentQuantity - 1

                    // Update the quantity in the local list
                    cartItemQuantities[position] = updatedQuantity
                    // Update the quantity in Firebase
                    updateQuantityInFirebase(position, updatedQuantity)
                    updateHomeCartQuantity(cartItems[position], updatedQuantity)
                } else {
                    showDeleteConfirmationDialog(position)
                }
                progressBarListener.hideProgressBar()
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
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e(ContentValues.TAG, "Database error: ${databaseError.message}")
                    }
                })
        }

        private fun deleteItem(position: Int) {
            // Check if it's the last item in the cart
            val isLastItem = cartItems.size == 1

            if (isLastItem) {
                // Show confirmation dialog
                showDeleteConfirmationDialog(position)
            } else {
                // Directly delete the item
                performDelete(position)
            }
        }

        private fun showDeleteConfirmationDialog(position: Int) {
            // Inflate the custom layout for the dialog
            val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_cart_delete_dialog, null)

            // Initialize views from the custom dialog layout
            val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
            val dialogMessage = dialogView.findViewById<TextView>(R.id.dialog_message)
            val buttonYes = dialogView.findViewById<AppCompatButton>(R.id.dialog_button_yes)
            val buttonNo = dialogView.findViewById<AppCompatButton>(R.id.dialog_button_no)

            // Customize dialog title and message (if needed)
            dialogTitle.text = "Delete Confirmation"
            dialogMessage.text = "Are you sure you want to remove this item?"

            // Create AlertDialog object with no default AlertDialog components
            val alertDialog = AlertDialog.Builder(context).create()

            // Set the custom layout to the AlertDialog
            alertDialog.setView(dialogView)

            // Set background to transparent (optional)
            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Set click listeners for buttons
            buttonYes.setOnClickListener {
                performDelete(position)
                alertDialog.dismiss() // Dismiss dialog after performing delete action
            }

            buttonNo.setOnClickListener {
                alertDialog.dismiss() // Dismiss dialog if "No" is clicked
            }

            // Show the AlertDialog
            alertDialog.show()
        }

        private fun performDelete(position: Int) {
            getUniqueKeyAtPosition(position) { uniqueKey ->
                val foodNameToDelete = cartItems[position]

                // Remove item from Firebase
                cartItemsReference.child(uniqueKey).removeValue()
                    .addOnSuccessListener {
                        // Remove item from local lists
                        cartItems.removeAt(position)
                        cartItemPrices.removeAt(position)
                        cartItemDescriptions.removeAt(position)
                        cartImages.removeAt(position)
                        cartItemQuantities.removeAt(position)
                        cartItemIngredients.removeAt(position)

                        // Notify adapter about item removal
                        notifyItemRemoved(position)

                        // Remove item from Home cart if needed
                        updateHomeCartQuantity(foodNameToDelete, 0)
                    }
                    .addOnFailureListener {
                        // Handle failure
                        Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun getUniqueKeyAtPosition(position: Int, callback: (String) -> Unit) {
            val cartItem = cartItems[position]
            cartItemsReference.orderByChild("foodName").equalTo(cartItem).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val key = child.key
                        if (key != null) {
                            callback(key)
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
}
