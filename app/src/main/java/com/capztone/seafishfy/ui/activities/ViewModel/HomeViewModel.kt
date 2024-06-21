package com.capztone.seafishfy.ui.activities.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.capztone.seafishfy.ui.activities.models.Order
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val databaseReference: DatabaseReference = database.getReference("locations")

    private val _address = MutableLiveData<String?>()
    val address: LiveData<String?>
        get() = _address

    private val _locality = MutableLiveData<String?>()
    val locality: LiveData<String?>
        get() = _locality

    private val _latitude = MutableLiveData<Double?>()
    val latitude: MutableLiveData<Double?>
        get() = _latitude


    private val _longitude = MutableLiveData<Double?>()
    val longitude: MutableLiveData<Double?>
        get() = _longitude
    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> get() = _orders
    private val _menuItems = MutableLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>> get() = _menuItems

    private val databases: DatabaseReference = FirebaseDatabase.getInstance().reference.child("OrderDetails")
    private val auth: FirebaseAuth = Firebase.auth



    fun setMenuItems(items: List<MenuItem>) {
        _menuItems.value = items
    }

    fun setMenuItem(items: List<MenuItem>) {
        _menuItems.value = items
    }
    fun fetchOrders() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            databases.orderByChild("userUid").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val orderList = mutableListOf<Order>()
                        for (orderSnapshot in snapshot.children) {
                            val order = orderSnapshot.getValue(Order::class.java)
                            order?.let { orderList.add(it) }
                        }
                        // Reverse the order of the list
                        orderList.reverse()
                        _orders.value = orderList
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }

    init {
        retrieveData()
    }

    private fun retrieveData() {
        viewModelScope.launch {
            retrieveAddressAndLocality()
        }
    }

    private suspend fun retrieveAddressAndLocality() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userLocationRef = databaseReference.child(uid)
            try {
                val snapshot = userLocationRef.get().await()
                if (snapshot.exists()) {
                    val address = snapshot.child("address").getValue(String::class.java)
                    val locality = snapshot.child("locality").getValue(String::class.java)
                    val latitude = snapshot.child("latitude").getValue(Double::class.java)
                    val longitude = snapshot.child("longitude").getValue(Double::class.java)

                    _address.value = address
                    _locality.value = locality
                    _latitude.value = latitude
                    _longitude.value = longitude
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

}
