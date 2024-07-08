package com.capztone.seafishfy.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.ActivityManualLocationBinding
import java.io.IOException
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.*


class ManualLocation : AppCompatActivity() {

    private lateinit var binding: ActivityManualLocationBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var selectedAddressType: String? = null
    private lateinit var geocoder: Geocoder

    // Variables for shop locations
    private val adminDestinations = mutableListOf<Pair<Double, Double>>()
    private val shopNames = mutableListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        geocoder = Geocoder(this) // or Geocoder(applicationContext) if preferred
        // Fetch shop locations from Firebase
        fetchShopLocationsFromFirebase()

        binding.AddressSave.setOnClickListener {
            if (validateInput() && selectedAddressType != null) {
                saveAddressToFirebase()
                val intent = Intent(this, LanguageActivity::class.java)
                startActivity(intent)

            } else {
                Toast.makeText(
                    this,
                    "Please fill all fields and select an address type",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnSaveAsHome.setOnClickListener { onAddressTypeSelected("HOME", it) }
        binding.btnSaveAsWork.setOnClickListener { onAddressTypeSelected("WORK", it) }
        binding.btnSaveAsOther.setOnClickListener { onAddressTypeSelected("OTHER", it) }
    }

    private fun fetchShopLocationsFromFirebase() {
        val shopLocationsRef = database.child("ShopLocations")
        shopLocationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (shopSnapshot in dataSnapshot.children) {
                    val shopName = shopSnapshot.key ?: continue
                    val lat =
                        shopSnapshot.child("latitude").getValue(Double::class.java) ?: continue
                    val lng =
                        shopSnapshot.child("longitude").getValue(Double::class.java) ?: continue
                    adminDestinations.add(Pair(lat, lng))
                    shopNames.add(shopName)
                }

                // Calculate distances once shop locations are fetched
                calculateDistances()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@ManualLocation,
                    "Failed to load shop locations",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateDistances() {
        val addressString = "${binding.etBuildingName.text}, " +
                "${binding.etHouseFlatNo.text}, " +
                "${binding.etStreet.text}, " +
                "${binding.etLocality.text} - " +
                "${binding.etPincode.text}"

        try {
            // Use Geocoder to get latitude and longitude from address
            val addresses: MutableList<Address>? = geocoder.getFromLocationName(addressString, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val userLat = addresses[0].latitude
                val userLng = addresses[0].longitude

                // Calculate distances between user location and shop locations
                val nearbyShops = mutableListOf<String>()

                for (i in adminDestinations.indices) {
                    val shopLat = adminDestinations[i].first
                    val shopLng = adminDestinations[i].second

                    val distance = calculateDistance(userLat, userLng, shopLat, shopLng)

                    if (distance < 10.0) { // Adjust the distance threshold as needed (10.0 kilometers in this case)
                        nearbyShops.add(shopNames[i])
                    }
                }

                if (nearbyShops.isNotEmpty()) {
                    val shopsWithinThreshold = nearbyShops.joinToString(", ")
                    binding.shopnameTextView.text = shopsWithinThreshold

                }
            }
        } catch (e: IOException) {
            Toast.makeText(
                this@ManualLocation,
                "Error geocoding address: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Radius of the Earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c // Distance in kilometers
    }

    private fun validateInput(): Boolean {
        return when {
            TextUtils.isEmpty(binding.etName.text.toString().trim()) -> {
                binding.etName.error = "Name is required"
                false
            }
            binding.etName.text.toString().trim().length < 3 -> {
                binding.etName.error = "Name must be at least 3 characters"
                false
            }
            binding.etName.text.toString().trim().length > 10 -> {
                binding.etName.error = "Name must be at most 10 characters"
                false
            }

            TextUtils.isEmpty(binding.etMobileNumber.text.toString().trim()) -> {
                binding.etMobileNumber.error = "Mobile number is required"
                false
            }
            binding.etMobileNumber.text.toString().trim().length != 10 -> {
                binding.etMobileNumber.error = "Mobile number must be 10 digits"
                false
            }
            TextUtils.isEmpty(binding.etHouseFlatNo.text.toString().trim()) -> {
                binding.etHouseFlatNo.error = "House/Flat No is required"
                false
            }
            TextUtils.isEmpty(binding.etBuildingName.text.toString().trim()) -> {
                binding.etBuildingName.error = "Building name is required"
                false
            }
            TextUtils.isEmpty(binding.etStreet.text.toString().trim()) -> {
                binding.etStreet.error = "Street name is required"
                false
            }
            TextUtils.isEmpty(binding.etPincode.text.toString().trim()) -> {
                binding.etPincode.error = "Pincode is required"
                false
            }
            binding.etPincode.text.toString().trim().length != 6 || !binding.etPincode.text.toString().trim().matches("\\d{6}".toRegex()) -> {
                binding.etPincode.error = "Pincode must be a 6-digit numeric value"
                false
            }
            TextUtils.isEmpty(binding.etLocality.text.toString().trim()) -> {
                binding.etLocality.error = "Locality is required"
                false
            }
            else -> true
        }
    }

    private fun onAddressTypeSelected(type: String, button: View) {
        // Reset all buttons to default color and icon tint
        resetButtonStyle(binding.btnSaveAsHome, R.color.navy)
        resetButtonStyle(binding.btnSaveAsWork, R.color.navy)
        resetButtonStyle(binding.btnSaveAsOther, R.color.navy)

        // Change the background drawable, text color, and icon tint of the selected button
        if (button is AppCompatButton) {
            button.setBackgroundResource(R.drawable.linearbg) // Use drawable resource for background
            button.setTextColor(ContextCompat.getColor(this, R.color.white))
            button.compoundDrawablesRelative.forEach { it?.setTint(ContextCompat.getColor(this, R.color.white)) }
        }
        selectedAddressType = type
    }

    private fun resetButtonStyle(button: AppCompatButton, color: Int) {
        button.setBackgroundResource(R.drawable.colorlinear) // Set the default background drawable
        button.setTextColor(ContextCompat.getColor(this, color))
        button.compoundDrawablesRelative.forEach { it?.setTint(ContextCompat.getColor(this, color)) }
    }



    private fun saveAddressToFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val addressString = "${binding.etName.text.toString().trim()},\n" +
                "${binding.etHouseFlatNo.text.toString().trim()}, " +
                "${binding.etStreet.text.toString().trim()}, " +
                "${binding.etBuildingName.text.toString().trim()}\n" +
                "${binding.etLocality.text.toString().trim()} - " +
                "${binding.etPincode.text.toString().trim()},\n" +
        "${binding.etMobileNumber.text.toString().trim()} "
        val locality = binding.etLocality.text.toString().trim()
        try {
            // Use Geocoder to get latitude and longitude from address
            val addresses: MutableList<Address>? = geocoder.getFromLocationName(addressString, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val latitude = addresses[0].latitude
                val longitude = addresses[0].longitude

                // Check if the distance to any shop location is greater than 10 kilometers
                var addressWithinThreshold = false

                // Calculate distances between user location and shop locations
                for (i in adminDestinations.indices) {
                    val shopLat = adminDestinations[i].first
                    val shopLng = adminDestinations[i].second

                    val distance = calculateDistance(latitude, longitude, shopLat, shopLng)

                    if (distance > 10.0) { // Only proceed if distance is above 10.0 kilometers
                        addressWithinThreshold = true
                        break
                    }
                }

                if (addressWithinThreshold) {
                    // Proceed to save address
                    val nearbyShops = mutableListOf<String>()

                    // Calculate distances and collect nearby shop names within 10.0 kilometers
                    for (i in adminDestinations.indices) {
                        val shopLat = adminDestinations[i].first
                        val shopLng = adminDestinations[i].second

                        val distance = calculateDistance(latitude, longitude, shopLat, shopLng)

                        if (distance < 10.0) { // Collect nearby shop names within 10.0 kilometers
                            nearbyShops.add(shopNames[i])
                        }
                    }

                    // Join the nearby shop names into a single string
                    val shopsWithinThreshold = nearbyShops.joinToString(", ")

                    // Store the address, latitude, longitude, and nearby shops in Firebase
                    val locationData = HashMap<String, Any>()
                    locationData["address"] = addressString
                    locationData["latitude"] = latitude
                    locationData["longitude"] = longitude

                    if (nearbyShops.isNotEmpty()) {
                        locationData["shopname"] = shopsWithinThreshold
                    } else {
                        // Remove shopname if no nearby shops
                        locationData.remove("shopname")
                    }

                    // Store the locality directly inside Locations -> userId
                    database.child("Locations").child(userId).child("locality").setValue(locality)
                        .addOnCompleteListener { localitySaveTask ->
                            if (!localitySaveTask.isSuccessful) {
                                Toast.makeText(
                                    this@ManualLocation,
                                    "Failed to save locality: ${localitySaveTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    // Store shopname under Locations -> userId -> shopname
                    database.child("Locations").child(userId).child("shopname")
                        .setValue(shopsWithinThreshold)
                        .addOnCompleteListener { shopnameSaveTask ->
                            if (shopnameSaveTask.isSuccessful) {
                                // Optional: Handle success message or action if needed
                            } else {
                                Toast.makeText(
                                    this@ManualLocation,
                                    "Failed to save shopname: ${shopnameSaveTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    // Store address data under selected address type
                    selectedAddressType?.let {
                        database.child("Locations").child(userId).child(it).setValue(locationData)
                            .addOnCompleteListener { addressSaveTask ->
                                if (addressSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ManualLocation,
                                        "Address saved successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    val intent = if (nearbyShops.isNotEmpty()) {
                                        Intent(this@ManualLocation, LanguageActivity::class.java)
                                    } else {
                                        Intent(
                                            this@ManualLocation,
                                            LocationNotAvailable::class.java
                                        )
                                    }
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(
                                        this@ManualLocation,
                                        "Failed to save address: ${addressSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(
                        this@ManualLocation,
                        "You are within 10 kilometers of all shop locations.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: IOException) {
            Toast.makeText(
                this@ManualLocation,
                "Error geocoding address: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}