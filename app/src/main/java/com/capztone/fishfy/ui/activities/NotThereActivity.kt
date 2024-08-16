package com.capztone.fishfy.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.capztone.fishfy.databinding.ActivityLocationNotAvailableBinding
import com.capztone.fishfy.databinding.ActivityNotThereBinding
import com.capztone.fishfy.ui.activities.LoginActivity

class   NotThereActivity : AppCompatActivity() {
    private lateinit var binding:   ActivityNotThereBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }

        binding = ActivityNotThereBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.another.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
