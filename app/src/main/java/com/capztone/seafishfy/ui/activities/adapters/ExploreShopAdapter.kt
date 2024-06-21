package com.capztone.seafishfy.ui.activities.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.ExploreShopBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ExploreShopAdapter(
    private var shopList: List<String>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<ExploreShopAdapter.ShopViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(shopName: String)
    }

    inner class ShopViewHolder(val binding: ExploreShopBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(shopName: String, locality: String) {
            binding.shopTextView.text = shopName
            binding.shopTextView1.text = locality

            binding.root.setOnClickListener {
                itemClickListener.onItemClick(shopName)
            }

            // Set different images for different positions
            when (absoluteAdapterPosition % 3) {
                0 -> binding.shopImageView.setImageResource(R.drawable.freshfish)
                1 -> binding.shopImageView.setImageResource(R.drawable.fishman)
                2 -> binding.shopImageView.setImageResource(R.drawable.pickles)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ExploreShopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shopName = shopList[position]

        // Fetch locality from Firebase Realtime Database
        val databaseReference = FirebaseDatabase.getInstance().reference
        databaseReference.child("ShopLocations").child(shopName).child("locality")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val locality = dataSnapshot.getValue(String::class.java) ?: ""
                    holder.bind(shopName, locality)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors
                }
            })

        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(shopName)
        }
    }

    override fun getItemCount(): Int = shopList.size

    fun setShopList(newShopList: List<String>) {
        shopList = newShopList
        notifyDataSetChanged()
    }
}
