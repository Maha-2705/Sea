package com.capztone.seafishfy.ui.activities.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.databinding.PreviousOrderBinding
import com.capztone.seafishfy.ui.activities.DetailsActivity
import com.capztone.seafishfy.ui.activities.models.PreviousItem

class PreviousOrderAdapter(
    private val buyHistory: MutableList<PreviousItem>
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
                    openDetailsActivity(position)
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

        private fun openDetailsActivity(position: Int) {
            val menuItem = buyHistory[position]

            // Intent to open details activity and pass data
            val intent = Intent(binding.root.context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.foodName ?: "")
                putExtra("MenuItemPrice", menuItem.foodPrice)
                putExtra("MenuItemDescription", menuItem.foodDescription ?: "")
                putExtra("MenuItemImage", menuItem.foodImage ?: "")
                putExtra("ShopName", menuItem.path ?: "") // Add ShopName to Intent
            }
            binding.root.context.startActivity(intent) // Start the details Activity
        }

        private fun loadFoodImage(imageUrl: String?) {
            Glide.with(binding.root)
                .load(imageUrl)
                .into(binding.previousImage)
        }
    }
}