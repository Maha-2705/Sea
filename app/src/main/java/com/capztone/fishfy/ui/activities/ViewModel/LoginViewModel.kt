package com.capztone.fishfy.ui.activities.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val usersRef = FirebaseDatabase.getInstance().getReference("user")

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        viewModelScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                if (result.user != null) {
                    val userId = result.user!!.uid
                    val userName = result.user!!.displayName

                    // Save username to Firebase under "users/userId/username"
                    usersRef.child(userId).child("username").setValue(userName)

                    onSuccess.invoke()
                } else {
                    onFailure.invoke("Authentication failed.")
                }
            } catch (e: Exception) {
                onFailure.invoke("Authentication failed: ${e.message}")
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
