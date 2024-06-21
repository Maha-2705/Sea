package com.capztone.seafishfy.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.seafishfy.databinding.ActivityVerifyNumberBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

class VerifyNumberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyNumberBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    private var phoneNum: String = "+91"
    private var storedVerificationId: String? = null
    private val TAG = "VerifyNumberActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        auth.useAppLanguage()

        if (intent != null) {
            val num = intent.getStringExtra(phoneNumberKey).toString()
            phoneNum += num
            Log.d(TAG, phoneNum)
            "Authenticate $phoneNum".also { binding.textAuthenticateNum.text = it }
        } else {
            Toast.makeText(this, "Bad Gateway 😒", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnVerify.setOnClickListener {
            if (binding.etOtp.editText?.text.toString().isNotEmpty()) {
                binding.etOtp.clearFocus()
                verifyVerificationCode(binding.etOtp.editText?.text.toString())
            } else {
                binding.etOtp.error = "Enter OTP 🤨"
                binding.etOtp.requestFocus()
                return@setOnClickListener
            }
        }

        binding.btnCancel.setOnClickListener {
            returnToEnterNumberActivity()
        }

        verificationCallbacks()

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNum)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verificationCallbacks() {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted:$credential")
                val code = credential.smsCode
                if (code != null) {
                    verifyVerificationCode(code)
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "onVerificationFailed", e)

                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        Toast.makeText(
                            this@VerifyNumberActivity,
                            "Invalid request", Toast.LENGTH_SHORT
                        ).show()
                        returnToEnterNumberActivity()
                    }
                    is FirebaseTooManyRequestsException -> {
                        Toast.makeText(
                            this@VerifyNumberActivity,
                            "The SMS quota for the project has been exceeded",
                            Toast.LENGTH_SHORT
                        ).show()
                        returnToEnterNumberActivity()
                    }
                    else -> {
                        Toast.makeText(
                            this@VerifyNumberActivity,
                            e.message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        returnToEnterNumberActivity()
                    }
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent:$verificationId")
                storedVerificationId = verificationId
                resendToken = token

                Toast.makeText(
                    this@VerifyNumberActivity,
                    "OTP sent to $phoneNum",
                    Toast.LENGTH_SHORT
                ).show()

                super.onCodeSent(verificationId, resendToken)
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    Toast.makeText(this, "Authorization Completed 🥳🥳", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, LanguageActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(
                            this,
                            "The verification code entered was invalid 🥺",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                    returnToEnterNumberActivity()
                }
            }
    }

    private fun verifyVerificationCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun returnToEnterNumberActivity() {
        val intent = Intent(applicationContext, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        const val phoneNumberKey = "PHONE_NUMBER_KEY"
    }
}