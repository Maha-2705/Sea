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
import android.widget.TextView
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
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        geocoder = Geocoder(this) // or Geocoder(applicationContext) if preferred
        // Fetch shop locations from Firebase
        fetchShopLocationsFromFirebase()

        binding.AddressSave.setOnClickListener {
            if (validateInput()) {
                if (selectedAddressType != null) {
                    saveAddressToFirebase()
                    val intent = Intent(this, LanguageActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Please select an address type",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Please fill all fields",
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
        val addressString =
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
        var isValid = true

        // Validate Name
        val name = binding.etName.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            binding.etName.error = "Name is required"
            setMandatoryFieldIndicatorVisible(true, "NAME *", binding.name)
            isValid = false
        } else if (name.length !in 3..10) {
            binding.etName.error = "Name must be between 3 and 10 characters"
            setMandatoryFieldIndicatorVisible(true, "NAME *", binding.name)
            isValid = false
        } else {
            setMandatoryFieldIndicatorVisible(false, "NAME *", binding.name)
        }

        // Validate Mobile Number
        val mobileNumber = binding.etMobileNumber.text.toString().trim()
        if (TextUtils.isEmpty(mobileNumber)) {
            binding.etMobileNumber.error = "Mobile number is required"
            setMandatoryFieldIndicatorVisible(true, "MOBILE NUMBER *", binding.PhoneNum)
            isValid = false
        } else if (mobileNumber.length != 10) {
            binding.etMobileNumber.error = "Mobile number must be 10 digits"
            setMandatoryFieldIndicatorVisible(true, "MOBILE NUMBER *", binding.PhoneNum)
            isValid = false
        } else {
            setMandatoryFieldIndicatorVisible(false, "MOBILE NUMBER *", binding.PhoneNum)
        }

        // Validate City (Locality)
        val city = binding.etLocality.text.toString().trim()
        if (TextUtils.isEmpty(city)) {
            binding.etLocality.error = "City is required"
            setMandatoryFieldIndicatorVisible(true, "CITY *", binding.local)
            isValid = false
        } else {
            setMandatoryFieldIndicatorVisible(false, "CITY *", binding.local)
        }

        // Validate Pincode
        val pincode = binding.etPincode.text.toString().trim()
        if (TextUtils.isEmpty(pincode)) {
            binding.etPincode.error = "Pincode is required"
            setMandatoryFieldIndicatorVisible(true, "PINCODE *", binding.pin)
            isValid = false
        } else if (pincode.length != 6 || !pincode.matches("\\d{6}".toRegex())) {
            binding.etPincode.error = "Pincode must be a 6-digit numeric value"
            setMandatoryFieldIndicatorVisible(true, "PINCODE *", binding.pin)
            isValid = false
        } else {
            setMandatoryFieldIndicatorVisible(false, "PINCODE *", binding.pin)
        }

        return isValid
    }

    private fun setMandatoryFieldIndicatorVisible(visible: Boolean, text: String, textView: TextView) {
        if (visible) {
            textView.visibility = View.VISIBLE
            textView.text = text  // Change text dynamically
        } else {
            textView.visibility = View.GONE
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
            button.compoundDrawablesRelative.forEach {
                it?.setTint(
                    ContextCompat.getColor(
                        this,
                        R.color.white
                    )
                )
            }
        }
        selectedAddressType = type
    }

    private fun resetButtonStyle(button: AppCompatButton, color: Int) {
        button.setBackgroundResource(R.drawable.colorlinear) // Set the default background drawable
        button.setTextColor(ContextCompat.getColor(this, color))
        button.compoundDrawablesRelative.forEach {
            it?.setTint(
                ContextCompat.getColor(
                    this,
                    color
                )
            )
        }
    }


    private fun saveAddressToFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val addressString = "${binding.etName.text.toString().trim()},\n" +
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
                    val locationData = hashMapOf(
                        "address" to addressString,
                        "latitude" to latitude,
                        "longitude" to longitude
                    )

                    if (nearbyShops.isNotEmpty()) {
                        locationData["shopname"] = shopsWithinThreshold
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

                    // Store latitude and longitude under Locations -> userId
                    val locationRef = database.child("Locations").child(userId)
                    locationRef.child("latitude").setValue(latitude)
                        .addOnCompleteListener { latitudeSaveTask ->
                            if (!latitudeSaveTask.isSuccessful) {
                                Toast.makeText(
                                    this@ManualLocation,
                                    "Failed to save latitude: ${latitudeSaveTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    locationRef.child("longitude").setValue(longitude)
                        .addOnCompleteListener { longitudeSaveTask ->
                            if (!longitudeSaveTask.isSuccessful) {
                                Toast.makeText(
                                    this@ManualLocation,
                                    "Failed to save longitude: ${longitudeSaveTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    // Store shopname under Locations -> userId -> shopname
                    database.child("Locations").child(userId).child("shopname")
                        .setValue(shopsWithinThreshold)
                        .addOnCompleteListener { shopnameSaveTask ->
                            if (!shopnameSaveTask.isSuccessful) {
                                Toast.makeText(
                                    this@ManualLocation,
                                    "Failed to save shopname: ${shopnameSaveTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    // Store address data under selected address type
                    selectedAddressType?.let {
                        locationRef.child(it).setValue(locationData)
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
            } else {
                Toast.makeText(
                    this@ManualLocation,
                    "No address found for provided location.",
                    Toast.LENGTH_SHORT
                ).show()
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