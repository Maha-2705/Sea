package com.capztone.seafishfy.ui.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.seafishfy.databinding.FragmentFavouriteBinding
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
            // Handle item click if needed
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
                            menuItem.firebaseKey = dataSnapshot.key // Ensure firebaseKey is set
                            menuItems.add(menuItem)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    updateEmptyCartMessageVisibility()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }
            })
        }
    }

    private fun updateEmptyCartMessageVisibility() {
        _binding?.let {
            if (menuItems.isEmpty()) {
                it.emptyCartMessage.visibility = View.VISIBLE
                it.scrollViewCart.visibility = View.GONE
            } else {
                it.emptyCartMessage.visibility = View.GONE
                it.scrollViewCart.visibility = View.VISIBLE
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}