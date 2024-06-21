package com.capztone.seafishfy.ui.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.ActivityLanguageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private lateinit var userLanguageRef: DatabaseReference
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Firebase database reference for user language preference
        val currentUser = FirebaseAuth.getInstance().currentUser
        userId = currentUser?.uid ?: ""

        // Firebase database reference for user language preference
        userLanguageRef = FirebaseDatabase.getInstance().getReference("user").child(userId).child("language")
       // Firebase database reference for user location
        val userLocationRef = FirebaseDatabase.getInstance().getReference("Locations").child(userId)

// Listener to check if shopName exists and navigate accordingly
        userLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val shopName = snapshot.child("shopname").getValue(String::class.java)
                    if (shopName != null && shopName.isNotBlank()) {
                        // Shop name exists, do nothing (or handle as needed)
                    } else {
                        // Shop name does not exist or is blank, navigate to LocationNotAvailableActivity
                        startActivity(Intent(this@LanguageActivity, LocationNotAvailable::class.java))
                        finish() // Optionally finish this activity to prevent going back to it
                    }
                } else {
                    // Location data does not exist, navigate to LocationNotAvailableActivity
                    startActivity(Intent(this@LanguageActivity, LocationNotAvailable::class.java))
                    finish() // Optionally finish this activity to prevent going back to it
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        // Populate the spinner with languages using a custom ArrayAdapter
        val languages = resources.getStringArray(R.array.languages_array)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, languages) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                // Customize TextView within the spinner item
                val textView = view.findViewById(android.R.id.text1) as android.widget.TextView
                textView.setTextColor(resources.getColor(R.color.black))  // Set text color to black
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                // Customize TextView within the dropdown item
                val textView = view.findViewById(android.R.id.text1) as android.widget.TextView
                textView.setTextColor(resources.getColor(R.color.black))
                textView.setBackgroundColor(resources.getColor(R.color.white)) // Set text color to white for dropdown items
                return view
            }
        }

        // Set the adapter to the spinner
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = adapter

        // Retrieve and set language preference
        userLanguageRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val languageCode = snapshot.getValue(String::class.java)
                languageCode?.let {
                    setLocale(it)
                    updateSpinner(it)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        // Spinner item selected listener to change language preference
        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedLanguage = languages[position].lowercase(Locale.ROOT)
                updateLanguagePreference(selectedLanguage)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Button click listener to navigate to MainActivity
        binding.next.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun updateLanguagePreference(languageCode: String) {
        userLanguageRef.setValue(languageCode)
    }

    private fun setLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun updateSpinner(languageCode: String) {
        val languages = resources.getStringArray(R.array.languages_array)
        val position = languages.indexOfFirst { it.equals(languageCode, ignoreCase = true) }
        if (position >= 0) {
            binding.languageSpinner.setSelection(position)
        }
    }
}
