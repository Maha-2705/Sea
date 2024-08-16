package com.capztone.fishfy.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.ActivityManualLocationBinding
import java.io.IOException
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
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
    private var isMainActivityStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupKeyboardListener()
        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.WHITE
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.WHITE
            }
        }
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        geocoder = Geocoder(this) // or Geocoder(applicationContext) if preferred
        // Fetch shop locations from Firebase
        fetchShopLocationsFromFirebase()
        binding.etName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateName()
            }
        }

        binding.etMobileNumber.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateMobileNumber()
            }
        }


        binding.etLocality.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                validateCity()

            }
        }
        binding.etPincode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePincode()
            }
        }

        binding.AddressSave.setOnClickListener {
            if (validateName() && validateCity() && validatePincode() && validateMobileNumber()) {
                if (selectedAddressType != null && !isMainActivityStarted) {
                    // Disable the button to prevent multiple clicks
                    binding.AddressSave.isEnabled = false
                    saveAddressToFirebase()
                } else if (selectedAddressType == null) {
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
                // Removed the code that starts MainActivity
            }
        }

        binding.detailGoToBackImageButton.setOnClickListener {
            onBackPressed()
        }


        binding.btnSaveAsHome.setOnClickListener { onAddressTypeSelected("HOME", it) }
        binding.btnSaveAsWork.setOnClickListener { onAddressTypeSelected("WORK", it) }
        binding.btnSaveAsOther.setOnClickListener { onAddressTypeSelected("OTHER", it) }
    }
    override fun onResume() {
        super.onResume()
        // Reset the flag when activity resumes to allow starting MainActivity again
        isMainActivityStarted = false
    }
    private fun setupKeyboardListener() {
        binding.etLocality.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {

            }
        }


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
        val addressString = "${binding.etBuildingName.text}, "

        try {
            // Use Geocoder to get latitude and longitude from address
            val addresses: MutableList<Address>? = geocoder.getFromLocationName(addressString, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val userLat = addresses[0].latitude
                val userLng = addresses[0].longitude

                // Retrieve the distance threshold from Firebase
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val databaseReference = FirebaseDatabase.getInstance().getReference("Admins/spXRl1jY4yTlhDKZJzLicp8E9kc2/User Distance")
                    databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val distanceThresholdString = dataSnapshot.getValue(String::class.java)
                            val distanceThreshold = distanceThresholdString?.toDoubleOrNull() ?: 10.0

                            // Calculate distances between user location and shop locations
                            val nearbyShops = mutableListOf<String>()

                            for (i in adminDestinations.indices) {
                                val shopLat = adminDestinations[i].first
                                val shopLng = adminDestinations[i].second

                                val distance = calculateDistance(userLat, userLng, shopLat, shopLng)

                                if (distance < distanceThreshold) {
                                    nearbyShops.add(shopNames[i])
                                }
                            }

                            if (nearbyShops.isNotEmpty()) {
                                val shopsWithinThreshold = nearbyShops.joinToString(", ")
                                binding.shopnameTextView.text = shopsWithinThreshold
                                storeNearbyShopsInFirebase(shopsWithinThreshold)
                            } else {
                                // Delete the shop name if no shops are within the threshold
                                deleteShopNameFromFirebase(userId)
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Toast.makeText(
                                this@ManualLocation,
                                "Error fetching distance threshold: ${databaseError.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(
                        this@ManualLocation,
                        "User not authenticated",
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

    private fun deleteShopNameFromFirebase(userId: String) {
        val userLocationRef = database.child("Locations").child(userId).child("shopname")
        userLocationRef.removeValue()
            .addOnSuccessListener {
            }
            .addOnFailureListener {
            }
    }

    private fun storeNearbyShopsInFirebase(shops: String) {
        val userId = auth.currentUser?.uid ?: return
        val userLocationRef = database.child("Locations").child(userId)
        userLocationRef.child("shopname").setValue(shops)
            .addOnSuccessListener {

            }
            .addOnFailureListener {
                Toast.makeText(this@ManualLocation, "Failed to store nearby shops", Toast.LENGTH_SHORT)
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


    private fun setMandatoryFieldIndicatorVisible(
        visible: Boolean,
        text: String,
        textView: TextView
    ) {
        if (visible) {
            textView.visibility = View.VISIBLE
            textView.text = text  // Change text dynamically
        } else {
            textView.visibility = View.VISIBLE
        }
    }

    private fun validateName(): Boolean {
        val name = binding.etName.text.toString().trim()
        return when {
            TextUtils.isEmpty(name) -> {
                binding.etName.error = "Name is required"
                setMandatoryFieldIndicatorVisible(true, "Name *", binding.name)
                false
            }

            name.length !in 3..10 -> {
                binding.etName.error = "Name must be between 3 and 10 characters"
                setMandatoryFieldIndicatorVisible(true, "Name *", binding.name)
                false
            }

            else -> {
                setMandatoryFieldIndicatorVisible(false, "Name *", binding.name)
                true
            }
        }
    }

    private fun validateMobileNumber(): Boolean {
        val mobileNumber = binding.etMobileNumber.text.toString().trim()
        return when {
            TextUtils.isEmpty(mobileNumber) -> {
                binding.etMobileNumber.error = "Mobile number is required"
                setMandatoryFieldIndicatorVisible(true, "Mobile Number *", binding.PhoneNum)
                false
            }

            mobileNumber.length != 10 -> {
                binding.etMobileNumber.error = "Mobile number must be 10 digits"
                setMandatoryFieldIndicatorVisible(true, "Mobile Number *", binding.PhoneNum)
                false
            }

            else -> {
                setMandatoryFieldIndicatorVisible(false, "Mobile Number *", binding.PhoneNum)
                true
            }
        }
    }

    private fun validateCity(): Boolean {
        val city = binding.etLocality.text.toString().trim()
        val cityPattern = "^[a-zA-Z ]{3,20}\$".toRegex()


        return when {
            TextUtils.isEmpty(city) -> {
                binding.etLocality.error = "City is required"
                setMandatoryFieldIndicatorVisible(true, "City *", binding.local)
                false
            }

            !city.matches(cityPattern) -> {
                binding.etLocality.error = "Enter valid city name (alphabets only)"
                setMandatoryFieldIndicatorVisible(true, "City *", binding.local)
                false
            }

            else -> {
                setMandatoryFieldIndicatorVisible(false, "City *", binding.local)
                true
            }
        }

    }


    private fun validatePincode(): Boolean {
        val pincode = binding.etPincode.text.toString().trim()
        return when {
            TextUtils.isEmpty(pincode) -> {
                binding.etPincode.error = "Pincode is required"
                setMandatoryFieldIndicatorVisible(true, "Pincode *", binding.pin)
                false
            }

            pincode.length != 6 || !pincode.matches("\\d{6}".toRegex()) -> {
                binding.etPincode.error = "Pincode must be a 6-digit numeric value"
                setMandatoryFieldIndicatorVisible(true, "Pincode *", binding.pin)
                false
            }

            else -> {
                setMandatoryFieldIndicatorVisible(false, "Pincode *", binding.pin)
                true
            }
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


    @SuppressLint("SuspiciousIndentation")
    private fun saveAddressToFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val uidid = "spXRl1jY4yTlhDKZJzLicp8E9kc2"

        // Retrieve the "User Distance" value from Firebase
        val adminDistanceRef = database.child("Admins").child(uidid).child("User Distance")
        adminDistanceRef.get().addOnSuccessListener { dataSnapshot ->
            val userDistance = try {
                dataSnapshot.getValue(Double::class.java) ?: 10.0 // Default to 10.0 if not found or conversion fails
            } catch (e: DatabaseException) {
                try {
                    dataSnapshot.getValue(String::class.java)?.toDouble() ?: 10.0
                } catch (e: NumberFormatException) {
                    10.0 // Default to 10.0 if conversion fails
                }
            }

            // Full address string for saving to Firebase
            val name = binding.etName.text.toString().trim()
            val houseFlatNo = binding.etHouseFlatNo.text.toString().trim()
            val buildingName = binding.etBuildingName.text.toString().trim()
            val street = binding.etStreet.text.toString().trim()
            val locality = binding.etLocality.text.toString().trim()
            val pincode = binding.etPincode.text.toString().trim()
            val mobileNumber = binding.etMobileNumber.text.toString().trim()

            // List to store non-empty fields
            val addressParts = mutableListOf<String>()

            if (name.isNotEmpty()) addressParts.add(name)
            if (houseFlatNo.isNotEmpty()) addressParts.add(houseFlatNo)
            if (buildingName.isNotEmpty()) addressParts.add(buildingName)
            if (street.isNotEmpty()) addressParts.add(street)
            if (locality.isNotEmpty() && pincode.isNotEmpty()) {
                addressParts.add("$locality - $pincode")
            }
            if (mobileNumber.isNotEmpty()) addressParts.add("+91 $mobileNumber")

            // Join the list with commas to form the full address string
            val fullAddressString = addressParts.joinToString(", \n ")

            // Address string for geocoding (only locality and pincode)
            val geocodeAddressString = "$locality - $pincode"

            try {
                // Use Geocoder to get latitude and longitude from geocodeAddressString
                val addresses: MutableList<Address>? = geocoder.getFromLocationName(geocodeAddressString, 1)

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

                        if (distance > userDistance) { // Only proceed if distance is above userDistance kilometers
                            addressWithinThreshold = true
                            break
                        }
                    }

                    if (addressWithinThreshold) {
                        // Proceed to save address
                        val nearbyShops = mutableListOf<String>()

                        // Calculate distances and collect nearby shop names within userDistance kilometers
                        for (i in adminDestinations.indices) {
                            val shopLat = adminDestinations[i].first
                            val shopLng = adminDestinations[i].second

                            val distance = calculateDistance(latitude, longitude, shopLat, shopLng)

                            if (distance < userDistance) { // Collect nearby shop names within userDistance kilometers
                                nearbyShops.add(shopNames[i])
                            }
                        }

                        // Join the nearby shop names into a single string
                        val shopsWithinThreshold = nearbyShops.joinToString(", ")

                        // Store the address, latitude, longitude, and nearby shops in Firebase
                        val locationData = hashMapOf(
                            "address" to fullAddressString,
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "locality"  to locality,
                            "addressType" to selectedAddressType
                        )


                        if (nearbyShops.isNotEmpty()) {
                            locationData["shopname"] = shopsWithinThreshold
                        } else {
                            deleteShopNameFromFirebase(userId)
                            database.child("Locations").child(userId).child("shopname")
                                .removeValue()
                                .addOnCompleteListener { shopNameRemoveTask ->
                                    if (shopNameRemoveTask.isSuccessful) {

                                    } else {

                                    }
                                }
                        }

                        // Store the locality directly inside Locations -> userId
                        database.child("Locations").child(userId).child("locality")
                            .setValue(locality)
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
                        locationRef.child("type").setValue(selectedAddressType)
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
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val currentUserId = currentUser?.uid
                        // Store address data under selected address type
                        selectedAddressType?.let {
                            locationRef.child(it).setValue(locationData)
                                .addOnCompleteListener { addressSaveTask ->
                                    if (addressSaveTask.isSuccessful) {
                                        // Address saved successfully
                                        val userCartRef = currentUserId?.let { it1 ->
                                            FirebaseDatabase.getInstance().getReference("user")
                                                .child(it1)
                                                .child("cartItems")
                                        }

                                        if (userCartRef != null) {
                                            userCartRef.removeValue().addOnCompleteListener { removeCartTask ->
                                                if (removeCartTask.isSuccessful) {
                                                    Toast.makeText(
                                                        this@ManualLocation,
                                                        "Address saved successfully",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    val intent = if (nearbyShops.isNotEmpty()) {
                                                        Intent(this@ManualLocation, MainActivity::class.java)
                                                    } else {
                                                        Intent(this@ManualLocation, LocationNotAvailable::class.java)
                                                    }
                                                    startActivity(intent)
                                                } else {
                                                    Toast.makeText(
                                                        this@ManualLocation,
                                                        "Address saved but failed to delete cart items: ${removeCartTask.exception?.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@ManualLocation,
                                            "Failed to save address: ${addressSaveTask.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }

                        // Save the user details under "Locations" -> userId -> "User Details"
                        val userDetailsRef = database.child("Locations").child(userId).child("User Details")
                        userDetailsRef.child("user name").setValue(name)
                            .addOnCompleteListener { nameSaveTask ->
                                if (!nameSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ManualLocation,
                                        "Failed to save username: ${nameSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        userDetailsRef.child("mobile number").setValue(mobileNumber)
                            .addOnCompleteListener { mobileNumberSaveTask ->
                                if (!mobileNumberSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ManualLocation,
                                        "Failed to save mobile number: ${mobileNumberSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(
                            this@ManualLocation,
                            "You are within $userDistance kilometers of all shop locations.",
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

}