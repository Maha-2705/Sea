package com.capztone.seafishfy.ui.activities

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.seafishfy.databinding.ActivityManualLocationBinding
import com.capztone.seafishfy.ui.activities.MainActivity
import com.capztone.seafishfy.ui.activities.MapsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class ManualLocation : AppCompatActivity() {

    private lateinit var binding: ActivityManualLocationBinding
    private val MAP_REQUEST_CODE = 1001
    private lateinit var auth: FirebaseAuth
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManualLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        binding.manualSelect.setOnClickListener {
            val intent = Intent(this, ManualMap::class.java)
            startActivityForResult(intent, MAP_REQUEST_CODE)
        }

        binding.Save.setOnClickListener {
            if (areDetailsFilled()) {
                // Save user location
                if (selectedLatitude != null && selectedLongitude != null) {
                    saveUserLocation(selectedLatitude!!, selectedLongitude!!)
                }

                // Start MainActivity only when all details are filled
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                // Show toast indicating that all details must be filled
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun areDetailsFilled(): Boolean {
        val address = binding.manualSelect.text.toString()
        val doorNo = binding.doorNo.text.toString()
        val streetName = binding.Street.text.toString()
        val cityName = binding.City.text.toString()

        return address.isNotEmpty() && doorNo.isNotEmpty() && streetName.isNotEmpty() && cityName.isNotEmpty()
    }

    private fun saveUserLocation(latitude: Double, longitude: Double) {
        val userId = auth.currentUser?.uid ?: ""
        val doorNo = binding.doorNo.text.toString()
        val locality = binding.City.text.toString()

        val address = getAddress(latitude, longitude)

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Locations").child(userId)

        myRef.setValue(UserLocation(userId, latitude, longitude, doorNo,locality , address))
    }

    private fun getAddress(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        return if (addresses.isNullOrEmpty()) {
            ""
        } else {
            addresses[0].getAddressLine(0) ?: ""
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MAP_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val address = data?.getStringExtra("address")
            val latitude = data?.getDoubleExtra("latitude", 0.0)
            val longitude = data?.getDoubleExtra("longitude", 0.0)
            if (!address.isNullOrEmpty() && latitude != null && longitude != null) {
                binding.manualSelect.text = address
                selectedLatitude = latitude
                selectedLongitude = longitude
            } else {
                // Handle case where address or coordinates are not available
            }
        }
    }
}

data class UserLocation(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val doorNo: String,
    val locality: String,
    val address: String
)
