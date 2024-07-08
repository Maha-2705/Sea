package com.capztone.seafishfy.ui.activities.adapters

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.SearchItemBinding
import com.capztone.seafishfy.ui.activities.fragments.DetailsFragment
import com.capztone.seafishfy.ui.activities.models.MenuItem

class SearchAdapter(
    private var menuItems: List<MenuItem>,
    private val context: Context
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
                    openDetailsFragment(it,position)
                }
            }
        }

        private fun openDetailsFragment(view: View, position: Int) {
            val menuItem = filteredMenuItems[position]
            val bundle = Bundle().apply {
                putString("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "")
                putString("MenuItemPrice", menuItem.foodPrice)
                putString("MenuItemDescription", menuItem.foodDescription)
                putString("MenuItemImage", menuItem.foodImage)
                putString("MenuQuantity", menuItem.productQuantity)
            }

            // Navigate to the details fragment using NavController
            view.findNavController().navigate(R.id.action_searchFragment_to_detailsFragment, bundle)
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
                Glide.with(context).load(url).into(menuImage)
            }
        }
    }

    fun filter(query: String) {
        filteredMenuItems = if (query.isEmpty()) {
            menuItems
        } else {
            menuItems.filter { menuItem ->
                // Check if the English name contains the query
                menuItem.foodName?.getOrNull(0)?.contains(query, ignoreCase = true) ?: false
            }
        }
        notifyDataSetChanged()
    }
}
