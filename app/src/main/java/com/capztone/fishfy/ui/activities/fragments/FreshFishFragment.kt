package com.capztone.fishfy.ui.activities.fragments
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentFreshFishBinding
import com.capztone.fishfy.ui.activities.MainActivity
import com.capztone.fishfy.ui.activities.ViewModel.FreshFishViewModel
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.capztone.fishfy.ui.activities.adapters.FreshFishAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FreshFishFragment: Fragment() {
    private lateinit var binding: FragmentFreshFishBinding
    private lateinit var adapter: FreshFishAdapter// Declare adapter property

    private val viewModel: FreshFishViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFreshFishBinding.inflate(inflater, container, false)
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color. TRANSPARENT
            }
        }


        setupObservers()
        binding.backBtn.setOnClickListener {
            // Navigate back to MainActivity
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
        }
        viewModel.retrieveMenuItems()
        retrieveAndDisplayUserInfo()
        return binding.root
    }
    private fun retrieveAndDisplayUserInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val userId = it.uid
            val database = FirebaseDatabase.getInstance().getReference("user").child(userId)

            database.child("category").get().addOnSuccessListener { dataSnapshot ->
                val category = dataSnapshot.getValue(String::class.java)
                binding.textView15.text = "$category"
            }.addOnFailureListener {
                // Handle any errors
                binding.textView15.text = "Failed to load user info"
            }
        }
    }


    private fun setupObservers() {
        viewModel.menuItemsLiveData.observe(viewLifecycleOwner) { menuItems ->
            menuItems?.let {
                showAllMenu(it)
            }
        }
    }

    private fun showAllMenu(menuItems: List<MenuItem>) {
        adapter = FreshFishAdapter(menuItems.toMutableList(), requireContext())
        binding.freshfishrecycler.layoutManager =  LinearLayoutManager(requireContext())
        binding.freshfishrecycler.adapter = adapter // Set the adapter here
    }


}