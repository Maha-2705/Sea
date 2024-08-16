package com.capztone.fishfy.ui.activities.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.PreviousOrderBinding
import com.capztone.fishfy.ui.activities.models.PreviousItem


class PreviousOrderAdapter(
    private val buyHistory: MutableList<PreviousItem>,
    private val context: Context
) : RecyclerView.Adapter<PreviousOrderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PreviousOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = buyHistory[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return buyHistory.size
    }

    fun updateData(newData: List<PreviousItem>) {
        buyHistory.clear()
        buyHistory.addAll(filterUniqueItems(newData))
        notifyDataSetChanged()
    }

    private fun filterUniqueItems(data: List<PreviousItem>): List<PreviousItem> {
        val uniqueItems = mutableListOf<PreviousItem>()
        val seenNames = mutableSetOf<String>()

        for (item in data) {
            val foodName = item.foodName?.substringBefore("/")
            if (foodName != null && foodName !in seenNames) {
                seenNames.add(foodName)
                uniqueItems.add(item)
            }
        }

        return uniqueItems
    }

    inner class ViewHolder(private val binding: PreviousOrderBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsActivity(it,position)
                }
            }
        }

        fun bind(item: PreviousItem) {
            with(binding) {
                loadFoodImage(item.foodImage)

                // Extracting text before the slash
                val foodName = item.foodName?.substringBefore("/")
                previousFoodName1.text = foodName
                previousPrice1.text = "₹${item.foodPrice}"
            }
        }

        private fun openDetailsActivity(view: View, position: Int) {
            val menuItem = buyHistory[position]
            val bundle = Bundle().apply {
                putString("MenuItemName", menuItem.foodName)
                putString("MenuItemPrice", menuItem.foodPrice)
                putString("MenuItemDescription", menuItem.foodDescription)
                putString("MenuItemImage", menuItem.foodImage)

            }

            // Navigate to the details fragment using NavController
            view.findNavController().navigate(R.id.action_homeFragment_to_detailsFragment, bundle)
        }

        private fun loadFoodImage(imageUrl: String?) {
            Glide.with(binding.root)
                .load(imageUrl)
                .into(binding.previousImage)
        }
    }
}