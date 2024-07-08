package com.capztone.seafishfy.ui.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.FragmentAccountBinding
import com.capztone.seafishfy.ui.activities.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AccountFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountBinding
    private lateinit var userNameEditText: TextView
    private lateinit var userEmailEditText: TextView
    private lateinit var banner: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        userNameEditText = binding.profileName
        userEmailEditText = binding.profileEmail

        // Fetch and display user details
        fetchAndDisplayUserDetails(user)

        binding.logout.setOnClickListener {
            signOut()
        }
        binding.recentBackButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Set onClickListener for logout text
        binding.text.setOnClickListener {
            signOut()
        }

        userNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Update Firebase user profile with new name
                val newName = s.toString()
                val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                user?.updateProfile(userProfileChangeRequest)
            }
        })

        userEmailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Update Firebase user email if it's changed
                val newEmail = s.toString()
                if (newEmail != user?.email) {
                    user?.updateEmail(newEmail)
                }
            }
        })
    }

    private fun fetchAndDisplayUserDetails(user: FirebaseUser?) {
        if (user != null) {
            val userId = user.uid
            val database = FirebaseDatabase.getInstance()

            // Check paths for HOME, WORK, and OTHER
            val homeRef = database.getReference("Locations").child(userId).child("HOME")
            val workRef = database.getReference("Locations").child(userId).child("WORK")
            val otherRef = database.getReference("Locations").child(userId).child("OTHER")

            // Listener for HOME address
            homeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val address = snapshot.child("address").getValue(String::class.java) ?: "No Address"
                        displayUserDetailsFromAddress(address)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }
            })

            // Listener for WORK address
            workRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val address = snapshot.child("address").getValue(String::class.java) ?: "No Address"
                        displayUserDetailsFromAddress(address)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }
            })

            // Listener for OTHER address
            otherRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val address = snapshot.child("address").getValue(String::class.java) ?: "No Address"
                        displayUserDetailsFromAddress(address)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }
            })
        }
    }

    private fun displayUserDetailsFromAddress(address: String) {
        // Split the address by commas and trim each part
        val addressParts = address.split(",").map { it.trim() }

        // Extract name (first part after the first comma) and phone number (last part)
        val name = if (addressParts.size > 1) {
            val firstCommaIndex = address.indexOf(",")
            val userName = address.substring(0, firstCommaIndex).trim()
            userName
        } else {
            "No Name"
        }

        val phoneNumber = addressParts.lastOrNull() ?: "No Phone Number"

        binding.profileName.text = name  // Update the profileName TextView
        userNameEditText.text = name
        userEmailEditText.text = phoneNumber

        val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl
        if (photoUrl != null) {
            Glide.with(this@AccountFragment)
                .load(photoUrl)
                .into(binding.profileImage)
        } else {
            // Load a default image if no photo URL is available
            Glide.with(this@AccountFragment)
                .load(R.drawable.baseline_account_circle_24)
                .into(binding.profileImage)
        }
    }


    private fun signOut() {
        // Sign out from Firebase Authentication
        FirebaseAuth.getInstance().signOut()

        // Sign out from Google
        val googleSignInClient =
            GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN)
        googleSignInClient.signOut().addOnCompleteListener {
            // Redirect to login activity
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }
}