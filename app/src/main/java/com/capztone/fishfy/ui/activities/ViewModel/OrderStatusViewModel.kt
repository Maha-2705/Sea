// OrderStatusViewModel.kt

package com.capztone.fishfy.ui.activities.ViewModel

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.lottie.LottieAnimationView
import com.capztone.fishfy.databinding.ActivityOrderStatusBinding
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrderStatusViewModel : ViewModel() {

    private lateinit var database: DatabaseReference
    private lateinit var itemPushKey: String
    private lateinit var orderDeliveredAnimationView: LottieAnimationView
    private lateinit var binding: ActivityOrderStatusBinding

    fun init(itemPushKey: String, binding: ActivityOrderStatusBinding) {
        this.itemPushKey = itemPushKey
        this.binding = binding
        database = FirebaseDatabase.getInstance().reference
        orderDeliveredAnimationView = binding.deliverylottie

        observeOrderStatusChanges()
        observeEstimatedTimeChanges()
        observeTimestampChanges()
        observeUsernameChanges()
    }

    private fun observeOrderStatusChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            database.child("status").child(itemPushKey)
                .addValueEventListener(object : ValueEventListener {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val status = dataSnapshot.child("message").getValue(String::class.java)
                        updateUI(status)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }

    private fun observeEstimatedTimeChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            database.child("Estimated Time").child(itemPushKey)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val estimatedTime =
                            dataSnapshot.child("estimated_time").getValue(String::class.java)
                        binding.estimatedtime.text = estimatedTime ?: "Waiting for Update"
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }
    private fun observeUsernameChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            database.child("status").child(itemPushKey).child("username")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val username = dataSnapshot.getValue(String::class.java)
                        val displayText = "The order was taken by ${username ?: "..."}"
                        binding.username.text = displayText
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }


    private fun observeTimestampChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            database.child("status").child(itemPushKey).child("timestamp")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val timestamp = dataSnapshot.getValue(String::class.java)
                        val formattedTimestamp = timestamp ?: ""
                        binding.delitime.text = "Delivered Time: $formattedTimestamp"
                        if (timestamp != null) {
                            binding.delitime.visibility = View.VISIBLE
                            orderDeliveredAnimationView.visibility = View.VISIBLE
                            orderDeliveredAnimationView.playAnimation()
                        } else {
                            binding.delitime.visibility = View.INVISIBLE
                            orderDeliveredAnimationView.visibility = View.INVISIBLE
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateUI(status: String?) {
        when (status) {
            "Order confirmed" -> {
                binding.confirmtick.setImageResource(android.R.drawable.checkbox_on_background)
            }

            "Order picked" -> {
                binding.picktick.setImageResource(android.R.drawable.checkbox_on_background)
            }

            "Order delivered" -> {
                binding.delivertick.setImageResource(android.R.drawable.checkbox_on_background)
            }

            else -> {
                binding.confirmtick.setImageResource(0)
                binding.picktick.setImageResource(0)
                binding.delivertick.setImageResource(0)
            }
        }
    }
}
