package com.capztone.seafishfy.ui.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.FragmentHistoryBinding
import com.capztone.seafishfy.ui.activities.Utils.ToastHelper
import com.capztone.seafishfy.ui.activities.adapters.RecentBuyAdapter
import com.capztone.seafishfy.ui.activities.ViewModel.HistoryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

 private lateinit var binding: FragmentHistoryBinding
 private lateinit var viewModel: HistoryViewModel
 private lateinit var adapter: RecentBuyAdapter

 override fun onCreateView(
  inflater: LayoutInflater, container: ViewGroup?,
  savedInstanceState: Bundle?
 ): View {
  binding = FragmentHistoryBinding.inflate(inflater, container, false)
  return binding.root
 }

 override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
  super.onViewCreated(view, savedInstanceState)

  viewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)
  adapter = RecentBuyAdapter(requireContext(), viewModel)

  binding.recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
  binding.recentRecyclerView.adapter = adapter

  binding.recentBackButton.setOnClickListener {
   requireActivity().onBackPressed()
  }
  binding.shopnow.setOnClickListener {
   findNavController().navigate(R.id.action_cartFragment_to_homefragment)
  }

  observeOrders()
 }

 private fun observeOrders() {
  viewModel.orders.observe(viewLifecycleOwner) { orders ->
   orders?.let {
    adapter.submitList(orders)
    updateEmptyOrdersMessageVisibility(orders.isEmpty())   }
  }
 }
 private fun updateEmptyOrdersMessageVisibility(isEmpty: Boolean) {
  if (isEmpty) {
   binding.emptyCartMessage.visibility = View.VISIBLE
   binding.recentRecyclerView.visibility = View.GONE
   binding.shopnow.visibility=View.VISIBLE
  } else {
   binding.emptyCartMessage.visibility = View.GONE
   binding.shopnow.visibility = View.GONE
   binding.recentRecyclerView.visibility = View.VISIBLE
  }
 }
 fun onCancelOrder(orderId: String) {
  CoroutineScope(Dispatchers.Main).launch {
   val isSuccess = viewModel.cancelOrder(orderId)
   if (isSuccess) {
    context?.let { ToastHelper.showCustomToast(it, "Your order has been cancelled") }
   } else {
    Toast.makeText(requireContext(), "Failed to cancel order", Toast.LENGTH_SHORT).show()
   }
  }
 }

 override fun onResume() {
  super.onResume()
  fetchOrders()
 }

 private fun fetchOrders() {
  CoroutineScope(Dispatchers.Main).launch {
   viewModel.fetchOrders()
  }
 }

}
