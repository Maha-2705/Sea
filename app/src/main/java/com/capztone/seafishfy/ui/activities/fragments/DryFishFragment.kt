package com.capztone.seafishfy.ui.activities.fragments
import android.content.Intent
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.capztone.seafishfy.databinding.FragmentDryFishBinding
import com.capztone.seafishfy.ui.activities.MainActivity
import com.capztone.seafishfy.ui.activities.ViewModel.DryFishViewModel
import com.capztone.seafishfy.ui.activities.adapters.FreshFishAdapter
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.capztone.seafishfy.ui.activities.adapters.NearItemAdapter

class DryFishFragment: Fragment() {
    private lateinit var binding: FragmentDryFishBinding
    private lateinit var adapter: FreshFishAdapter // Declare adapter property
    private val viewModel: DryFishViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDryFishBinding.inflate(inflater, container, false)

        setupObservers()
        // Set OnClickListener for backBtn
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
        binding.dryfishrecycler.layoutManager = GridLayoutManager(requireContext(),3)
        binding.dryfishrecycler.adapter = adapter // Set the adapter here
    }


}

