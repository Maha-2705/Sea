package com.capztone.seafishfy.ui.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capztone.seafishfy.databinding.FragmentShopOneBinding
import com.capztone.seafishfy.ui.activities.ViewModel.ShopOneViewModel
import com.capztone.seafishfy.ui.activities.adapters.MenuAdapter
import com.capztone.seafishfy.ui.activities.adapters.DiscountAdapter
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.capztone.seafishfy.ui.activities.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ShopOneFragment : Fragment() {

    private lateinit var binding: FragmentShopOneBinding
    private val viewModel: ShopOneViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShopOneBinding.inflate(inflater, container, false)
        binding.recentBackButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userRef = FirebaseDatabase.getInstance().getReference("Exploreshop").child(user.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shopName = snapshot.child("ShopName").getValue(String::class.java)
                    shopName?.let {
                        viewModel.retrieveData(it)
                    }
                }


                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }

        setupMenuRecyclerView(binding.popularRecyclerView)
        setupMenuRecyclerView(binding.popularRecyclerView1)
        setupMenuRecyclerView(binding.popularRecyclerView3)
        setupDiscountRecyclerView(binding.discountRecyclerView)

        observeViewModel()
    }

    private fun setupMenuRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager =  LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupDiscountRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun observeViewModel() {
        viewModel.menuItems.observe(viewLifecycleOwner) { items ->
            setMenuAdapter(items, binding.popularRecyclerView)
        }

        viewModel.menu1Items.observe(viewLifecycleOwner) { items ->
            setMenuAdapter(items, binding.popularRecyclerView1)
        }

        viewModel.menu2Items.observe(viewLifecycleOwner) { items ->
            setMenuAdapter(items, binding.popularRecyclerView3)
        }

        viewModel.discountItems.observe(viewLifecycleOwner) { items ->
            setDiscountAdapter(items)
        }
    }

    private fun setMenuAdapter(menuItems: List<MenuItem>, recyclerView: RecyclerView) {
        val adapter = MenuAdapter(menuItems.toMutableList(), requireContext())
        recyclerView.adapter = adapter
    }

    private fun setDiscountAdapter(discountItems: List<DiscountItem>) {
        val adapter = DiscountAdapter(discountItems, requireContext())
        binding.discountRecyclerView.adapter = adapter
    }
}
