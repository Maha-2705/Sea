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

class AccountFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountBinding
    private lateinit var userNameEditText: EditText
    private lateinit var userEmailEditText: EditText
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
            val displayName = user.displayName ?: "No Name"
            val email = user.email ?: "No Email"
            val photoUrl = user.photoUrl

            binding.bannerName.text = displayName
            userNameEditText.setText(displayName)
            userEmailEditText.setText(email)

            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .into(binding.profileImage)
            } else {
                // Load a default image if no photo URL is available
                Glide.with(this)
                    .load(R.drawable.baseline_account_circle_24)  // replace with your default image resource
                    .into(binding.profileImage)
            }
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
