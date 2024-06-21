package com.capztone.seafishfy.ui.activities.adapters
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.databinding.DiscountItemsBinding
import com.capztone.seafishfy.ui.activities.DetailsActivity
import com.capztone.seafishfy.ui.activities.Utils.ToastHelper
import com.capztone.seafishfy.ui.activities.models.DiscountItem
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
                        openDiscountDetailsActivity(position)
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
        private fun openDiscountDetailsActivity(position: Int) {
            val discountItem = discountItems[position]

            // Intent to open details activity and Pass data
            val intent = Intent(context, DetailsActivity::class.java)
            intent.putExtra("DiscountItemName", discountItem.foodNames?.getOrNull(0) ?: "")
            intent.putExtra("DiscountItemPrice", discountItem.foodPrices)
            intent.putExtra("DiscountItemDescription", discountItem.foodDescriptions)
            intent.putExtra("DiscountItemImage", discountItem.foodImages)
            intent.putExtra("DiscountAmount", discountItem.discounts)
            intent.putExtra("DiscountQuantity", discountItem.productQuantity)


            context.startActivity(intent)  // Start the details Activity
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
