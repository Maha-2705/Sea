package com.capztone.seafishfy.ui.activities.adapters

import android.content.Context
import android.content.Intent
import android.database.DataSetObserver
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.DealsItemBinding
import com.capztone.seafishfy.databinding.NearItemBinding
import com.capztone.seafishfy.ui.activities.DetailsActivity
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DealItemAdapter(
    private var menuItems: List<MenuItem>,
    private val context: Context
) : RecyclerView.Adapter<DealItemAdapter.MenuViewHolder>(), ListAdapter {

    private val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseDatabase.getInstance().getReference("Favourite").child(currentUserID ?: "")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = DealsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }
    fun updateMenuItems(newMenuItems: List<MenuItem>) {
        menuItems = newMenuItems
        notifyDataSetChanged()
    }
    fun updateData(newItems: List<MenuItem>) {
        menuItems = newItems.toMutableList()
        notifyDataSetChanged()

    }
    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = menuItems.size

    inner class MenuViewHolder(private val binding: DealsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsActivity(position)
                }
            }

        }
        private fun loadFavoritesFromFirebase() {
            currentUserID?.let { userId ->
                database.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (favoriteSnapshot in snapshot.children) {
                            val favoriteItem = favoriteSnapshot.getValue(MenuItem::class.java)
                            favoriteItem?.let { item ->
                                menuItems.find { it.foodId == item.foodId }?.apply {
                                    favorite = true
                                    firebaseKey = item.firebaseKey
                                }
                            }
                        }
                        notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Failed to load favorites", error.toException())
                    }
                })
            }
        }

        private fun openDetailsActivity(position: Int) {
            val menuItem = menuItems[position]
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "") // Pass the first language-specific foodName
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
                val parts = foodName.split("/")
                if (parts.size == 2) {
                    menuFoodName1.text = parts[0].trim()
                    menuFoodName2.text = parts[1].trim()
                } else {
                    menuFoodName1.text = foodName
                    menuFoodName2.text = ""
                }

                val priceWithPrefix = "₹${menuItem.foodPrice}"
                menuPrice1.text = priceWithPrefix
                Qty.text = menuItem.productQuantity.toString()

                Glide.with(context)
                    .load(Uri.parse(menuItem.foodImage))
                    .into(nearImage)


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
                menuItem.firebaseKey = favoriteRef.key // Store the key in the MenuItem
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


    override fun registerDataSetObserver(observer: DataSetObserver?) {
        TODO("Not yet implemented")
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        TODO("Not yet implemented")
    }

    override fun getCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getItem(position: Int): Any {
        TODO("Not yet implemented")
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        TODO("Not yet implemented")
    }

    override fun getViewTypeCount(): Int {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun areAllItemsEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEnabled(position: Int): Boolean {
        TODO("Not yet implemented")
    }


}