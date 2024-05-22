package com.example.seafishfy.ui.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.seafishfy.databinding.FragmentHistoryBinding
import com.example.seafishfy.ui.activities.Utils.ToastHelper
import com.example.seafishfy.ui.activities.adapters.RecentBuyAdapter
import com.example.seafishfy.ui.activities.ViewModel.HistoryViewModel
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

  observeOrders()
 }

 private fun observeOrders() {
  viewModel.orders.observe(viewLifecycleOwner) { orders ->
   orders?.let {
    adapter.submitList(orders)
   }
  }
 }

 fun onCancelOrder(orderId: String) {
  CoroutineScope(Dispatchers.Main).launch {
   val isSuccess = viewModel.cancelOrder(orderId)
   if (isSuccess) {
    ToastHelper.showCustomToast(context, "Your order has been cancelled")
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
