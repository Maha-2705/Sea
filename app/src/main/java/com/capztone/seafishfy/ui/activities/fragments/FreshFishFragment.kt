package com.capztone.seafishfy.ui.activities.fragments
import android.content.Intent
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.seafishfy.databinding.FragmentFreshFishBinding
import com.capztone.seafishfy.databinding.FragmentSearchBinding
import com.capztone.seafishfy.ui.activities.MainActivity
import com.capztone.seafishfy.ui.activities.ViewModel.FreshFishViewModel
import com.capztone.seafishfy.ui.activities.adapters.SearchAdapter
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.capztone.seafishfy.ui.activities.ViewModel.SearchViewModel
import com.capztone.seafishfy.ui.activities.adapters.FreshFishAdapter
import com.capztone.seafishfy.ui.activities.adapters.NearItemAdapter

class FreshFishFragment: Fragment() {
    private lateinit var binding: FragmentFreshFishBinding
    private lateinit var adapter: FreshFishAdapter// Declare adapter property
    private val viewModel: FreshFishViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFreshFishBinding.inflate(inflater, container, false)

        setupObservers()
        binding.backBtn.setOnClickListener {
            // Navigate back to MainActivity
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
        }
        viewModel.retrieveMenuItems()
        return binding.root
    }

    private fun setupObservers() {
        viewModel.menuItemsLiveData.observe(viewLifecycleOwner) { menuItems ->
            menuItems?.let {
                showAllMenu(it)
            }
        }
    }

    private fun showAllMenu(menuItems: List<MenuItem>) {
        adapter = FreshFishAdapter(menuItems, requireContext())
        binding.freshfishrecycler.layoutManager = GridLayoutManager(requireContext(),3)
        binding.freshfishrecycler.adapter = adapter // Set the adapter here
    }


}

