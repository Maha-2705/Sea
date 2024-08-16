package com.capztone.fishfy.ui.activities.adapters
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.DiscountItemsBinding
import com.capztone.fishfy.ui.activities.Utils.ToastHelper
import com.capztone.fishfy.ui.activities.models.DiscountItem
import java.util.*

class DiscountAdapter(
    private val discountItems: List<DiscountItem>,
    private val context: Context
) : RecyclerView.Adapter<DiscountAdapter.DiscountViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscountViewHolder {
        val binding = DiscountItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiscountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiscountViewHolder, position: Int) {
        holder.bind(position)

    }

    override fun getItemCount(): Int = discountItems.size

    inner class DiscountViewHolder(private val binding: DiscountItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (isDiscountRecyclerViewClickable()) {
                        openDiscountDetailsActivity(it,position)
                    } else {
                        // Show toast message if DiscountRecyclerView is not clickable
                        // You can customize the message as needed
                        ToastHelper.showCustomToast(context, "Discount items open only after 5 Pm")
                    }
                }
            }
        }

        // Check if the DiscountRecyclerView is clickable based on the current time
        private fun isDiscountRecyclerViewClickable(): Boolean {
            val currentTime = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            val hour = currentTime.get(Calendar.HOUR_OF_DAY)
            return hour !in 7 until 15 // Returns true if not between 7 AM and 5 PM
        }

        // Open discount details activity
        private fun   openDiscountDetailsActivity(view: View, position: Int) {
            val menuItem =  discountItems[position]
            val bundle = Bundle().apply {
                putString("MenuItemName", menuItem.foodNames?.getOrNull(0) ?: "")
                putString("MenuItemPrice", menuItem.foodPrices)
                putString("MenuItemDescription", menuItem.foodDescriptions)
                putString("MenuItemImage", menuItem.foodImages)
                putString("MenuQuantity", menuItem.productQuantity)
            }

            // Navigate to the details fragment using NavController
            view.findNavController().navigate(R.id.action_homeFragment_to_detailsFragment, bundle)
        }
        // Set data in RecyclerView items
        fun bind(position: Int) {
            val discountItem = discountItems[position]
            binding.apply {
                val foodName = discountItem.foodNames?.toString()?.replace("[", "")?.replace("]", "") ?: ""
                val slashIndex = foodName.indexOf("/")
                if (slashIndex != -1 && slashIndex < foodName.length - 1) {
                    discountfoodname.text = foodName.substring(0, slashIndex + 1).trim()
                    menuFoodName2.text = foodName.substring(slashIndex + 1).trim()
                } else {
                    discountfoodname.text = foodName
                    menuFoodName2.text = ""
                }
                discounttextview.text=discountItem.discounts
                Qty.text = "${discountItem.productQuantity}"
                discountprice.text = "₹${discountItem.foodPrices}"
                Glide.with(context).load(Uri.parse(discountItem.foodImages)).into(discountimage)
            }
        }
    }

}
