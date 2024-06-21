package com.capztone.seafishfy.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.ActivityLoginBinding
import com.capztone.seafishfy.ui.activities.ViewModel.LoginViewModel

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        configureGoogleSignIn()

        if (viewModel.isUserLoggedIn()) {
            startActivity(Intent(this, LanguageActivity::class.java))
            finish()
        }

        binding.googleLoginbutton.setOnClickListener {
            signIn()
        }

        binding.btnGetOtp.setOnClickListener {
            validateNumber()
        }
    }

    private fun validateNumber() {

        if (binding.etPhoneNum.editableText?.toString()!!.isEmpty()) {
            binding.etPhoneNum.error = "Enter your Phone Number"
            binding.etPhoneNum.requestFocus()
            return
        }

        if (binding.etPhoneNum.editableText?.toString()!!.count() == 10) {
            binding.etPhoneNum.clearFocus()
            val intent = Intent(this, VerifyNumberActivity::class.java).apply {
                putExtra(phoneNumberKey, binding.etPhoneNum.editableText?.toString())
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Enter 10 digit number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_Id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                viewModel.signInWithGoogle(account.idToken!!,
                    onSuccess = {
                        startActivity(Intent(this, LocationActivity::class.java))
                        finish()
                    },
                    onFailure = { errorMessage ->
                        Log.w(TAG, "Google sign in failed")
                        Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                    })
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
        const val phoneNumberKey = "PHONE_NUMBER_KEY"
    }
}
