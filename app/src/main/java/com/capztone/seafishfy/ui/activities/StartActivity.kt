package com.example.seafishfy.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.seafishfy.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
