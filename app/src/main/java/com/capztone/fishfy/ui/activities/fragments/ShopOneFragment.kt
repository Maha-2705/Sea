package com.capztone.fishfy.ui.activities.fragments

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentShopOneBinding
import com.capztone.fishfy.ui.activities.ViewModel.ShopOneViewModel
import com.capztone.fishfy.ui.activities.adapters.MenuAdapter
import com.capztone.fishfy.ui.activities.models.MenuItem
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
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.WHITE
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.WHITE
            }
        }
        // Show loading indicator
        binding.progress.visibility = View.VISIBLE

        binding.progress.setProgressVector(resources.getDrawable(R.drawable.spinload))
        binding.progress.setTextViewVisibility(true)
        binding.progress.setTextStyle(true)
        binding.progress.setTextColor(Color.YELLOW)
        binding.progress.setTextSize(12F)
        binding.progress.setTextMsg("Please Wait")
        binding.progress.setEnlarge(5)
        // Start a delay to hide the loading indicator after 5000 milliseconds (5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE

            // Call your method to retrieve cart items or perform other operations

        }, 1500)
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
        viewModel.menuItems.observe(viewLifecycleOwner) { menuItems ->
            // Update RecyclerView adapter with menuItems
            if (menuItems.isNotEmpty()) {
                binding.textView19.visibility = View.VISIBLE
                binding.textView13.visibility = View.VISIBLE
                binding.textView21.visibility = View.VISIBLE
                binding.shrimp.visibility = View.VISIBLE
                binding.crab.visibility = View.VISIBLE
                binding.lobster.visibility = View.VISIBLE

            }
        }

        setupMenuRecyclerView(binding.popularRecyclerView)
        setupMenuRecyclerView(binding.popularRecyclerView1)
        setupMenuRecyclerView(binding.popularRecyclerView3)
        setupMenuRecyclerView(binding.crabRecycler)
        setupMenuRecyclerView(binding.shrimpRecycler)
        setupMenuRecyclerView(binding.lobsterRecycler)



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
        viewModel.menu3Items.observe(viewLifecycleOwner) { items ->
            setMenuAdapter(items, binding.crabRecycler)
        }
        viewModel.menu4Items.observe(viewLifecycleOwner) { items ->
            setMenuAdapter(items, binding.shrimpRecycler)
        }
        viewModel.menu5Items.observe(viewLifecycleOwner) { items ->
            setMenuAdapter(items, binding.lobsterRecycler)
        }
    }

    private fun setMenuAdapter(menuItems: List<MenuItem>, recyclerView: RecyclerView) {
        val adapter = MenuAdapter(menuItems.toMutableList(), requireContext())
        recyclerView.adapter = adapter
        // Set LinearLayoutManager with horizontal orientation
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }



}