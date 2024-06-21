package com.capztone.seafishfy.ui.activities.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.FreshFishBinding

import com.capztone.seafishfy.ui.activities.DetailsActivity

import com.capztone.seafishfy.ui.activities.models.MenuItem

class FreshFishAdapter(
    var menuItems: List<MenuItem>,
    private val context: Context
) : RecyclerView.Adapter<FreshFishAdapter.FishViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FishViewHolder {
        val binding = FreshFishBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FishViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FishViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = menuItems.size

    inner class FishViewHolder(private val binding: FreshFishBinding) :
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
            val menuItem = menuItems[position]
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "")
                putExtra("MenuItemPrice", menuItem.foodPrice)
                putExtra("MenuItemDescription", menuItem.foodDescription)
                putExtra("MenuItemImage", menuItem.foodImage)
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

                Qty.text=menuItem.productQuantity
                 menuPrice1.text = "₹${menuItem.foodPrice}"
                Glide.with(context).load(Uri.parse(menuItem.foodImage)).into(nearImage)
            }
        }
    }
}
