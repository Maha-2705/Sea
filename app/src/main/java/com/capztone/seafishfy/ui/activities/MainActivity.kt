// MainActivity.kt
package com.capztone.seafishfy.ui.activities

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.ActivityMainBinding
import com.capztone.seafishfy.ui.activities.ViewModel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isNetworkAvailable()) {
            showNetworkDialog()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }

        auth = FirebaseAuth.getInstance()


        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // Initialize Lottie animation view
        val animationView: LottieAnimationView = binding.lottieAnimationView

        // Load animation from assets folder
        loadAnimation(animationView, "shower.json")

        // Start animation
        animationView.playAnimation()

        binding.tvAddress.setOnClickListener {
            showPopupMenu(it)
        }

        // Check if fragmentContainerView exists in the layout
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
            ?: // Handle the case where fragmentContainerView is not found
            // You might want to log an error or handle it in an appropriate way
            return

        // Find NavController only if fragmentContainerView is found
        val navController = navHostFragment.findNavController()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val address = intent.getStringExtra("ADDRESS")
        val locality = intent.getStringExtra("LOCALITY")
        val shoptextview = intent.getStringExtra("SHOP_TEXT_VIEW_VALUE")

        // Display the address and locality in TextViews
        binding.tvAddress.text = "Address: $address"
        binding.tvLocality.text = " $locality"


        // Check for null before setting up with NavController
        navController?.let {
            bottomNav.setupWithNavController(it)
        }

        // Observe the user address from ViewModel
        viewModel.userAddress.observe(this) { userAddress ->
            // Update UI with the user's address
            binding.tvAddress.text = "Address: $userAddress"
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.shopOneFragment, R.id.shopTwoFragment, R.id.shopThreeFragment,
                R.id.shopFourFragment, R.id.shopFiveFragment, R.id.shopSixFragment ,R.id.contactusActivity,R.id.accountFragment-> {
                    bottomNav.visibility = View.GONE
                }

                else -> {
                    bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }
    private fun loadAnimation(animationView: LottieAnimationView, animationFileName: String) {
        LottieCompositionFactory.fromAsset(this, animationFileName)
            .addListener { composition ->
                animationView.setComposition(composition)
            }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.address, popupMenu.menu)

        // Set text color for the PopupMenu items
        val menu = popupMenu.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val spannableString = SpannableString(menuItem.title.toString())
            spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.navy)), 0, spannableString.length, 0)
            menuItem.title = spannableString
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.use_current_location -> {
                    // Handle "Use Current Location" click
                    startActivity(Intent(this, LocationActivity::class.java))
                    // Hide the dropdown icon after picking an address
                    binding.dropdown.visibility = View.GONE
                    true
                }

                // Add more saved address clicks as needed
                else -> false
            }
        }

        popupMenu.setOnDismissListener {
            // Dismiss the PopupMenu when it's dismissed
            popupMenu.dismiss()
        }

        popupMenu.show()
    }
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showNetworkDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
        val title = TextView(this)
        title.text = "No Internet Connection"
        title.setTextColor(ContextCompat.getColor(this, R.color.Dnavy)) // Title color
        title.textSize = 20f
        title.setPadding(20, 25, 20, 20)
        builder.setCustomTitle(title)

        val message = TextView(this)
        message.text = "Please check your network connection and try again."
        message.setTextColor(ContextCompat.getColor(this, R.color.navy)) // Message color
        message.textSize = 16f
        message.setPadding(25, 20, 25, 0)
        builder.setView(message)

        builder.setPositiveButton("Retry") { dialog, _ ->
            dialog.dismiss()
            recreate()
        }
        builder.setOnCancelListener {
            finish()
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        // Set button color programmatically
        val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.setTextColor(ContextCompat.getColor(this, R.color.Lblack))
    }


    override fun onBackPressed() {
        // Check if there are fragments in the back stack
        if (supportFragmentManager.backStackEntryCount > 0) {
            // Pop the fragment from the back stack
            supportFragmentManager.popBackStack()
        } else {
            // If no fragments in the back stack, let the system handle the back button press
            super.onBackPressed()
        }
    }
}
