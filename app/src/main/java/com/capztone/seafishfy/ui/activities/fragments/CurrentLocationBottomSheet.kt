package com.capztone.seafishfy.ui.activities.fragments

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.capztone.seafishfy.databinding.FragmentCurrentLocationBottomSheetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import kotlin.math.*
import com.capztone.seafishfy.R
import com.capztone.seafishfy.ui.activities.LanguageActivity
import com.capztone.seafishfy.ui.activities.LocationNotAvailable
import com.capztone.seafishfy.ui.activities.ManualLocation

class CurrentLocationBottomSheet : DialogFragment() {

    private var _binding: FragmentCurrentLocationBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var selectedAddressType: String? = null
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var geocoder: Geocoder

    // Variables for shop locations
    private val adminDestinations = mutableListOf<Pair<Double, Double>>()
    private val shopNames = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCurrentLocationBottomSheetBinding.inflate(inflater, container, false)
        val address = arguments?.getString("address", "No Address")
        binding.etBuildingName.setText(address)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        geocoder = Geocoder(requireContext())
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            }
        }
        // Fetch shop locations from Firebase
        fetchShopLocationsFromFirebase()

        binding.btnSaveAsHome.setOnClickListener {
            onAddressTypeSelected("HOME", it)
        }

        binding.btnSaveAsWork.setOnClickListener {
            onAddressTypeSelected("WORK", it)
        }

        binding.btnSaveAsOther.setOnClickListener {
            onAddressTypeSelected("OTHER", it)
        }

        binding.AddressSave.setOnClickListener {
            if (validateInput()) {
                saveAddressToFirebase()
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                    requireContext(),
                    "Failed to load shop locations",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateDistances() {
        val addressString = "${binding.etBuildingName.text}, "

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
                    storeNearbyShopsInFirebase(shopsWithinThreshold)
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location not found for the entered address",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: IOException) {
            Toast.makeText(
                requireContext(),
                "Error geocoding address: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun storeNearbyShopsInFirebase(shops: String) {
        val userId = auth.currentUser?.uid ?: return
        val userLocationRef = database.child("Locations").child(userId)
        userLocationRef.child("shopname").setValue(shops)
            .addOnSuccessListener {

            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to store nearby shops", Toast.LENGTH_SHORT)
                    .show()
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

    private fun onAddressTypeSelected(type: String, button: View) {
        // Reset all buttons to default color and icon tint
        resetButtonStyle(binding.btnSaveAsHome, R.color.navy)
        resetButtonStyle(binding.btnSaveAsWork, R.color.navy)
        resetButtonStyle(binding.btnSaveAsOther, R.color.navy)

        // Change the background drawable, text color, and icon tint of the selected button
        if (button is AppCompatButton) {
            button.setBackgroundResource(R.drawable.linearbg) // Use drawable resource for background
            button.setTextColor(ContextCompat.getColor( requireContext(), R.color.white))
            button.compoundDrawablesRelative.forEach { it?.setTint(ContextCompat.getColor( requireContext(), R.color.white)) }
        }
        selectedAddressType = type
    }

    private fun resetButtonStyle(button: AppCompatButton, color: Int) {
        button.setBackgroundResource(R.drawable.colorlinear) // Set the default background drawable
        button.setTextColor(ContextCompat.getColor( requireContext(), color))
        button.compoundDrawablesRelative.forEach { it?.setTint(ContextCompat.getColor( requireContext(), color)) }
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
            setMandatoryFieldIndicatorVisible(true, "MOBILE NUMBER *", binding.mblnum)
            isValid = false
        } else if (mobileNumber.length != 10) {
            binding.etMobileNumber.error = "Mobile number must be 10 digits"
            setMandatoryFieldIndicatorVisible(true, "MOBILE NUMBER *", binding.mblnum)
            isValid = false
        } else {
            setMandatoryFieldIndicatorVisible(false, "MOBILE NUMBER *", binding.mblnum)
        }
        val address = binding.etBuildingName.text.toString().trim()
        if (TextUtils.isEmpty(address)) {
            binding.etBuildingName.error = "Address is required"
            setMandatoryFieldIndicatorVisible(true, "ADDRESS *", binding.Address)
            isValid = false
        }
        // Validate City (Locality)


        // Validate Pincode

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
    private fun saveAddressToFirebase() {
        val userId = auth.currentUser?.uid ?: return

        val addressString = "${binding.etName.text.toString().trim()},\n" +
                "${binding.etBuildingName.text.toString().trim()}\n" +
                "${binding.etMobileNumber.text.toString().trim()} "

        try {
            // Use Geocoder to get latitude and longitude from address
            val addresses: MutableList<Address>? = geocoder.getFromLocationName(addressString, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val latitude = addresses[0].latitude
                val longitude = addresses[0].longitude
                // Extract locality from the geocoded address
                val locality = addresses[0].locality

                // Check if the distance to any shop location is greater than 10 kilometers
                var addressWithinThreshold = false

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
                        // Remove shopname from Firebase if no nearby shops
                        database.child("Locations").child(userId).child("shopname").removeValue()
                            .addOnCompleteListener { shopNameRemoveTask ->
                                if (shopNameRemoveTask.isSuccessful) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Removed shopname from Firebase",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to remove shopname: ${shopNameRemoveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }

                    database.child("Locations").child(userId).child("locality").setValue(locality)
                        .addOnCompleteListener { localitySaveTask ->
                            if (!localitySaveTask.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
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
                                    requireContext(),
                                    "Failed to save latitude: ${latitudeSaveTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    locationRef.child("longitude").setValue(longitude)
                        .addOnCompleteListener { longitudeSaveTask ->
                            if (!longitudeSaveTask.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to save longitude: ${longitudeSaveTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    selectedAddressType?.let {
                        database.child("Locations").child(userId).child(it).setValue(locationData)
                            .addOnCompleteListener { addressSaveTask ->
                                if (addressSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Address saved successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    val intent = if (nearbyShops.isNotEmpty()) {
                                        Intent(requireContext(), LanguageActivity::class.java)
                                    } else {
                                        Intent(requireContext(), LocationNotAvailable::class.java)
                                    }
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to save address and shops: ${addressSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "You are within 10 kilometers of all shop locations.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location not found for the entered address",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: IOException) {
            Toast.makeText(
                requireContext(),
                "Error geocoding address: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}