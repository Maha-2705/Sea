package com.capztone.seafishfy.ui.activities.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.databinding.SearchItemBinding
import com.capztone.seafishfy.ui.activities.DetailsActivity
import com.capztone.seafishfy.ui.activities.models.MenuItem

class SearchAdapter(
    private var menuItems: List<MenuItem>,
    private val requireContext: Context,
) : RecyclerView.Adapter<SearchAdapter.MenuViewHolder>() {
    private var filteredMenuItems: List<MenuItem> = menuItems

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = SearchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(filteredMenuItems[position])
    }

    override fun getItemCount(): Int = filteredMenuItems.size

    inner class MenuViewHolder(private val binding: SearchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsActivity(position)
                }
            }
        }

        private fun openDetailsActivity(position: Int) {
            val menuItem = filteredMenuItems[position]

            // Intent to open details activity and pass data
            val intent = Intent(requireContext, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "")
                putExtra("MenuItemPrice", menuItem.foodPrice)
                putExtra("MenuItemDescription", menuItem.foodDescription)
                putExtra("MenuItemImage", menuItem.foodImage)
                putExtra("MenuQuantity", menuItem.productQuantity)
                putExtra("ShopName", menuItem.path) // Add ShopName to Intent
            }
            requireContext.startActivity(intent)  // Start the details Activity
        }

        fun bind(menuItem: MenuItem) {
            binding.apply {
                // Join food names with commas
                val foodName =
                    menuItem.foodName?.toString()?.replace("[", "")?.replace("]", "") ?: ""
                val slashIndex = foodName.indexOf("/")
                if (slashIndex != -1 && slashIndex < foodName.length - 1) {
                    menuFoodName.text = foodName.substring(0, slashIndex + 1).trim()
                     menuFoodName1.text = foodName.substring(slashIndex + 1).trim()
                } else {
                    menuFoodName.text = foodName
                    menuFoodName1.text = ""
                }
                val priceWithPrefix = "₹${menuItem.foodPrice}" // Prefixing the price with "₹"
                menuPrice.text = priceWithPrefix
                val url = Uri.parse(menuItem.foodImage)
                Glide.with(requireContext).load(url).into(menuImage)
            }
        }
    }
    fun filter(query: String) {
        filteredMenuItems = if (query.isEmpty()) {
            menuItems
        } else {
            menuItems.filter { menuItem ->
                // Check if the English name contains the query
                menuItem.foodName?.get(0)?.contains(query, ignoreCase = true) ?: false
            }
        }
        notifyDataSetChanged()
    }

}