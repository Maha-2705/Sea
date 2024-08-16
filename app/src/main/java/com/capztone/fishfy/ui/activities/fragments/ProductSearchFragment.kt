package com.capztone.fishfy.ui.activities.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.databinding.FragmentProductSearchBinding
import com.capztone.fishfy.ui.activities.MainActivity
import com.capztone.fishfy.ui.activities.ViewModel.SearchViewModel
import com.capztone.fishfy.ui.activities.adapters.SearchAdapter
import com.capztone.fishfy.ui.activities.models.MenuItem

class ProductSearchFragment : Fragment() {
    private lateinit var binding: FragmentProductSearchBinding
    private lateinit var adapter: SearchAdapter
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductSearchBinding.inflate(inflater, container, false)

        setupObservers()
        viewModel.retrieveMenuItems()
        setupStatusBar()

        binding.searchBackButton.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }

        setupSearchView()
        return binding.root
    }

    private fun setupObservers() {
        viewModel.menuItemsLiveData.observe(viewLifecycleOwner) { menuItems ->
            menuItems?.let {
                setupRecyclerView(it)
                // Apply the filter after adapter is initialized
                applySearchQuery()
            }
        }
    }

    private fun setupRecyclerView(menuItems: List<MenuItem>) {
        adapter = SearchAdapter(menuItems, requireContext(), binding.noResultsTextView)
        binding.menuRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.menuRecyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    viewModel.setSearchQuery(it) // Save the search query
                    filterMenuItems(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    viewModel.setSearchQuery(it) // Save the search query
                    filterMenuItems(it)
                }
                return true
            }
        })
    }

    private fun filterMenuItems(query: String) {
        if (::adapter.isInitialized) {
            adapter.filter(query)
        }
    }

    private fun applySearchQuery() {
        val query = viewModel.getSearchQuery()
        binding.searchView.setQuery(query, false) // Restore the previous search query
        adapter.filter(query) // Restore the filtered items
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            applySearchQuery()
        }
    }

    private fun setupStatusBar() {
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }
    }
}
