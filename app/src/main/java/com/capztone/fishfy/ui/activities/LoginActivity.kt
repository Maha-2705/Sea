package com.capztone.fishfy.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import com.capztone.fishfy.R
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.capztone.fishfy.databinding.ActivityLoginBinding
import com.capztone.fishfy.ui.activities.VerifyNumberActivity.Companion.phoneNumberKey
import com.capztone.fishfy.ui.activities.ViewModel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        configureGoogleSignIn()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }

        database = FirebaseDatabase.getInstance().reference

        if (viewModel.isUserLoggedIn()) {
            checkUserSetup()
        }

        binding.googleLoginbutton.setOnClickListener {
            signIn()
        }

        binding.btnGetOtp.setOnClickListener {
            validateNumber()
        }
    }

    private fun validateNumber() {
        val phoneNumber = binding.etPhoneNum.editableText?.toString()

        if (phoneNumber.isNullOrEmpty()) {
            binding.etPhoneNum.error = "Enter your Phone Number"
            binding.etPhoneNum.requestFocus()
            return
        }

        if (phoneNumber.length != 10) {
            Toast.makeText(this, "Enter 10 digit number", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the phone number exists in the DeletedAccounts path
        val databaseReference = FirebaseDatabase.getInstance().getReference("DeletedAccounts")
        val query = databaseReference.orderByChild("phoneNumber").equalTo(phoneNumber)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Phone number exists in DeletedAccounts path
                    val intent = Intent(this@LoginActivity, NotThereActivity::class.java).apply {
                        putExtra(phoneNumberKey, phoneNumber)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Phone number does not exist in DeletedAccounts path
                    val intent = Intent(this@LoginActivity, VerifyNumberActivity::class.java).apply {
                        putExtra(phoneNumberKey, phoneNumber)
                    }
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_Id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val email = account.email
                checkDeletedAccounts(email) { isDeleted ->
                    if (isDeleted) {
                        navigateToNotThereActivity()
                    } else {
                        viewModel.signInWithGoogle(account.idToken!!,
                            onSuccess = {
                                val currentUser = auth.currentUser
                                if (currentUser != null) {
                                    val userRef = database.child("users").child(currentUser.uid)
                                    val userMap = mapOf(
                                        "uid" to currentUser.uid,
                                        "email" to currentUser.email
                                    )
                                    userRef.setValue(userMap).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            checkUserSetup()
                                        } else {
                                            Log.w(TAG, "Failed to store user data")
                                            Toast.makeText(baseContext, "Failed to store user data", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onFailure = { errorMessage ->
                                Log.w(TAG, "Google sign in failed")
                                Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                            })
                    }
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun checkDeletedAccounts(email: String?, callback: (Boolean) -> Unit) {
        if (email.isNullOrEmpty()) {
            callback(false)
            return
        }

        val deletedAccountsRef = database.child("DeletedAccounts")
        deletedAccountsRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "checkDeletedAccounts:onCancelled", error.toException())
                callback(false)
            }
        })
    }

    private fun navigateToNotThereActivity() {
        val intent = Intent(this, NotThereActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkUserSetup() {
        val user = auth.currentUser
        if (user != null) {
            val userRef = database.child("users").child(user.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this@LoginActivity, LanguageActivity::class.java))
                    }
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "checkUserSetup:onCancelled", error.toException())
                    Toast.makeText(this@LoginActivity, "Failed to check user setup", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }
}