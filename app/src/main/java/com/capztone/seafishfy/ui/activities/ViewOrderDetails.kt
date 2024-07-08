package com.capztone.seafishfy.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.ActivityViewOrderDetailsBinding
import com.capztone.seafishfy.ui.activities.models.Order
import com.capztone.seafishfy.ui.activities.Utils.ToastHelper
import com.capztone.seafishfy.ui.activities.ViewModel.ViewODViewModel

class ViewOrderDetails : AppCompatActivity() {

    private lateinit var binding: ActivityViewOrderDetailsBinding
    private lateinit var viewModel: ViewODViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var orderId: String
    val ONE_MINUTE_IN_MILLIS = 60000

    private lateinit var cancelHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }

        viewModel = ViewModelProvider(this).get(ViewODViewModel::class.java)

        binding.detailGoToBackImageButton.setOnClickListener {
            finish()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        orderId = intent.getStringExtra("order_id") ?: ""

        if (orderId.isNotEmpty()) {
            viewModel.fetchOrderDetails(orderId)
            viewModel.fetchOrderImages(orderId)
        } else {
            ToastHelper.showCustomToast(this, "Order id not found")
            finish() // Finish activity if order id is not provided
        }

        cancelHandler = Handler()
        sharedPreferences = getSharedPreferences("OrderCancellation", Context.MODE_PRIVATE) // Initialize SharedPreferences

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.orderDetails.observe(this) { order ->
            if (order != null) {
                displayOrderDetails(order)
            }
        }
        // Check order status from Firebase and enable/disable radio button accordingly
        viewModel.fetchOrderStatus(orderId) { status ->
            when (status) {
                "Order confirmed" -> {
                    binding.radio.setOnClickListener {
                        showOrderTakenDialog()
                    }
                }
                "Order picked" -> {
                    binding.radio.setOnClickListener {
                        showOrderTakenDialog()
                    }
                }
                "Order delivered" -> {
                    binding.radio.setOnClickListener {
                        showOrderTakenDialog()
                    }
                    disableOrderView1()
                }
                else -> {
                    binding.radio.isEnabled = true
                    binding.radio.setOnClickListener {
                        showCancelOrderDialog()
                    }
                }
            }
        }

        viewModel.orderImages.observe(this) { imageUrls ->
            imageUrls.forEachIndexed { index, imageUrl ->
                // Load image into respective ImageView based on index
                when (index) {
                    0 -> loadImageIntoImageView(imageUrl, binding.foodImage)
                    1 -> loadImageIntoImageView(imageUrl, binding.foodImage1)
                    2 -> loadImageIntoImageView(imageUrl, binding.foodImage2)
                    3 -> loadImageIntoImageView(imageUrl, binding.foodImage3)
                    4 -> loadImageIntoImageView(imageUrl, binding.foodImage4)
                    // Add more cases if you have more ImageViews
                }
            }
        }

        viewModel.orderCancellationStatus.observe(this) { cancelled ->
            if (cancelled) {
                ToastHelper.showCustomToast(this, "Order cancelled")
                // Disable the view
                disableOrderView()
                // Store cancellation status in SharedPreferences
                sharedPreferences.edit().putBoolean("order_cancelled_$orderId", true).apply()
            }
        }

        // Check cancellation status when activity is created
        if (sharedPreferences.getBoolean("order_cancelled_$orderId", false)) {
            disableOrderView()
        }
    }

    private fun showOrderTakenDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
        alertDialogBuilder.setMessage("You can't cancel your order because the driver has already taken it.")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                binding.radio.isChecked = false
                dialog.dismiss()
            }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun loadImageIntoImageView(imageUrl: String, imageView: ImageView) {
        Glide.with(this@ViewOrderDetails)
            .load(imageUrl)
            .into(imageView)
    }

    private fun extractFoodName(foodName: String): String {
        val slashIndex = foodName.indexOf('/')
        return if (slashIndex != -1) {
            foodName.substring(slashIndex + 1).trimEnd(']')
        } else {
            foodName.trimEnd(']')
        }
    }

    private fun extractFoodNames(foodNames: List<String>): String {
        return foodNames.joinToString(", ") { extractFoodName(it) }
    }

    private fun displayOrderDetails(order: Order) {
        binding.apply {
            oid.text = "Order ID: ${order.itemPushKey}"
            cid.text = "User ID: ${order.userUid}"
            foodName.text = "Food Name: ${extractFoodNames(order.foodNames)}"
            foodPrice.text = "Food Price: ${order.adjustedTotalAmount}"
            quantity.text = "Quantity: ${order.foodQuantities}"
            time.text = "${order.currentTime}"

            binding.orderstatus.setOnClickListener {
                val intent = Intent(this@ViewOrderDetails, OrderStatusActivity::class.java)
                intent.putExtra("itemPushKey", order.itemPushKey)
                startActivity(intent)
            }
            binding.radio.setOnClickListener {
                showCancelOrderDialog()
            }
        }
    }
    private fun showCancelOrderDialog() {
        // Check the current order status
        viewModel.fetchOrderStatus(orderId) { status ->
            when (status) {
                "Order confirmed", "Order picked", "Order delivered" -> {
                    showOrderTakenDialog()
                }
                else -> {
                    // Check if the order is already cancelled
                    if (sharedPreferences.getBoolean("order_cancelled_$orderId", false)) {
                        ToastHelper.showCustomToast(this, "Your order is already cancelled")
                    } else {
                        // Show the cancel order dialog if the order status allows cancellation
                        AlertDialog.Builder(this)
                            .setMessage("Are you sure you want to cancel your order?")
                            .setCancelable(false)
                            .setPositiveButton("Yes") { _, _ ->
                                viewModel.cancelOrder(orderId)
                            }
                            .setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                                binding.radio.isChecked = false
                            }
                            .show()
                    }
                }
            }
        }
    }

    private fun disableOrderView1() {
        binding.viewOrder.isEnabled = false
        binding.viewOrder.setBackgroundColor(ContextCompat.getColor(this, R.color.Lgreen))
        binding.viewOrder.alpha = 0.5f
        // Display cancellation message on the disabled view
        binding.orderstatus.isEnabled = true // Enable the orderstatus button
        binding.radio.isEnabled = false
        binding.radio.isClickable = false
    }

    private fun disableOrderView() {
        binding.viewOrder.isEnabled = false
        binding.viewOrder.setBackgroundColor(ContextCompat.getColor(this, R.color.LiteAsh))
        binding.viewOrder.alpha = 0.5f
        // Display cancellation message on the disabled view
        val cancelledImageView = ImageView(this)
        cancelledImageView.setImageResource(R.drawable.cancelimg)
        cancelledImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        binding.orderstatus.isEnabled = false
        binding.radio.isEnabled = false
        val marginParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        marginParams.setMargins(150, 150, 150, 150)
        cancelledImageView.layoutParams = marginParams
        binding.viewOrder.addView(cancelledImageView)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to avoid memory leaks
        cancelHandler.removeCallbacksAndMessages(null)
    }
}
