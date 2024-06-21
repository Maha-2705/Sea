package com.capztone.seafishfy.ui.activities.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.FavouritesItemBinding
import com.capztone.seafishfy.ui.activities.models.MenuItem

class FavouriteAdapter(
    private val context: Context,
    private val menuItems: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<FavouriteAdapter.FavouriteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val binding = FavouritesItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavouriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size

    inner class FavouriteViewHolder(private val binding: FavouritesItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menuItem: MenuItem) {
            binding.apply {
                val foodName = menuItem.foodName?.toString()?.replace("[", "")?.replace("]", "") ?: ""
                val parts = foodName.split("/")
                if (parts.size == 2) {
                    favFoodNameTextView.text = parts[0].trim()
                     favFoodNameTextView1.text = parts[1].trim()
                } else {
                     favFoodNameTextView.text = foodName
                     favFoodNameTextView1.text = ""
                }
                val priceWithPrefix = "₹${menuItem.foodPrice}"
                favPriceTextView.text = priceWithPrefix
                Glide.with(context)
                    .load(Uri.parse(menuItem.foodImage))
                    .into(favImageView)

                // Set the favorite icon based on the stored favorite state

                // Handle item click
                root.setOnClickListener {
                    onItemClick(menuItem)
                }
            }
        }
    }
}
