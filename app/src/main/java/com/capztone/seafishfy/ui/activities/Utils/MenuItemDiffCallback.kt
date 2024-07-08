package com.capztone.seafishfy.ui.activities.Utils

import androidx.recyclerview.widget.DiffUtil
import com.capztone.seafishfy.ui.activities.models.MenuItem

class ProductDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
    override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
        // Use the unique field, here assuming 'name' as unique identifier
        return oldItem.foodName == newItem.foodName
    }

    override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
        // Compare the entire object
        return oldItem == newItem
    }
}
