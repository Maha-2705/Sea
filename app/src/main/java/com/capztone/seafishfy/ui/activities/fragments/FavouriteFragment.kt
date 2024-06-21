package com.capztone.seafishfy.ui.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.seafishfy.databinding.FragmentFavouriteBinding
import com.capztone.seafishfy.ui.activities.DetailsActivity
import com.capztone.seafishfy.ui.activities.adapters.FavouriteAdapter
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavouriteFragment : Fragment() {

    private var _binding: FragmentFavouriteBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FavouriteAdapter
    private lateinit var database: DatabaseReference
    private val menuItems = mutableListOf<MenuItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavouriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        fetchFavorites()
    }

    private fun setupRecyclerView() {
        adapter = FavouriteAdapter(requireContext(), menuItems) { menuItem ->
            openDetailsActivity(menuItem)
        }
        binding.favRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.favRecyclerView.adapter = adapter
    }

    private fun fetchFavorites() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            database = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    menuItems.clear()
                    for (dataSnapshot in snapshot.children) {
                        val menuItem = dataSnapshot.getValue(MenuItem::class.java)
                        if (menuItem != null && menuItem.favorite) {
                            menuItems.add(menuItem)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }
            })
        }
    }

    private fun openDetailsActivity(menuItem: MenuItem) {
        val intent = Intent(requireContext(), DetailsActivity::class.java).apply {
            putExtra("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "")
            putExtra("MenuItemPrice", menuItem.foodPrice)
            putExtra("MenuItemDescription", menuItem.foodDescription)
            putExtra("MenuItemImage", menuItem.foodImage)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
