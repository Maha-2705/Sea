package com.capztone.seafishfy.ui.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import android.app.AlertDialog
import android.content.DialogInterface

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchLocation(it)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        binding.buttonConfirmLocation.setOnClickListener {
            marker?.let { currentMarker ->
                val latitude = currentMarker.position.latitude
                val longitude = currentMarker.position.longitude

                val geocoder = Geocoder(this, Locale.getDefault())
                val addressList = geocoder.getFromLocation(latitude, longitude, 1)
                val address = if (addressList!!.isNotEmpty()) {
                    addressList[0].getAddressLine(0)
                } else {
                    "Unknown location"
                }

                val locality = if (addressList.isNotEmpty()) {
                    addressList[0].locality
                } else {
                    "Unknown locality"
                }

                // Show confirmation dialog
                showConfirmationDialog(latitude, longitude, locality, address)
            }
        }
    }

    private fun showConfirmationDialog(latitude: Double, longitude: Double, locality: String, address: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Location")
        builder.setMessage("Do you want to save this location?")

        builder.setPositiveButton("Yes") { dialog, which ->
            saveLocationToFirebase(latitude, longitude, locality, address)

            val resultIntent = Intent().apply {
                putExtra("address", address)
                putExtra("latitude", latitude)
                putExtra("longitude", longitude)
                putExtra("locality", locality)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
            val intent = Intent(this, LanguageActivity::class.java)
            startActivity(intent)
        }

        builder.setNegativeButton("No") { dialog, which ->
            saveLocalityToFirebase(latitude, longitude, locality)
            dialog.dismiss()
            val intent = Intent(this, LanguageActivity::class.java)
            startActivity(intent)
        }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, get the current location
            getCurrentLocation()
        } else {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        mMap.setOnMapClickListener { latLng ->
            marker?.remove()
            val markerIcon = BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.map)
            )
            marker = mMap.addMarker(
                MarkerOptions().position(latLng).draggable(true).icon(markerIcon)
            )
        }

        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                this@MapsActivity.marker?.position = marker.position
            }
        })
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the missing permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val locality = addresses!![0].locality // Assuming you want the locality name

                val currentLocation = LatLng(location.latitude, location.longitude)
                val markerIcon = BitmapDescriptorFactory.fromBitmap(
                    BitmapFactory.decodeResource(resources, R.drawable.map)
                )
                marker = mMap.addMarker(
                    MarkerOptions().position(currentLocation).draggable(true).icon(markerIcon)
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))

                // Save location to Firebase
                saveLocalityToFirebase(location.latitude, location.longitude, locality)
            }
        }
    }

    private fun saveLocationToFirebase(latitude: Double, longitude: Double, locality: String, address: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val locationData = HashMap<String, Any>()
            locationData["latitude"] = latitude
            locationData["longitude"] = longitude
            locationData["locality"] = locality
            locationData["Default Address"] = address

            val userLocationRef = database.child("Locations").child(userId)
            userLocationRef.setValue(locationData)
                .addOnSuccessListener {
                    // Data successfully written
                    // Handle success if needed
                }
                .addOnFailureListener { e ->
                    // Failed to write data
                    // Handle failure if needed
                }
        }
    }

    private fun saveLocalityToFirebase(latitude: Double, longitude: Double, locality: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val locationData = HashMap<String, Any>()
            locationData["latitude"] = latitude
            locationData["longitude"] = longitude
            locationData["locality"] = locality

            val userLocationRef = database.child("Locations").child(userId)
            userLocationRef.setValue(locationData)
                .addOnSuccessListener {
                    // Data successfully written
                    // Handle success if needed
                }
                .addOnFailureListener { e ->
                    // Failed to write data
                    // Handle failure if needed
                }
        }
    }

    private fun searchLocation(location: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(location, 1)
        if (addresses!!.isNotEmpty()) {
            val address = addresses[0]
            val latLng = LatLng(address.latitude, address.longitude)
            mMap.clear()
            val markerIcon = BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.map)
            )
            marker = mMap.addMarker(
                MarkerOptions().position(latLng).draggable(true).icon(markerIcon)
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getCurrentLocation()
            } else {
                // Permission denied, show a default location (Tamil Nadu)
                val defaultLocation = LatLng(11.1271, 78.6569)
                val markerIcon = BitmapDescriptorFactory.fromBitmap(
                    BitmapFactory.decodeResource(resources, R.drawable.map)
                )
                marker = mMap.addMarker(
                    MarkerOptions().position(defaultLocation).draggable(true).icon(markerIcon)
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 7f))
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}