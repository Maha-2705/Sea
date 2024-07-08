package com.capztone.seafishfy.ui.activities.fragments
import android.content.Intent
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.capztone.seafishfy.databinding.FragmentPicklesBinding
import com.capztone.seafishfy.databinding.FragmentSearchBinding
import com.capztone.seafishfy.ui.activities.MainActivity
import com.capztone.seafishfy.ui.activities.ViewModel.FreshFishViewModel
import com.capztone.seafishfy.ui.activities.ViewModel.PicklesViewModel
import com.capztone.seafishfy.ui.activities.adapters.SearchAdapter
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.capztone.seafishfy.ui.activities.ViewModel.SearchViewModel
import com.capztone.seafishfy.ui.activities.adapters.FreshFishAdapter
import com.capztone.seafishfy.ui.activities.adapters.NearItemAdapter

class PicklesFragment: Fragment() {
    private lateinit var binding: FragmentPicklesBinding
    private lateinit var adapter: FreshFishAdapter // Declare adapter property
    private val viewModel: PicklesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPicklesBinding.inflate(inflater, container, false)

        setupObservers()
        binding.backBtnn.setOnClickListener {
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
        adapter = FreshFishAdapter(menuItems.toMutableList(), requireContext())
        binding.picklesrecycler.layoutManager = GridLayoutManager(requireContext(),3)
        binding.picklesrecycler.adapter = adapter // Set the adapter here
    }


}

