package com.capztone.seafishfy.ui.activities.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.seafishfy.databinding.HomeDiscountBinding
import com.capztone.seafishfy.ui.activities.DetailsActivity
import com.capztone.seafishfy.ui.activities.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeDiscountAdapter(
    private val context: Context
) : RecyclerView.Adapter<HomeDiscountAdapter.HomeDiscountViewHolder>() {

    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val locationsRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Locations").child(userId)
    private var shopNames: List<String> = emptyList()
    private val discountItems: MutableList<DiscountItem> = mutableListOf()

    init {
        locationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shopNameString = snapshot.child("shopname").value.toString()
                shopNames = shopNameString.split(",").map { it.trim() }
                fetchDiscountItems()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchDiscountItems() {
        for (shopName in shopNames) {
            val shopRef = FirebaseDatabase.getInstance().getReference(shopName)
            shopRef.child("discount").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val discountItem = snapshot.getValue(DiscountItem::class.java)
                    discountItem?.let {
                        discountItems.add(it)
                        notifyDataSetChanged()  // Notify adapter about data change
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle child changed if needed
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // Handle child removed if needed
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle child moved if needed
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    inner class HomeDiscountViewHolder(private val binding: HomeDiscountBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(discountItem: DiscountItem) {
            val foodName = discountItem.foodNames?.getOrNull(0) ?: ""
            binding.name.text = foodName.toString()
            binding.discount.text = "${discountItem.discounts} Off"
            Glide.with(binding.root)
                .load(discountItem.foodImages)
                .into(binding.freshfishImage)

            binding.root.setOnClickListener {
                openDetailsActivity(discountItem)
            }
        }

        private fun openDetailsActivity(discountItem: DiscountItem) {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("DiscountItemName", discountItem.foodNames?.getOrNull(0) ?: "")
                putExtra("DiscountItemPrice", discountItem.foodPrices)
                putExtra("DiscountItemDescription", discountItem.foodDescriptions)
                putExtra("DiscountItemImage", discountItem.foodImages)
                putExtra("DiscountQuantity", discountItem.quantity)
                putExtra("discounts", discountItem.discounts)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeDiscountViewHolder {
        val binding = HomeDiscountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeDiscountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeDiscountViewHolder, position: Int) {
        holder.bind(discountItems[position])
    }

    override fun getItemCount(): Int {
        return discountItems.size
    }
}