package com.capztone.fishfy.ui.activities.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.capztone.fishfy.R
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.fishfy.databinding.CategoryItemBinding
import com.capztone.fishfy.ui.activities.models.Category

class CategoryAdapter(
    private val context: Context,
    private val categories: List<Category>,
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(private val binding: CategoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.fishLabel.text = category.name
            Glide.with(context).load(category.imageUrl).into(binding.fishIcon)

            binding.root.setOnClickListener {
                Log.d("CategoryAdapter", "Item clicked: ${category.name}")
                onItemClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = CategoryItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int {
        return categories.size
    }
}
