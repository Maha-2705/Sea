package com.capztone.seafishfy.ui.activities.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.databinding.MenuItemBinding
import com.capztone.seafishfy.ui.activities.DetailsActivity
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MenuAdapter(
    private val menuItems: List<MenuItem>,
    private val context: Context
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    private val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseDatabase.getInstance().getReference("Favourite").child(currentUserID ?: "")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuAdapter.MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuAdapter.MenuViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = menuItems.size

    inner class MenuViewHolder(private val binding: MenuItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsActivity(position)
                }
            }
        }

        private fun openDetailsActivity(position: Int) {
            val menuItem = menuItems[position]
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "") // Pass the combined name
                putExtra("MenuItemPrice", menuItem.foodPrice)
                putExtra("MenuItemDescription", menuItem.foodDescription)
                putExtra("MenuItemImage", menuItem.foodImage)
                putExtra("MenuQuantity", menuItem.productQuantity)
            }
            context.startActivity(intent)
        }

        fun bind(position: Int) {
            val menuItem = menuItems[position]
            binding.apply {
                val foodName = menuItem.foodName?.toString()?.replace("[", "")?.replace("]", "") ?: ""
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
            }
        }

        private fun toggleFavorite(position: Int) {
            val menuItem = menuItems[position]
            val newFavoriteState = !menuItem.favorite

            menuItem.favorite = newFavoriteState

            if (newFavoriteState) {
                updateFavoriteStateInFirebase(menuItem)
            } else {
                removeFavoriteFromFirebase(menuItem)
            }

            notifyItemChanged(position)
        }

        private fun updateFavoriteStateInFirebase(menuItem: MenuItem) {
            currentUserID?.let { userId ->
                val database = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)
                val favoriteRef = database.push()
                menuItem.firebaseKey = favoriteRef.key
                favoriteRef.setValue(menuItem).addOnSuccessListener {
                    Log.d("Firebase", "Item added to favorites: ${favoriteRef.key}")
                }.addOnFailureListener { e ->
                    Log.e("Firebase", "Failed to add item to favorites", e)
                }
            }
        }

        private fun removeFavoriteFromFirebase(menuItem: MenuItem) {
            if (!menuItem.favorite) {
                currentUserID?.let { userId ->
                    val database = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)
                    menuItem.firebaseKey?.let { key ->
                        database.child(key).removeValue().addOnSuccessListener {
                            Log.d("Firebase", "Item removed from favorites: ${menuItem.foodId}")
                        }.addOnFailureListener { e ->
                            Log.e("Firebase", "Failed to remove item from favorites", e)
                        }
                    }
                }
            } else {
                Log.d("Firebase", "Item is not removed because it is still a favorite: ${menuItem.foodId}")
            }
        }
    }
}
